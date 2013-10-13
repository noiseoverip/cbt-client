package com.cbt.client.device;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.cbt.client.configuration.Configuration;
import com.cbt.client.util.MultipleOutputWriter;
import com.cbt.client.util.Utils;
import com.cbt.core.entity.Device;
import com.cbt.core.entity.DeviceJob;
import com.cbt.core.entity.DeviceJobResult;
import com.cbt.jooq.enums.DeviceJobResultState;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.squareup.spoon.DeviceResult;
import com.squareup.spoon.DeviceTestResult;
import com.squareup.spoon.SpoonRunner;
import com.squareup.spoon.SpoonSummary;

/**
 * Wrapper class for all ADB related calls
 * 
 * @author SauliusAlisauskas 2013-10-13 Initial version, moved contents from DeviceMonitor, DeviceWorker
 * 
 */
public class AdbWrapper {

   private final Logger logger = Logger.getLogger(AdbWrapper.class);
   private Configuration config;

   @Inject
   public AdbWrapper(Configuration configuraiton) {
      this.config = configuraiton;
   }

   /**
    * Query ADB for connected devices
    * 
    * @return
    */
   public Map<String, IDevice> findAllDevices() {
      return Utils.findAllDevices(AndroidDebugBridge.getBridge());
   }

   /**
    * Run specified {@link DeviceJob} on a specified {@link Device} and return {@link DeviceJobResult}
    * 
    * @param device
    * @param job
    * @param isUiAutomator
    * @return
    * @throws IOException
    */
   public DeviceJobResult runTest(Device device, DeviceJob job, boolean isUiAutomator) throws IOException {
      File jobOutputPath = FileUtils.getFile(config.getWorkspace(), device.getSerialNumber(),
            String.valueOf(job.getId()));
      File spoonOutputPath = FileUtils.getFile(jobOutputPath, "spoon");
      logger.debug("Job output path: " + jobOutputPath);
      logger.debug("Spoon output path: " + spoonOutputPath);

      SpoonRunner runner = new SpoonRunner.Builder().setOutputDirectory(spoonOutputPath)
            .setApplicationApk(FileUtils.getFile(jobOutputPath, job.getTestTarget().getFileName()))
            .setInstrumentationApk(FileUtils.getFile(jobOutputPath, job.getTestScript().getFileName()))
            .setDisableHtml(true).setDisableScreenshot(true).setUiAutomator(isUiAutomator)
            .setAndroidSdk(config.getSdk()).setClassName(Joiner.on(",").join(job.getMetadata().getTestClasses()))
            .addDevice(device.getSerialNumber()).setKeepAdb(true).setDebug(true).build(); // config.isDebug()

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
      DeviceJobResult result = parseJobResult(spoonSummary, job.getId(), baos.toString(), device);

      return result;
   }

   /**
    * Convert {@link SpoonSummary} into {@link DeviceJobResult}
    * 
    * @param summary
    * @param deviceJobId
    * @param output
    * @param device
    * @return
    */
   private DeviceJobResult parseJobResult(SpoonSummary summary, long deviceJobId, String output, Device device) {
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
