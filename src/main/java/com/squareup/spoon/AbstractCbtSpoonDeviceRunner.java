package com.squareup.spoon;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.FileListingService;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.logcat.LogCatMessage;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.squareup.spoon.Spoon.SPOON_SCREENSHOTS;
import static com.squareup.spoon.SpoonLogger.logDebug;
import static com.squareup.spoon.SpoonLogger.logError;
import static com.squareup.spoon.SpoonLogger.logInfo;
import static com.squareup.spoon.SpoonUtils.GSON;
import static com.squareup.spoon.SpoonUtils.createAnimatedGif;
import static com.squareup.spoon.SpoonUtils.obtainDirectoryFileEntry;
import static com.squareup.spoon.SpoonUtils.obtainRealDevice;

/**
 * Class AbstractCbtSpoonDeviceRunner
 *
 * @author iljabobkevic 2013-10-20 initial version
 */
public abstract class AbstractCbtSpoonDeviceRunner {
   private static final String FILE_EXECUTION = "execution.json";
   private static final String FILE_RESULT = "result.json";
   static final String TEMP_DIR = "work";
   static final String JUNIT_DIR = "junit-reports";
   protected static final String FAILED_INSTALLATION_MSG = "Unable to install application APK.";
   private final File sdk;
   private final File apk;
   private final File testApk;
   private final String serial;
   private final boolean debug;
   private final boolean noAnimations;
   private final int adbTimeout;
   private final File output;
   private final String className;
   private final String methodName;
   private final IRemoteAndroidTestRunner.TestSize testSize;
   private final File work;
   private final File junitReport;
   private final String classpath;
   private final SpoonInstrumentationInfo instrumentationInfo;
   private final boolean disableScreenshot;

   /**
    * Create a test runner for a single device.
    *
    * @param sdk                 Path to the local Android SDK directory.
    * @param apk                 Path to application APK.
    * @param testApk             Path to test application APK.
    * @param output              Path to output directory.
    * @param serial              Device to run the test on.
    * @param debug               Whether or not debug logging is enabled.
    * @param adbTimeout          time in ms for longest test execution
    * @param classpath           Custom JVM classpath or {@code null}.
    * @param instrumentationInfo Test apk manifest information.
    * @param className           Test class name to run or {@code null} to run all tests.
    * @param methodName          Test method name to run or {@code null} to run all tests.  Must also pass
    *                            {@code className}.
    */
   AbstractCbtSpoonDeviceRunner(File sdk, File apk, File testApk, File output, String serial, boolean debug,
                                boolean noAnimations, int adbTimeout, String classpath,
                                SpoonInstrumentationInfo instrumentationInfo, String className, String methodName,
                                IRemoteAndroidTestRunner.TestSize testSize, boolean disableScreenshot) {
      this.sdk = sdk;
      this.apk = apk;
      this.testApk = testApk;
      this.serial = serial;
      this.debug = debug;
      this.noAnimations = noAnimations;
      this.adbTimeout = adbTimeout;
      this.output = output;
      this.className = className;
      this.methodName = methodName;
      this.testSize = testSize;
      this.work = FileUtils.getFile(output, TEMP_DIR, serial);
      this.junitReport = FileUtils.getFile(output, JUNIT_DIR, serial + ".xml");
      this.classpath = classpath;
      this.instrumentationInfo = instrumentationInfo;
      this.disableScreenshot = disableScreenshot;
   }

   /**
    * De-serialize from disk, run the tests, and serialize the result back to disk.
    */
   public static void main(String... args) {
      if (args.length != 1) {
         throw new IllegalArgumentException("Must be started with a device directory.");
      }

      try {
         String outputDirName = args[0];
         File outputDir = new File(outputDirName);
         File executionFile = new File(outputDir, FILE_EXECUTION);
         if (!executionFile.exists()) {
            throw new IllegalArgumentException("Device directory and/or execution file doesn't exist.");
         }

         FileReader reader = new FileReader(executionFile);
         AbstractCbtSpoonDeviceRunner target = GSON.fromJson(reader, AbstractCbtSpoonDeviceRunner.class);
         reader.close();

         AndroidDebugBridge adb = SpoonUtils.initAdb(target.sdk);
         DeviceResult result = target.run(adb);
         AndroidDebugBridge.terminate();

         // Write device result file.
         FileWriter writer = new FileWriter(new File(outputDir, FILE_RESULT));
         GSON.toJson(result, writer);
         writer.close();
      } catch (Throwable ex) {
         logInfo("ERROR: Unable to execute test for target.  Exception message: %s", ex.getMessage());
         ex.printStackTrace(System.out);
         System.exit(1);
      }
   }

   /**
    * Serialize to disk and start {@link #main(String...)} in another process.
    */
   public DeviceResult runInNewProcess() throws IOException, InterruptedException {
      logDebug(debug, "[%s]", serial);

      // Create the output directory.
      work.mkdirs();

      // Write our configuration to a file in the output directory.
      FileWriter executionWriter = new FileWriter(new File(work, FILE_EXECUTION));
      GSON.toJson(this, executionWriter);
      executionWriter.close();

      // Kick off a new process to interface with ADB and perform the real execution.
      String name = SpoonDeviceRunner.class.getName();
      Process process = new ProcessBuilder("java", "-Djava.awt.headless=true", "-cp", classpath, name,
            work.getAbsolutePath()).start();
      printStream(process.getInputStream(), "STDOUT");
      printStream(process.getErrorStream(), "STDERR");

      final int exitCode = process.waitFor();
      logDebug(debug, "Process.waitFor() finished for [%s] with exitCode %d", serial, exitCode);

      // Read the result from a file in the output directory.
      FileReader resultFile = new FileReader(new File(work, FILE_RESULT));
      DeviceResult result = GSON.fromJson(resultFile, DeviceResult.class);
      logDebug(debug, result.getTestResults().toString());
      resultFile.close();

      return result;
   }

   private void printStream(InputStream stream, String tag) throws IOException {
      BufferedReader stdout = new BufferedReader(new InputStreamReader(stream));
      String s;
      while ((s = stdout.readLine()) != null) {
         logDebug(debug, "[%s] %s %s", serial, tag, s);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   ////  Secondary Per-Device Process  /////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   /**
    * Execute instrumentation on the target device and return a result summary.
    */
   public DeviceResult run(AndroidDebugBridge adb) {
      String appPackage = instrumentationInfo.getApplicationPackage();
      String testPackage = instrumentationInfo.getInstrumentationPackage();
      String testRunner = instrumentationInfo.getTestRunnerClass();
      logDebug(debug, "InstrumentationInfo: [%s]", instrumentationInfo);

      if (debug) {
         SpoonUtils.setDdmlibInternalLoggingLevel();
      }

      DeviceResult.Builder result = new DeviceResult.Builder();

      IDevice device = obtainRealDevice(adb, serial);
      logDebug(debug, "Got realDevice for [%s]", serial);

      // Get relevant device information.
      final DeviceDetails deviceDetails = DeviceDetails.createForDevice(device);
      result.setDeviceDetails(deviceDetails);
      logDebug(debug, "[%s] setDeviceDetails %s", serial, deviceDetails);

      try {
         installAppPackage(device);
         installTestPackage(device, testApk);
      } catch (InstallException e) {
         e.printStackTrace(System.out);
         return result.markInstallAsFailed(e.getMessage()).build();
      }

      // Create the output directory, if it does not already exist.
      work.mkdirs();

      // Initiate device logging.
      SpoonDeviceLogger deviceLogger = new SpoonDeviceLogger(device);

      String[] testClasses = new String[0];
      if (!Strings.isNullOrEmpty(className)) {
         if (Strings.isNullOrEmpty(methodName)) {
            testClasses = className.split(",");
         } else {
            testClasses = new String[]{className + "#" + methodName};
         }
      }

      IRemoteAndroidTestRunner runner = getRemoteTestRunner(testPackage, testRunner, device);

      // Run all the tests! o/
      try {
         logDebug(debug, "About to actually run tests for [%s]", serial);
         runner.setMaxtimeToOutputResponse(adbTimeout);
         runner.setClassNames(testClasses);
         if (testSize != null) {
            runner.setTestSize(testSize);
         }
         runner.run(
               new SpoonTestRunListener(result, debug),
               new XmlTestRunListener(junitReport)
         );
      } catch (Exception e) {
         result.addException(e);
      }

      // Grab all the parsed logs and map them to individual tests.
      Map<DeviceTest, List<LogCatMessage>> logs = deviceLogger.getParsedLogs();
      for (Map.Entry<DeviceTest, List<LogCatMessage>> entry : logs.entrySet()) {
         DeviceTestResult.Builder builder = result.getMethodResultBuilder(entry.getKey());
         if (builder != null) {
            builder.setLog(entry.getValue());
         }
      }

      try {
         grabScreenshots(device, result, appPackage);
      } catch (Exception e) {
         result.addException(e);
      }

      return result.build();
   }

   protected void installAppPackage(IDevice device) throws InstallException {
      // Now install the main application and the instrumentation application.
      String installError = device.installPackage(apk.getAbsolutePath(), true);
      if (installError != null) {
         logInfo("[%s] app apk install failed.  Error [%s]", serial, installError);
         throw new InstallException(installError, null);
      }
   }

   protected abstract void installTestPackage(IDevice device, File testApk) throws InstallException;

   protected abstract IRemoteAndroidTestRunner getRemoteTestRunner(String testPackage, String testRunner, IDevice device);

   protected String getSerial() {
      return serial;
   }

   private void grabScreenshots(IDevice device, DeviceResult.Builder resultBuilder,
                                String appPackage) throws IOException, TimeoutException, AdbCommandRejectedException, SyncException {
      // Ignore if screenshot grabbing is disabled
      if (disableScreenshot) {
         return;
      }

      logDebug(debug, "About to grab screenshots and prepare output for [%s]", serial);

      // Sync device screenshots, if any, to the local filesystem.
      String dirName = "app_" + SPOON_SCREENSHOTS;
      String localDirName = work.getAbsolutePath();
      final String devicePath = "/data/data/" + appPackage + "/" + dirName;
      FileListingService.FileEntry deviceDir = obtainDirectoryFileEntry(devicePath);
      logDebug(debug, "Pulling screenshots from [%s] %s", serial, devicePath);

      device.getSyncService()
            .pull(new FileListingService.FileEntry[]{deviceDir}, localDirName, SyncService.getNullProgressMonitor());

      File screenshotDir = new File(work, dirName);
      if (screenshotDir.exists()) {
         File imageDir = FileUtils.getFile(output, "image", serial);
         imageDir.mkdirs();

         // Move all children of the screenshot directory into the image folder.
         File[] classNameDirs = screenshotDir.listFiles();
         if (classNameDirs != null) {
            Multimap<DeviceTest, File> testScreenshots = ArrayListMultimap.create();
            for (File classNameDir : classNameDirs) {
               String className = classNameDir.getName();
               File destDir = new File(imageDir, className);
               FileUtils.copyDirectory(classNameDir, destDir);

               // Get a sorted list of all screenshots from the device run.
               List<File> screenshots = new ArrayList<File>(
                     FileUtils.listFiles(destDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE));
               Collections.sort(screenshots);

               // Iterate over each screenshot and associate it with its corresponding method result.
               for (File screenshot : screenshots) {
                  String methodName = screenshot.getParentFile().getName();

                  DeviceTest testIdentifier = new DeviceTest(className, methodName);
                  DeviceTestResult.Builder builder = resultBuilder.getMethodResultBuilder(testIdentifier);
                  if (builder != null) {
                     builder.addScreenshot(screenshot);
                     testScreenshots.put(testIdentifier, screenshot);
                  } else {
                     logError("Unable to find test for %s", testIdentifier);
                  }
               }
            }

            // Don't generate animations if the switch is present
            if (!noAnimations) {
               // Make animated GIFs for all the tests which have screenshots.
               for (DeviceTest deviceTest : testScreenshots.keySet()) {
                  List<File> screenshots = new ArrayList<File>(testScreenshots.get(deviceTest));
                  if (screenshots.size() == 1) {
                     continue; // Do not make an animated GIF if there is only one screenshot.
                  }
                  File animatedGif = FileUtils.getFile(imageDir, deviceTest.getClassName(),
                        deviceTest.getMethodName() + ".gif");
                  createAnimatedGif(screenshots, animatedGif);
                  resultBuilder.getMethodResultBuilder(deviceTest).setAnimatedGif(animatedGif);
               }
            }
         }
         FileUtils.deleteDirectory(screenshotDir);
      }
   }
}

