package com.cbt.client;

import com.android.ddmlib.IDevice;
import com.cbt.client.configuration.Configuration;
import com.cbt.client.device.DeviceMonitor;
import com.cbt.client.device.DeviceWorker;
import com.cbt.client.util.SupervisorFactory;
import com.cbt.client.ws.CbtWsClientException;
import com.cbt.client.ws.WsClient;
import com.cbt.core.entity.Device;
import com.cbt.core.entity.DeviceType;
import com.cbt.jooq.enums.DeviceDeviceState;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.sun.jersey.api.client.ClientHandlerException;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class CbtClient
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public class CbtClient implements Callable<Boolean> {

   /**
    * {@link com.cbt.client.device.DeviceMonitor.Callback} implementation
    */
   public class DeviceMonitorCallback implements DeviceMonitor.Callback {

      /**
       * {@inheritDoc}
       */
      @Override
      public Device deviceOnline(IDevice device) throws CbtWsClientException {
         logger.info("Registering device: " + device);
         Device cbtDevice = new Device();
         cbtDevice.setOwnerId(config.getUserId());
         cbtDevice.setSerialNumber(device.getSerialNumber());
         cbtDevice.setState(DeviceDeviceState.ONLINE);
         DeviceType deviceType = new DeviceType();
         deviceType.setManufacture(device.getProperty("ro.product.manufacturer"));
         deviceType.setModel(device.getProperty("ro.product.model"));
         if (null != deviceType) {
            DeviceType deviceTypeSynced = wsClient.syncDeviceType(deviceType);
            cbtDevice.setDeviceTypeId(deviceTypeSynced.getId());
            cbtDevice.setDeviceOsId(1L);
         }
         Long deviceId = wsClient.registerDevice(cbtDevice);
         cbtDevice.setId(deviceId);
         return cbtDevice;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void deviceUpdate(Device device) {
         try {
            wsClient.updateDevice(device);
         } catch (CbtWsClientException e) {
            logger.error("Could not update device:" + device);
         } catch (ClientHandlerException connectionException) {
            logger.error("Connection problem", connectionException);
         }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void deviceWorker(Device device) {
         // TODO: Introduce busy concept
         synchronized (device) {
            device.setTitle(DEVICE_TITLE_BUSY);
         }
         DeviceWorker worker = injector.getInstance(DeviceWorker.class);
         worker.setDevice(device);
         SupervisorFactory.supervise(deviceWorkerExecutor.schedule(worker, WORKER_SCHEDULE_DELAY, TimeUnit.SECONDS));
      }
   }

   /**
    * Number of simultaneous device worker threads. Should be equal to most probable device count.
    */
   private static final int WORKER_MAX_THREAD = 10;
   /**
    * How often device worker should be called in seconds. Corresponds to the period at which it shall check if there
    * any jobs to be started.
    */
   private static final int WORKER_SCHEDULE_DELAY = 5;
   /**
    * Number of simultaneous device monitoring threads
    */
   private static final int MONITOR_MAX_THREAD = 1;
   /**
    * How ofter device monitoring threads should be called in seconds
    */
   private static final int MONITOR_SCHEDULE_DELAY = 10;
   /**
    * Device title to be set when device is used for the job
    */
   static final String DEVICE_TITLE_BUSY = "BUSY";
   private final Configuration config;
   private final WsClient wsClient;
   private final Logger logger = Logger.getLogger(CbtClient.class);
   private final DeviceMonitor monitor;
   private final Injector injector;
   private final ScheduledExecutorService deviceMonitorExecutor = Executors.newScheduledThreadPool(MONITOR_MAX_THREAD);
   private final ScheduledExecutorService deviceWorkerExecutor = Executors.newScheduledThreadPool(WORKER_MAX_THREAD);
   private boolean isStopped = false;

   @Inject
   public CbtClient(Configuration config, WsClient wsClient, DeviceMonitor monitor, Injector injector) {
      this.config = config;
      this.wsClient = wsClient;
      this.injector = injector;
      this.monitor = monitor;
      this.monitor.setCallback(new DeviceMonitorCallback());
   }

   /**
    * Set stopped flag so that client would exit after executing scheduled device monitor
    */
   public synchronized void setStopped() {
      isStopped = true;
   }

   /**
    * True if stop flag was set
    *
    * @return true if stop flag was set
    */
   public synchronized boolean isStopped() {
      return isStopped;
   }

   /**
    * Try to authenticate defined user with the server
    *
    * @return true if succeeded
    */
   boolean authenticate() {
      Map<String, Object> userProperties = wsClient.getUserByName(config.getUsername());
      boolean result = false;
      if (userProperties != null) {
         logger.debug("Authenticated user: " + userProperties);
         config.setUserId(Long.valueOf(userProperties.get("id").toString()));
         result = true;
      }
      return result;
   }

   /**
    * Authenticate and continuously schedule device monitor. {@link java.util.concurrent.ScheduledFuture#get()} shall
    * be called to retrieve the result.
    *
    * @return null
    * @throws Exception - From scheduled future
    */
   @Override
   public Boolean call() throws Exception {
      logger.info("Client is running...");
      boolean result = true;
      if (authenticate()) {
         ScheduledFuture<Void> future;
         do {
            future = deviceMonitorExecutor.schedule(monitor, MONITOR_SCHEDULE_DELAY, TimeUnit.SECONDS);
            future.get();
         } while (!future.isCancelled() && !isStopped());
      } else {
         logger.error("Could not authenticate");
         result = false;
      }
      return result;
   }
}