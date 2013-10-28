package com.cbt.client.device;

import com.android.ddmlib.logcat.LogCatMessage;
import com.cbt.client.configuration.Configuration;
import com.cbt.client.util.Utils;
import com.cbt.client.ws.WsClient;
import com.cbt.core.entity.Device;
import com.cbt.core.entity.DeviceJob;
import com.cbt.core.entity.DeviceJobResult;
import com.cbt.jooq.enums.DeviceJobResultState;
import com.cbt.jooq.enums.TestscriptTestscriptType;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.squareup.spoon.CbtSpoonRunner;
import com.squareup.spoon.DeviceResult;
import com.squareup.spoon.DeviceTest;
import com.squareup.spoon.DeviceTestResult;
import com.squareup.spoon.SpoonSummary;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Class responsible for updating particular device state, fetching and executing jobs. It support one device only,
 * therefore, separate instances must be run for each device
 *
 * @author SauliusAlisauskas 2013-03-22 Initial version
 */
public class DeviceWorker implements Callable<Void> {

   static final String DEVICE_TITLE_FREE = "FREE";
   static final String TEST_RUNNER_JAR = "testrunner.jar";
   static final String TEST_RUNNER_CLASS = "com.cbt.testrunner.uiautomator.SpoonUiAutomatorTestRunner";
   private final Configuration config;
   private final Logger logger = Logger.getLogger(DeviceWorker.class);
   private final WsClient wsClient;
   private final Utils utils;
   private Device device;

   @Inject
   public DeviceWorker(Configuration config, WsClient wsClient, Utils utils) {
      this.config = config;
      this.wsClient = wsClient;
      this.utils = utils;
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
               File testrunner = FileUtils.getFile(config.getWorkspace(), TEST_RUNNER_JAR);
               if (!testrunner.exists()) {
                  InputStream is = DeviceWorker.class.getResourceAsStream("/" + TEST_RUNNER_JAR);
                  if (null != is) {
                     Files.copy(is, testrunner.toPath());
                     logger.debug("Wrote file: " + testrunner);
                  } else {
                     logger.error("Failed writing: " + TEST_RUNNER_JAR);
                  }
               }
            }

            DeviceJobResult result = runTest(device, job, isUiAutomator);

            logger.info("Publishing results: " + result);
            wsClient.publishDeviceJobResult(result);
         } else {
            logger.info("No jobs found");
         }
      } finally {
         synchronized (device) {
            device.setTitle(DEVICE_TITLE_FREE);
         }
      }

      return null;
   }

   private DeviceJobResult parseJobResult(SpoonSummary summary) {
      DeviceJobResult jobResult = new DeviceJobResult();
      jobResult.setTestsErrors(0);
      jobResult.setTestsFailed(0);

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

      DeviceResult spoonResult = summary.getResults().get(device.getSerialNumber());
      jobResult.setTestsRun(spoonResult.getTestResults().size());
      jobResult.setState(jobResult.getState() == null ? DeviceJobResultState.PASSED : jobResult.getState());
      jobResult.setCreated(new Date(summary.getStarted()));
      jobResult.setName(summary.getTitle());
      return jobResult;
   }

   public DeviceJobResult runTest(Device device, DeviceJob job, boolean isUiAutomator) throws IOException {
      File jobOutputPath = FileUtils.getFile(config.getWorkspace(), device.getSerialNumber(), String.valueOf(job.getId()));
      File spoonOutputPath = FileUtils.getFile(jobOutputPath, "spoon");
      logger.debug("Job output path: " + jobOutputPath);
      logger.debug("Spoon output path: " + spoonOutputPath);
      CbtSpoonRunner runner = new CbtSpoonRunner.Builder()
            .setOutputDirectory(spoonOutputPath)
            .setApplicationApk(FileUtils.getFile(jobOutputPath, job.getTestTarget().getFileName()))
            .setInstrumentationApks(isUiAutomator ? FileUtils.getFile(config.getWorkspace(), TEST_RUNNER_JAR) : null,
                  FileUtils.getFile(jobOutputPath, job.getTestScript().getFileName()))
            .setDisableHtml(true)
            .setDisableScreenshot(true)
            .setUiAutomator(isUiAutomator)
            .setTestRunner(isUiAutomator ? TEST_RUNNER_CLASS : null)
            .setAndroidSdk(config.getSdk())
            .setClassName(Joiner.on(",").join(job.getMetadata().getTestClasses()))
            .addDevice(device.getSerialNumber())
            .setKeepAdb(true)
            .setDebug(true).build(); // config.isDebug()

      runner.run();

      logger.info("Read the result from a file in the output directory.");
      FileReader resultFile = new FileReader(FileUtils.getFile(spoonOutputPath, "result.json"));
      SpoonSummary spoonSummary = utils.GSON.fromJson(resultFile, SpoonSummary.class);
      resultFile.close();

      StringBuilder outputBuilder = new StringBuilder();
      Set<Map.Entry<DeviceTest, DeviceTestResult>> deviceResultEntries = spoonSummary.getResults().get(device.getSerialNumber()).getTestResults().entrySet();
      for (Map.Entry<DeviceTest, DeviceTestResult> entry : deviceResultEntries) {
         for (LogCatMessage message : entry.getValue().getLog()) {
            outputBuilder.append(message).append(System.lineSeparator());
         }
      }

      DeviceJobResult jobResult = parseJobResult(spoonSummary);
      jobResult.setOutput(outputBuilder.toString());
      jobResult.setDevicejobId(job.getId());

      return jobResult;
   }
}