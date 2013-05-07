package com.cbt.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.cbt.ws.entity.Device;
import com.cbt.ws.entity.DeviceJob;
import com.cbt.ws.entity.DeviceJobResult;
import com.cbt.ws.entity.DeviceType;
import com.cbt.ws.entity.TestPackage;
import com.cbt.ws.utils.Utils;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;

/**
 * Cbt Web service client API
 * 
 * @author SauliusAlisauskas 2013-03-12 Initial version
 * 
 */
public class CbtWsClientApi {
	private ClientAuthFilter mAuthFilter;
	private Client mClient;
	private Logger mLogger = Logger.getLogger(CbtWsClientApi.class);
	private String mWorkspace;
	private String mWsUrl;
	boolean trace;

	@Inject
	public CbtWsClientApi(Configuration config, ClientAuthFilter authFilter) {
		mWsUrl = config.getUriCbtWebservice();
		mWorkspace = config.getPathWorkspace();
		mAuthFilter = authFilter;
		mLogger.debug("Cbt client using URL:" + mWsUrl + " workspace:" + mWorkspace);
		trace = config.isTraceRestClient();
	}

	/**
	 * Fetch Test package for specified device job id, extract file into workspace with randomly generated folder name
	 * 
	 * @param deviceJobId
	 * @return path to checked out file, null if error
	 * @throws CbtWsClientException
	 * @throws IOException
	 */
	public TestPackage checkoutTestPackage(Long deviceJobId) throws CbtWsClientException, IOException {
		mLogger.info("Checking out files for job id:" + deviceJobId + " workspace:" + mWorkspace);

		// Get Test package information
		TestPackage testPackage = null;
		try {
			testPackage = getWebRes().path("checkout/testpackage").queryParam("devicejob_id", deviceJobId.toString())
					.get(TestPackage.class);
		} catch (Exception e) {
			throw new CbtWsClientException("Could not fetch test package information", e);
		}

		mLogger.debug("Received info:" + testPackage);

		// Fetch required files
		ClientResponse response = getWebRes().path("checkout/testpackage.zip")
				.queryParam("devicejob_id", deviceJobId.toString()).get(ClientResponse.class);

		String tmpZipFileName;
		if (ClientResponse.Status.OK.getStatusCode() == response.getStatus()) {
			File downloadedFile = response.getEntity(File.class);
			// Generate temporary file
			tmpZipFileName = mWorkspace + String.valueOf(new Random().nextLong());
			// Make sure we have required directories created
			new File(mWorkspace).mkdirs();
			File ff = new File(tmpZipFileName);
			downloadedFile.renameTo(ff);
			FileWriter fr = new FileWriter(downloadedFile);
			fr.flush();
			fr.close();
		} else {
			throw new CbtWsClientException("Unexpected response result:" + response);
		}

		mLogger.debug("Zip file fetched");

		Utils.extractZipFiles(tmpZipFileName, mWorkspace);

		return testPackage;
	}

	/**
	 * Fetch device type information
	 * 
	 * @param deviceType
	 * @return
	 */
	public DeviceType getDeviceType(DeviceType deviceType) {
		WebResource webResource = getWebRes();
		ClientResponse response = webResource.path("device/type").accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, deviceType);

		DeviceType deviceTypeSynced = null;
		mLogger.debug("Received response:" + response);
		if (response.getStatus() == ClientResponse.Status.OK.getStatusCode()) {
			deviceTypeSynced = response.getEntity(DeviceType.class);
		}
		return deviceTypeSynced;
	}

	/**
	 * Fetch waiting device job
	 * 
	 * @param device
	 * @return {@link DeviceJob} if available, null if no jobs found
	 */
	public DeviceJob getWaitingJob(Device device) {
		WebResource webResource = getWebRes();

		ClientResponse response = webResource.path("devicejob/waiting")
				.queryParam("deviceId", device.getId().toString()).accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

		DeviceJob job = null;
		mLogger.debug("Received response:" + response);
		if (response.getStatus() == ClientResponse.Status.OK.getStatusCode()) {
			job = response.getEntity(DeviceJob.class);
		}
		return job;
	}

	/**
	 * Publish device job result
	 * 
	 * @param result
	 * @throws CbtWsClientException
	 */
	public DeviceJobResult publishDeviceJobResult(DeviceJobResult result) throws CbtWsClientException {
		mLogger.debug("Sending result");
		WebResource webResource = getWebRes();
		DeviceJobResult response = webResource.path("devicejob").path(result.getDevicejobId().toString())
				.path("result").type(MediaType.APPLICATION_JSON_TYPE).put(DeviceJobResult.class, result);
		if (response.getId() < 0) {
			throw new CbtWsClientException("Failed to publish device job result");
		}
		return result;
	}

	public Map<String, Object> getUserByName(String name) {
		mLogger.debug("Getting user statistics");
		WebResource webResource = getWebRes();
		ClientResponse response = webResource.path("user").queryParam("name", name)
				.type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
		if (response.getStatus() == Response.Status.OK.getStatusCode()) {
			return response.getEntity(Map.class);
		}
		return null;

	}

	/**
	 * Register new device
	 * 
	 * @param device
	 * @return new device id or existing device id if already registered
	 * @throws CbtWsClientException
	 */
	public Long registerDevice(Device device) throws CbtWsClientException, ClientHandlerException {
		mLogger.debug("Registering device:" + device);
		WebResource webResource = getWebRes();
		ClientResponse response = webResource.path("device").type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.TEXT_HTML).put(ClientResponse.class, device);
		mLogger.debug(response);
		int result = response.getStatus();
		if (result == ClientResponse.Status.OK.getStatusCode()) {
			mLogger.debug("Device register OK");
			return Long.valueOf(response.getEntity(String.class));
		} else if (result == ClientResponse.Status.CONFLICT.getStatusCode()) {
			Device registeredDevice = response.getEntity(Device.class);
			mLogger.warn("Device was already registered:" + registeredDevice);
			return registeredDevice.getId();
		} else {
			mLogger.debug("Device register failed");
			throw new CbtWsClientException("Failed to register new device");
		}
	}

	/**
	 * Update device
	 * 
	 * @param device
	 * @throws CbtWsClientException
	 */
	public void updatedevice(Device device) throws CbtWsClientException, ClientHandlerException {
		mLogger.debug("Updating device:" + device);
		WebResource webResource = getWebRes();
		ClientResponse response = webResource.path("device/" + device.getId()).type(MediaType.APPLICATION_JSON_TYPE)
				.post(ClientResponse.class, device);
		if (response.getStatus() != ClientResponse.Status.NO_CONTENT.getStatusCode()) {
			throw new CbtWsClientException("Failed to update device, response:" + response);
		}
	}

	/**
	 * Get client instance, instantiate and configure
	 * 
	 * @return
	 */
	private Client getClient() {
		if (null == mClient) {
			mClient = Client.create();
			mClient.setConnectTimeout(5000);
			mClient.setReadTimeout(5000);
			if (trace) {
				mClient.addFilter(new LoggingFilter(System.out));
			}
			mClient.addFilter(mAuthFilter);
		}
		return mClient;
	}

	private WebResource getWebRes() {
		return getClient().resource(mWsUrl);
	}
}
