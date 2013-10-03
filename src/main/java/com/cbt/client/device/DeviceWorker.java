package com.cbt.client.device;

import com.cbt.client.configuration.Configuration;
import com.cbt.client.util.MultipleOutputWriter;
import com.cbt.client.util.Utils;
import com.cbt.client.ws.WsClient;
import com.cbt.ws.entity.Device;
import com.cbt.ws.entity.DeviceJob;
import com.cbt.ws.entity.DeviceJobResult;
import com.cbt.ws.jooq.enums.DeviceJobResultState;
import com.cbt.ws.jooq.enums.TestscriptTestscriptType;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.squareup.spoon.DeviceResult;
import com.squareup.spoon.DeviceTestResult;
import com.squareup.spoon.SpoonRunner;
import com.squareup.spoon.SpoonSummary;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * Class responsible for updating particular device state, fetching and executing jobs. It support one device only,
 * therefore, separate instances must be run for each device
 *
 * @author SauliusAlisauskas 2013-03-22 Initial version
 */
public class DeviceWorker implements Callable<Void> {

   private final Configuration config;
   private final Logger logger = Logger.getLogger(DeviceWorker.class);
   private final WsClient wsClient;
   private Device device;

   @Inject
   public DeviceWorker(Configuration config, WsClient wsClient) {
      this.config = config;
      this.wsClient = wsClient;
   }

   public Device getDevice() {
      return device;
   }

   public void setDevice(Device device) {
      this.device = device;
   }

   @Override
   public Void call() throws Exception {
      logger.info("Checking jobs for device: " + device);
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

            File jobOutputPath = FileUtils.getFile(config.getWorkspace(), device.getSerialNumber(), String.valueOf(job.getId()));
            File spoonOutputPath = FileUtils.getFile(jobOutputPath, "spoon");
            logger.debug("Job output path: " + jobOutputPath);
            logger.debug("Spoon output path: " + spoonOutputPath);
            SpoonRunner runner = new SpoonRunner.Builder()
                  .setOutputDirectory(spoonOutputPath)
                  .setApplicationApk(FileUtils.getFile(jobOutputPath, job.getTestTarget().getFileName()))
                  .setInstrumentationApk(FileUtils.getFile(jobOutputPath, job.getTestScript().getFileName()))
                  .setDisableHtml(true)
                  .setDisableScreenshot(true)
                  .setUiAutomator(isUiAutomator)
                  .setAndroidSdk(config.getSdk())
                  .setClassName(Joiner.on(",").join(job.getMetadata().getTestClasses()))
                  .addDevice(device.getSerialNumber())
                  .setKeepAdb(true)
                  .setDebug(true).build(); // config.isDebug()

            PrintStream origOut = System.out;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MultipleOutputWriter multiOut = new MultipleOutputWriter(baos, origOut);

            PrintStream interceptor = new PrintStream(multiOut);
            System.setOut(interceptor);

            runner.run();

            System.out.flush();
            System.setOut(origOut);

            baos.close();

            logger.info("Read the result from a file in the output directory.");
            FileReader resultFile = new FileReader(FileUtils.getFile(spoonOutputPath, "result.json"));
            SpoonSummary spoonSummary = Utils.GSON.fromJson(resultFile, SpoonSummary.class);
            resultFile.close();
            DeviceJobResult result = parseJobResult(spoonSummary, job.getId(), baos.toString());

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

   private DeviceJobResult parseJobResult(SpoonSummary summary, long deviceJobId, String output) {
      DeviceJobResult jobResult = new DeviceJobResult();
      jobResult.setTestsErrors(0);
      jobResult.setTestsFailed(0);

      DeviceResult spoonResult = summary.getResults().get(device.getSerialNumber());
      for (DeviceResult result : summary.getResults().values()) {
         if (result.getInstallFailed()) {
            jobResult.setTestsErrors(jobResult.getTestsErrors() + 1);
            jobResult.setState(DeviceJobResultState.FAILED);
         }
         if (!result.getExceptions().isEmpty() && result.getTestResults().isEmpty()) {
            jobResult.setTestsErrors(jobResult.getTestsErrors() + 1);
            jobResult.setState(DeviceJobResultState.FAILED);
         }
         for (DeviceTestResult methodResult : result.getTestResults().values()) {
            if (methodResult.getStatus() != DeviceTestResult.Status.PASS) {
               jobResult.setState(DeviceJobResultState.FAILED);
               jobResult.setTestsFailed(jobResult.getTestsFailed() + 1);
            }
         }
      }

      jobResult.setDevicejobId(deviceJobId);
      jobResult.setTestsRun(spoonResult.getTestResults().size());
      jobResult.setOutput(output);
      jobResult.setState(jobResult.getState() == null ? DeviceJobResultState.PASSED : jobResult.getState());
      jobResult.setCreated(new Date(summary.getStarted()));
      jobResult.setName(summary.getTitle());
      return jobResult;
   }
}