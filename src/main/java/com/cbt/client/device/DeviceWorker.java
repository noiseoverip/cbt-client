package com.cbt.client.device;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.cbt.client.ws.WsClient;
import com.cbt.core.entity.Device;
import com.cbt.core.entity.DeviceJob;
import com.cbt.core.entity.DeviceJobResult;
import com.cbt.jooq.enums.TestscriptTestscriptType;
import com.google.inject.Inject;

/**
 * Class responsible for updating particular device state, fetching and executing jobs. It support one device only,
 * therefore, separate instances must be run for each device
 *
 * @author SauliusAlisauskas 2013-03-22 Initial version
 */
public class DeviceWorker implements Callable<Void> {

   private final Logger logger = Logger.getLogger(DeviceWorker.class);
   private final WsClient wsClient;
   private Device device;
   private AdbWrapper adbBridge;

   @Inject
   public DeviceWorker(WsClient wsClient, AdbWrapper adbBridge) {
      this.wsClient = wsClient;
      this.adbBridge = adbBridge;
   }

   public Device getDevice() {
      return device;
   }

   public void setDevice(Device device) {
      this.device = device;
   }

   @Override
   public Void call() throws Exception {
      logger.debug("Checking jobs for device: " + device);
      DeviceJob job = wsClient.getWaitingJob(device);
      try {
         if (null != job) {
            logger.info("Found job: " + job);

            // Fetch testpackage.zip file
            wsClient.receiveTestPackage(job.getId(), device.getSerialNumber());

            boolean isUiAutomator = false;
            if (TestscriptTestscriptType.UIAUTOMATOR.equals(job.getTestScript().getTestScriptType())) {
               isUiAutomator = true;
            }
            
            DeviceJobResult result = adbBridge.runTest(device, job, isUiAutomator);
            
            logger.info("Publishing results:" + result);
            wsClient.publishDeviceJobResult(result);

         } else {
            logger.info("No jobs found");
         }
      } finally {
         synchronized (device) {
            device.setTitle("FREE");
         }
      }

      return null;
   }   
}