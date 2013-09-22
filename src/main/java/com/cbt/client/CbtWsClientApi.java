package com.cbt.client;

import com.cbt.core.utils.Utils;
import com.cbt.ws.entity.Device;
import com.cbt.ws.entity.DeviceJob;
import com.cbt.ws.entity.DeviceJobResult;
import com.cbt.ws.entity.DeviceType;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

/**
 * CBT Web service client API
 *
 * @author SauliusAlisauskas 2013-03-12 Initial version
 */
public class CbtWsClientApi {
   private ClientAuthFilter mAuthFilter;
   private Client mClient;
   private Logger mLogger = Logger.getLogger(CbtWsClientApi.class);
   private String mWorkspace;
   private String mWsUrl;
   boolean mTrace;

   @Inject
   public CbtWsClientApi(Configuration config, ClientAuthFilter authFilter) {
      mWsUrl = config.getUriCbtWebservice();
      mWorkspace = config.getPathWorkspace();
      mAuthFilter = authFilter;
      mLogger.debug("Cbt client using URL:" + mWsUrl + " workspace:" + mWorkspace);
      mTrace = config.isTraceRestClient();
   }

   /**
    * Fetch Test package for specified device job id, extract file into workspace with randomly generated folder name
    *
    * @param deviceJobId
    * @return path to checked out file, null if error
    * @throws CbtWsClientException
    * @throws IOException
    */
   public void checkoutTestPackage(Long deviceJobId) throws CbtWsClientException, IOException {
      mLogger.info("Checking out files for job id:" + deviceJobId + " workspace:" + mWorkspace);

      // Fetch required files
      ClientResponse response = getWebRes()
            .path("testpackage.zip")
            .queryParam("devicejobId", deviceJobId.toString())
            .get(ClientResponse.class);

      String tmpZipFileName;
      if (Status.OK.getStatusCode() == response.getStatus()) {
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
   }

   /**
    * Send device type info
    *
    * @param deviceType
    * @return unique device type id based on submitted device info
    */
   public DeviceType syncDeviceType(DeviceType deviceType) {

      ClientResponse response = getWebRes()
            .path("device")
            .path("type")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(ClientResponse.class, deviceType);

      DeviceType deviceTypeSynced = null;
      mLogger.debug("Received response:" + response);
      if (Status.OK.getStatusCode() == response.getStatus()) {
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

      ClientResponse response = getWebRes().path("devicejob")
            .queryParam("deviceId", device.getId().toString())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

      DeviceJob job = null;
      mLogger.debug("Received response:" + response);
      if (Status.OK.getStatusCode() == response.getStatus()) {
         // Currently we care only about oldest job in the list
         DeviceJob[] jobs = response.getEntity(DeviceJob[].class);
         if (jobs.length > 0) {
            job = jobs[0];
         }
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

      DeviceJobResult response = getWebRes().path("devicejob")
            .path(result.getDevicejobId().toString())
            .path("result")
            .type(MediaType.APPLICATION_JSON_TYPE)
            .put(DeviceJobResult.class, result);

      if (response.getId() < 0) {
         throw new CbtWsClientException("Failed to publish device job result");
      }
      return result;
   }

   //TODO: create/use required entities
   @SuppressWarnings("unchecked")
   public Map<String, Object> getUserByName(String name) {
      mLogger.debug("Getting user statistics");

      ClientResponse response = getWebRes()
            .path("user")
            .queryParam("name", name)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .get(ClientResponse.class);

      if (Status.OK.getStatusCode() == response.getStatus()) {
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

      ClientResponse response = getWebRes()
            .path("device")
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.TEXT_HTML)
            .put(ClientResponse.class, device);

      switch (Status.fromStatusCode(response.getStatus())) {
         case OK:
            mLogger.debug("Device register OK");
            return Long.valueOf(response.getEntity(String.class));
         case CONFLICT:
            Device registeredDevice = response.getEntity(Device.class);
            mLogger.warn("Device was already registered:" + registeredDevice);
            return registeredDevice.getId();
         default:
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

      ClientResponse response = getWebRes()
            .path("device")
            .path(device.getId().toString())
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(ClientResponse.class, device);

      if (Status.OK.getStatusCode() == response.getStatus()) {
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
         if (mTrace) {
            mClient.addFilter(new LoggingFilter(System.out));
         }
         mClient.addFilter(mAuthFilter);
      }
      return mClient;
   }

   /**
    * Helper method to construct request resource
    *
    * @return
    */
   private WebResource getWebRes() {
      return getClient().resource(mWsUrl).path("rip");
   }
}
