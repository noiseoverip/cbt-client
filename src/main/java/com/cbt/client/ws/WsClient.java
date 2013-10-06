package com.cbt.client.ws;

import com.cbt.client.configuration.Configuration;
import com.cbt.core.entity.Device;
import com.cbt.core.entity.DeviceJob;
import com.cbt.core.entity.DeviceJobResult;
import com.cbt.core.entity.DeviceType;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

/**
 * Class WsClient
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public class WsClient {

   private static final int JERSEY_CLIENT_TIMEOUT = 5000;
   private final String serverUrl;
   private final File workspace;
   private final Logger logger = Logger.getLogger(WsClient.class);
   private final ClientAuthFilter authFilter;
   private final boolean debug;
   private Client jerseyClient;

   @Inject
   public WsClient(Configuration config, ClientAuthFilter authFilter) {
      this.serverUrl = config.getServer();
      this.workspace = config.getWorkspace();
      this.debug = config.isDebug();
      this.authFilter = authFilter;
      logger.debug("Cbt client using URL: " + serverUrl + " workspace: " + workspace);
   }

   /**
    * Receive the package that contains tests to be executed
    *
    * @param jobId
    * @param deviceSerial
    * @throws CbtWsClientException
    * @throws IOException
    */
   public void receiveTestPackage(long jobId, String deviceSerial) throws CbtWsClientException, IOException {
      logger.debug("Checking out files for job id: " + jobId + " workspace: " + workspace);

      // Fetch required files
      ClientResponse response = getWebResource()
            .path("testpackage.zip")
            .queryParam("devicejobId", String.valueOf(jobId))
            .get(ClientResponse.class);

      File output = FileUtils.getFile(workspace, deviceSerial, String.valueOf(jobId));
      output.mkdirs();

      File ff;
      if (ClientResponse.Status.OK.getStatusCode() == response.getStatus()) {
         File downloadedFile = response.getEntity(File.class);
         // Make sure we have required directories created
         ff = FileUtils.getFile(output, String.valueOf(Math.abs(new Random().nextLong())));
         downloadedFile.renameTo(ff);
         FileWriter fr = new FileWriter(downloadedFile);
         fr.flush();
         fr.close();
      } else {
         throw new CbtWsClientException("Unexpected response result: " + response);
      }

      logger.debug("Zip file fetched");

      com.cbt.core.utils.Utils.extractZipFiles(ff.toString(), output.toString());
   }

   /**
    * Get client instance, instantiate and configure
    *
    * @return
    */
   private Client getClient() {
      if (null == jerseyClient) {
         ClientConfig clientConfig = new DefaultClientConfig(JacksonJsonProvider.class);
         jerseyClient = Client.create(clientConfig);
         jerseyClient.setConnectTimeout(JERSEY_CLIENT_TIMEOUT);
         jerseyClient.setReadTimeout(JERSEY_CLIENT_TIMEOUT);
         if (debug) {
            jerseyClient.addFilter(new LoggingFilter(System.out));
         }
         jerseyClient.addFilter(authFilter);
      }
      return jerseyClient;
   }

   /**
    * Helper method to construct request resource
    *
    * @return
    */
   private WebResource getWebResource() {
      return getClient().resource(serverUrl).path("rip");
   }

   /**
    * Get user by id map
    *
    * @param name - user name
    * @return
    */
   @SuppressWarnings("unchecked")
   public Map<String, Object> getUserByName(String name) {
      //TODO: create/use required entities

      logger.debug("Getting user statistics");

      ClientResponse response = getWebResource()
            .path("user")
            .queryParam("name", name)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .get(ClientResponse.class);

      return ClientResponse.Status.OK.getStatusCode() == response.getStatus() ? response.getEntity(Map.class) : null;
   }

   /**
    * Register defined device
    *
    * @param device
    * @return new device id or existing device id if already registered
    * @throws CbtWsClientException
    */
   public long registerDevice(Device device) throws CbtWsClientException, ClientHandlerException {
      logger.debug("Registering device: " + device);

      ClientResponse response = getWebResource()
            .path("device")
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.TEXT_HTML)
            .put(ClientResponse.class, device);

      switch (ClientResponse.Status.fromStatusCode(response.getStatus())) {
         case OK:
            logger.debug("Device register OK");
            return Long.valueOf(response.getEntity(String.class));
         case CONFLICT:
            Device registeredDevice = response.getEntity(Device.class);
            logger.debug("Device was already registered: " + registeredDevice);
            return registeredDevice.getId();
         default:
            logger.debug("Device register failed");
            throw new CbtWsClientException("Failed to register new device");
      }
   }

   /**
    * Update defined device information
    *
    * @param device
    * @throws CbtWsClientException
    * @throws ClientHandlerException
    */
   public void updateDevice(Device device) throws CbtWsClientException, ClientHandlerException {
      logger.debug("Updating device: " + device);

      ClientResponse response = getWebResource()
            .path("device")
            .path(device.getId().toString())
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(ClientResponse.class, device);

      if (ClientResponse.Status.OK.getStatusCode() == response.getStatus()) {
         throw new CbtWsClientException("Failed to update device, response:" + response);
      }
   }

   /**
    * Send job results to the server
    *
    * @param result
    * @return
    * @throws CbtWsClientException
    */
   public DeviceJobResult publishDeviceJobResult(DeviceJobResult result) throws CbtWsClientException {
      DeviceJobResult response = getWebResource().path("devicejob")
            .path(result.getDevicejobId().toString())
            .path("result")
            .type(MediaType.APPLICATION_JSON_TYPE)
            .put(DeviceJobResult.class, result);

      if (response.getId() < 0) {
         throw new CbtWsClientException("Failed to publish device job result");
      }
      return result;
   }

   /**
    * Sync (create if does not exit) device type with the server
    *
    * @param deviceType
    * @return
    */
   public DeviceType syncDeviceType(DeviceType deviceType) {
      ClientResponse response = getWebResource()
            .path("device")
            .path("type")
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .post(ClientResponse.class, deviceType);

      DeviceType deviceTypeSynced = null;
      logger.debug("Received response: " + response);
      if (ClientResponse.Status.OK.getStatusCode() == response.getStatus()) {
         deviceTypeSynced = response.getEntity(DeviceType.class);
      }
      return deviceTypeSynced;
   }

   /**
    * Get the job which is waiting in the queue to be executed
    *
    * @param device
    * @return
    */
   public DeviceJob getWaitingJob(Device device) {
      ClientResponse response = getWebResource().path("devicejob")
            .queryParam("deviceId", device.getId().toString())
            .accept(MediaType.APPLICATION_JSON)
            .type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

      DeviceJob job = null;
      logger.debug("Received response: " + response);
      if (ClientResponse.Status.OK.getStatusCode() == response.getStatus()) {
         // Currently we care only about oldest job in the list
         DeviceJob[] jobs = response.getEntity(DeviceJob[].class);
         if (jobs.length > 0) {
            job = jobs[0];
         }
      }
      return job;
   }
}
