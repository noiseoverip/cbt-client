package com.cbt.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.cbt.client.annotations.CbtWsURI;
import com.cbt.client.annotations.WorkspacePath;
import com.cbt.ws.entity.Device;
import com.cbt.ws.entity.DeviceJob;
import com.cbt.ws.entity.TestPackage;
import com.cbt.ws.utils.Utils;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Cbt webservcie client api
 * 
 * @author SauliusAlisauskas 2013-03-12 Initial version
 * 
 */
public class CbtWsClientApi {
	private Client mClient;
	private Logger mLogger = Logger.getLogger(CbtWsClientApi.class);
	private String mWorkspace;
	private String mWsUrl;

	@Inject
	public CbtWsClientApi(@CbtWsURI String wsUrl, @WorkspacePath String workspace) {
		mWsUrl = wsUrl;
		mWorkspace = workspace;
	}

	/**
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
		testPackage = getClient().resource(mWsUrl).path("checkout/testpackage")
				.queryParam("devicejob_id", deviceJobId.toString()).get(TestPackage.class);
		} catch (Exception e) {
			throw new CbtWsClientException("Could not fetch test package information", e);
		}
		
		mLogger.debug("Received info:" + testPackage);
		
		// Fetch required files
		ClientResponse response = getClient().resource(mWsUrl).path("checkout/testpackage.zip")
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
	 * Get client instance
	 * 
	 * @return
	 */
	private Client getClient() {
		if (null == mClient) {
			mClient = Client.create();
			mClient.setConnectTimeout(5000);
			mClient.setReadTimeout(5000);
			//mClient.addFilter(new LoggingFilter(System.out));
		}
		return mClient;
	}

	public DeviceJob getWaitingJob(Device device) {
		WebResource webResource = getClient().resource(mWsUrl);
		ClientResponse response = webResource.path("devicejobs/waiting")
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
	 * Register new device
	 * 
	 * @param device
	 * @return new device id or existing device id if already registered
	 * @throws CbtWsClientException
	 */
	public Long registerDevice(Device device) throws CbtWsClientException, ClientHandlerException {
		mLogger.debug("Registering device:" + device);
		WebResource webResource = getClient().resource(mWsUrl);
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
		WebResource webResource = getClient().resource(mWsUrl);
		ClientResponse response = webResource.path("device/" + device.getId()).type(MediaType.APPLICATION_JSON_TYPE)
				.post(ClientResponse.class, device);
		if (response.getStatus() != ClientResponse.Status.NO_CONTENT.getStatusCode()) {
			throw new CbtWsClientException("Failed to update device, response:" + response);
		}
	}
}
