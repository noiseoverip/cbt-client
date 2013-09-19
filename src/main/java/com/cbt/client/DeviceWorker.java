package com.cbt.client;

import com.cbt.ws.entity.Device;
import com.cbt.ws.entity.DeviceJob;
import com.cbt.ws.entity.DeviceJobResult;
import com.cbt.ws.entity.TestPackage;
import com.cbt.ws.jooq.enums.DeviceJobResultState;
import com.cbt.ws.jooq.enums.DeviceState;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;
import com.squareup.spoon.DeviceResult;
import com.squareup.spoon.DeviceTestResult;
import com.squareup.spoon.SpoonRunner;
import com.squareup.spoon.SpoonSummary;
import com.sun.jersey.api.client.ClientHandlerException;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

/**
 * Class responsible for updating particular device state, fetching and executing jobs. It support one device only,
 * therefore, separate instances must be run for each device
 *
 * @author SauliusAlisauskas 2013-03-22 Initial version
 */
public class DeviceWorker implements Runnable {
   public interface Callback {
      void onDeviceOffline(Device device);
   }

   private static final Logger mLogger = Logger.getLogger(DeviceWorker.class);
   private AdbApi mAdbApi;
   private Callback mCallback;
   private Device mDevice;
   private CbtWsClientApi mWsApi;

   @Inject
   public DeviceWorker(AdbApi adbApi, CbtWsClientApi wsApi) {
      mAdbApi = adbApi;
      mWsApi = wsApi;
   }

   /**
    * Update device state, check for waiting jobs, execute if any found and send results
    */
   @Override
   public void run() {
      mLogger.info("Checking device state, " + mDevice);
      // Check device status
      List<String> deviceNames = null;
      try {
         deviceNames = mAdbApi.getDevices();
      } catch (Exception e) {
         mLogger.error("Could not find any device attached");
      } finally {
         if (null != deviceNames && deviceNames.indexOf(mDevice.getSerialNumber()) > -1) {
            mDevice.setState(DeviceState.ONLINE);
         } else {
            mLogger.warn("Device wen offline, " + mDevice);
            mDevice.setState(DeviceState.OFFLINE);
            if (null != mCallback) {
               mCallback.onDeviceOffline(mDevice);
            }
         }
         sendDeviceUpdate();
      }

      // Check for available jobs
      if (mDevice.getState().equals(DeviceState.ONLINE)) {
         mLogger.info("Checking jobs for " + mDevice);
         DeviceJob job = mWsApi.getWaitingJob(mDevice);
         if (null != job) {
            mLogger.info("Found job " + job);

            TestPackage testPackage = fetchTestPackage(job);

            Path tempDir = null;
            try {
               tempDir = Files.createTempDirectory("cbt-spoon");
               SpoonRunner runner = new SpoonRunner.Builder()
                     .setOutputDirectory(tempDir.toFile())
                     .setApplicationApk(new File(testPackage.getTestTargetPath()))
                     .setInstrumentationApk(new File(testPackage.getTestScriptPath()))
                     .setDisableHtml(true)
                     .setDisableScreenshot(true)
                     .setUiAutomator(true)
                     .setAndroidSdk(new File("/Users/iljabobkevic/personal/dev/adt/sdk"))
                     .setClassName(Joiner.on(",").join(job.getMetadata().getTestClasses()))
                     .addDevice(mDevice.getSerialNumber())
                     .setDebug(true).build();


               PrintStream origOut = System.out;
               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               MultipleOutputWriter multiOut = new MultipleOutputWriter(baos, origOut);

               PrintStream interceptor = new PrintStream(multiOut);
               System.setOut(interceptor);

               runner.run();

               System.out.flush();
               System.setOut(origOut);

               baos.close();


               mLogger.info("Read the result from a file in the output directory.");
               FileReader resultFile = new FileReader(new File(tempDir.toAbsolutePath().toString(), "result.json"));
               SpoonSummary spoonSummary = GSON.fromJson(resultFile, SpoonSummary.class);
               resultFile.close();
               DeviceJobResult result = parseJobResult(spoonSummary, testPackage, baos.toString());

               mLogger.info("Publishing results:" + result);
               publishTestResult(result);

            } catch (IOException e) {
               e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
               exitJobRun("Could not execute test on to device", e);
            }
         } else {
            mLogger.info("No jobs found");
         }
      }

   }

   /**
    * Set callback implementation
    *
    * @param callback
    */
   public void setCallback(Callback callback) {
      mCallback = callback;
   }

   /**
    * Set device
    *
    * @param device
    */
   public void setDevice(Device device) {
      mDevice = device;
   }

   /**
    * Handle abnormal exit of device job execution
    *
    * @param message
    */
   private void exitJobRun(String message, Throwable e) {
      mLogger.error(message, e);
      // TODO: send result of abnormal exit to server
      throw new RuntimeException(message);
   }

   /**
    * Retrieve test package for specified device job
    *
    * @param job
    * @return
    * @throws CbtWsClientException
    * @throws IOException
    * @see {@link CbtWsClientApi#checkoutTestPackage(Long)}
    */
   private TestPackage fetchTestPackage(DeviceJob job) {
      TestPackage testPackage = null;
      try {
         testPackage = mWsApi.checkoutTestPackage(job.getId());
      } catch (CbtWsClientException | IOException e) {
         exitJobRun("Error while checking out files", e);
      }
      return testPackage;
   }

   /**
    * Helper method for handling publishing of test results
    *
    * @param result
    */
   private void publishTestResult(DeviceJobResult result) {
      try {
         mWsApi.publishDeviceJobResult(result);
      } catch (CbtWsClientException e) {
         exitJobRun("Could not publish job result", e);
      }
   }

   /**
    * Helper method for sending device state update
    */
   private void sendDeviceUpdate() {
      try {
         mWsApi.updatedevice(mDevice);
      } catch (CbtWsClientException e) {
         mLogger.error("Could not update device:" + mDevice);
      } catch (ClientHandlerException connectionException) {
         mLogger.error("Connection problem", connectionException);
      }
   }


   private static final Gson GSON = new GsonBuilder() //
         .registerTypeAdapter(File.class, new TypeAdapter<File>() {
            @Override
            public void write(JsonWriter jsonWriter, File file) throws IOException {
               if (file == null) {
                  jsonWriter.nullValue();
               } else {
                  jsonWriter.value(file.getAbsolutePath());
               }
            }

            @Override
            public File read(JsonReader jsonReader) throws IOException {
               return new File(jsonReader.nextString());
            }
         }) //
         .enableComplexMapKeySerialization() //
         .setPrettyPrinting() //
         .create();

   private DeviceJobResult parseJobResult(SpoonSummary summary, TestPackage testPackage, String output) {
      DeviceJobResult jobResult = new DeviceJobResult();
      jobResult.setTestsErrors(0);
      jobResult.setTestsFailed(0);

      DeviceResult spoonResult = summary.getResults().get(mDevice.getSerialNumber());
      for (DeviceResult result : summary.getResults().values()) {
         if (result.getInstallFailed()) {
            jobResult.setTestsErrors(jobResult.getTestsErrors() + 1);
         }
         if (!result.getExceptions().isEmpty() && result.getTestResults().isEmpty()) {
            jobResult.setTestsErrors(jobResult.getTestsErrors() + 1);
         }
         for (DeviceTestResult methodResult : result.getTestResults().values()) {
            if (methodResult.getStatus() != DeviceTestResult.Status.PASS) {
               jobResult.setState(DeviceJobResultState.FAILED);
               jobResult.setTestsFailed(jobResult.getTestsFailed() + 1);
            }
         }
      }

      jobResult.setDevicejobId(testPackage.getDevicejobId());
      jobResult.setTestsRun(spoonResult.getTestResults().size());
      jobResult.setOutput(output);
      jobResult.setState(jobResult.getState() == null ? DeviceJobResultState.PASSED : jobResult.getState());
      jobResult.setCreated(new Date(summary.getStarted()));
      jobResult.setName(summary.getTitle());
      return jobResult;
   }

   private class MultipleOutputWriter extends OutputStream {

      private OutputStream[] outs;


      public MultipleOutputWriter(OutputStream... outs) {
         this.outs = outs;
      }

      @Override
      public void write(int b) throws IOException {
         for (OutputStream out : outs) {
            out.write(b);
         }
      }

      public void flush() throws IOException {
         for (OutputStream out : outs) {
            out.flush();
         }
      }

      public void close() throws IOException {
         for (OutputStream out : outs) {
            try {
               flush();
            } catch (IOException ignored) {
            }
            out.close();
         }
      }
   }
}