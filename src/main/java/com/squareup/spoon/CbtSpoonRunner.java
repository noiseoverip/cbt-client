package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.google.common.base.Strings;
import com.squareup.spoon.html.CbtHtmlRenderer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.spoon.SpoonInstrumentationInfo.parseFromFile;
import static com.squareup.spoon.SpoonLogger.logDebug;
import static com.squareup.spoon.SpoonLogger.logInfo;
import static java.util.Collections.synchronizedSet;
import static java.util.Collections.unmodifiableSet;

/**
 * Class CbtSpoonRunner
 *
 * @author iljabobkevic 2013-10-20 initial version
 */
public final class CbtSpoonRunner {
   /**
    * Build a test suite for the specified devices and configuration.
    */
   public static class Builder {
      private String title = DEFAULT_TITLE;
      private File androidSdk;
      private File applicationApk;
      private File[] instrumentationApks;
      private File output;
      private boolean debug = false;
      private Set<String> serials;
      private String classpath = System.getProperty("java.class.path");
      private String className;
      private String methodName;
      private boolean noAnimations;
      private IRemoteAndroidTestRunner.TestSize testSize;
      private int adbTimeout;
      private boolean disableScreenshot;
      private boolean disableHtml;
      private boolean uiAutomator;
      private boolean keepAdb;
      private String testRunner;
      private boolean useAllDevices = false;

      /**
       * Identifying title for this execution.
       */
      public Builder setTitle(String title) {
         checkNotNull(title, "Title cannot be null.");
         this.title = title;
         return this;
      }

      /**
       * Path to the local Android SDK directory.
       */
      public Builder setAndroidSdk(File androidSdk) {
         checkNotNull(androidSdk, "SDK path not specified.");
         checkArgument(androidSdk.exists(), "SDK path does not exist.");
         this.androidSdk = androidSdk;
         return this;
      }

      /**
       * Path to application APK.
       */
      public Builder setApplicationApk(File apk) {
         checkNotNull(apk, "APK path not specified.");
         checkArgument(apk.exists(), "APK path does not exist.");
         this.applicationApk = apk;
         return this;
      }

      /**
       * Path to instrumentation APK.
       */
      public Builder setInstrumentationApks(File... apks) {
         checkNotNull(apks, "Instrumentation APK path not specified.");
         for (File apk : apks) {
            checkNotNull(apk, "Instrumentation APK path not specified.");
            checkArgument(apk.exists(), "Instrumentation APK path does not exist: " + apk);
         }
         this.instrumentationApks = apks;
         return this;
      }

      /**
       * Path to output directory.
       */
      public Builder setOutputDirectory(File output) {
         checkNotNull(output, "Output directory not specified.");
         this.output = output;
         return this;
      }

      /**
       * Whether or not debug logging is enabled.
       */
      public Builder setDebug(boolean debug) {
         this.debug = debug;
         return this;
      }

      /**
       * Whether or not animations are enabled.
       */
      public Builder setNoAnimations(boolean noAnimations) {
         this.noAnimations = noAnimations;
         return this;
      }

      /**
       * Set ADB timeout.
       */
      public Builder setAdbTimeout(int value) {
         this.adbTimeout = value;
         return this;
      }

      /**
       * Add a device serial for test execution.
       */
      public Builder addDevice(String serial) {
         checkNotNull(serial, "Serial cannot be null.");
         checkArgument(!useAllDevices, "Already marked as using all devices.");
         if (serials == null) {
            serials = new LinkedHashSet<String>();
         }
         serials.add(serial);
         return this;
      }

      /**
       * Use all currently attached device serials when executed.
       */
      public Builder useAllAttachedDevices() {
         if (this.serials != null) {
            throw new IllegalStateException("Serial list already contains entries.");
         }
         if (this.androidSdk == null) {
            throw new IllegalStateException("SDK must be set before calling this method.");
         }
         this.serials = Collections.emptySet();
         this.useAllDevices = true;
         return this;
      }

      /**
       * Classpath to use for new JVM processes.
       */
      public Builder setClasspath(String classpath) {
         checkNotNull(classpath, "Classpath cannot be null.");
         this.classpath = classpath;
         return this;
      }

      public Builder setClassName(String className) {
         this.className = className;
         return this;
      }

      public Builder setTestSize(IRemoteAndroidTestRunner.TestSize testSize) {
         this.testSize = testSize;
         return this;
      }

      public Builder setMethodName(String methodName) {
         this.methodName = methodName;
         return this;
      }

      public CbtSpoonRunner build() {
         checkNotNull(androidSdk, "SDK is required.");
         checkArgument(androidSdk.exists(), "SDK path does not exist.");
         checkNotNull(applicationApk, "Application APK is required.");
         checkNotNull(instrumentationApks, "Instrumentation APK is required.");
         checkNotNull(output, "Output path is required.");
         checkNotNull(serials, "Device serials are required.");
         if (!Strings.isNullOrEmpty(methodName)) {
            checkArgument(!Strings.isNullOrEmpty(className),
                  "Must specify class name if you're specifying a method name.");
         }

         return new CbtSpoonRunner(title, androidSdk, applicationApk, instrumentationApks, output, debug,
               noAnimations, adbTimeout, serials, classpath, className, methodName, testSize, disableHtml,
               disableScreenshot, uiAutomator, keepAdb, testRunner);
      }

      public Builder setDisableHtml(boolean disableHtml) {
         this.disableHtml = disableHtml;
         return this;
      }

      public Builder setDisableScreenshot(boolean disableScreenshot) {
         this.disableScreenshot = disableScreenshot;
         return this;
      }

      public Builder setUiAutomator(boolean uiAutomator) {
         this.uiAutomator = uiAutomator;
         return this;
      }

      public Builder setKeepAdb(boolean keepAdb) {
         this.keepAdb = keepAdb;
         return this;
      }

      public Builder setTestRunner(String testRunner) {
         this.testRunner = testRunner;
         return this;
      }
   }

   private static final String DEFAULT_TITLE = "Spoon Execution";
   private final String title;
   private final File androidSdk;
   private final File applicationApk;
   private final File[] instrumentationApks;
   private final File output;
   private final boolean debug;
   private final boolean noAnimations;
   private final int adbTimeout;
   private final String className;
   private final String methodName;
   private final Set<String> serials;
   private final String classpath;
   private final IRemoteAndroidTestRunner.TestSize testSize;
   private final boolean disableHtml;
   private final boolean disableScreenshot;
   private final boolean uiAutomator;
   private final boolean keepAdb;
   private final String testRunner;

   private CbtSpoonRunner(String title, File androidSdk, File applicationApk, File[] instrumentationApks,
                          File output, boolean debug, boolean noAnimations, int adbTimeout, Set<String> serials, String classpath,
                          String className, String methodName, IRemoteAndroidTestRunner.TestSize testSize,
                          boolean disableHtml, boolean disableScreenshot, boolean uiAutomator, boolean keepAdb,
                          String testRunner) {
      this.title = title;
      this.androidSdk = androidSdk;
      this.applicationApk = applicationApk;
      this.instrumentationApks = instrumentationApks;
      this.output = output;
      this.debug = debug;
      this.noAnimations = noAnimations;
      this.adbTimeout = adbTimeout;
      this.className = className;
      this.methodName = methodName;
      this.classpath = classpath;
      this.testSize = testSize;
      this.disableHtml = disableHtml;
      this.disableScreenshot = disableScreenshot;
      this.uiAutomator = uiAutomator;
      this.keepAdb = keepAdb;
      this.testRunner = testRunner;

      // Sanitize the serials for use on the filesystem as a folder name.
      Set<String> serialsCopy = new LinkedHashSet<String>(serials.size());
      for (String serial : serials) {
         serialsCopy.add(SpoonUtils.sanitizeSerial(serial));
      }
      this.serials = unmodifiableSet(serialsCopy);
   }

   /**
    * Returns {@code false} if a test failed on any device.
    */
   static boolean parseOverallSuccess(SpoonSummary summary) {
      for (DeviceResult result : summary.getResults().values()) {
         if (result.getInstallFailed()) {
            return false; // App and/or test installation failed.
         }
         if (!result.getExceptions().isEmpty() && result.getTestResults().isEmpty()) {
            return false; // No tests run and top-level exception present.
         }
         for (DeviceTestResult methodResult : result.getTestResults().values()) {
            if (methodResult.getStatus() != DeviceTestResult.Status.PASS) {
               return false; // Individual test failure.
            }
         }
      }
      return true;
   }

   /**
    * Install and execute the tests on all specified devices.
    *
    * @return {@code true} if there were no test failures or exceptions thrown.
    */
   public boolean run() {
      checkArgument(applicationApk.exists(), "Could not find application APK.");

      AndroidDebugBridge adb = CbtSpoonUtils.initAdb(androidSdk);

      try {
         // If we were given an empty serial set, load all available devices.
         Set<String> serials = this.serials;
         if (serials.isEmpty()) {
            serials = SpoonUtils.findAllDevices(adb);
         }

         // Execute all the things...
         SpoonSummary summary = runTests(adb, serials);

         CbtHtmlRenderer renderer = new CbtHtmlRenderer(summary, SpoonUtils.GSON, output);
         renderer.renderResultJson();
         if (!disableHtml) {
            renderer.renderHtml();
         }

         return parseOverallSuccess(summary);
      } finally {
         if (!keepAdb) {
            AndroidDebugBridge.disconnectBridge();
            AndroidDebugBridge.terminate();
         }
      }
   }

   private SpoonSummary runTests(AndroidDebugBridge adb, Set<String> serials) {
      int targetCount = serials.size();
      logInfo("Executing instrumentation suite on %d device(s).", targetCount);

      try {
         FileUtils.deleteDirectory(output);
      } catch (IOException e) {
         throw new RuntimeException("Unable to clean output directory: " + output, e);
      }

      final SpoonSummary.Builder summary = new SpoonSummary.Builder().setTitle(title).start();

      if (testSize != null) {
         summary.setTestSize(testSize);
      }

      final SpoonInstrumentationInfo testInfo[] = new SpoonInstrumentationInfo[instrumentationApks.length];
      for (int i = 0; i < instrumentationApks.length; i++) {
         if (uiAutomator) {
            testInfo[i] = new SpoonInstrumentationInfo(CbtSpoonUtils.getAppPackage(applicationApk),
                  instrumentationApks[i].getName(), testRunner);
         } else {
            testInfo[i] = parseFromFile(instrumentationApks[i]);
         }
         logDebug(debug, "Instrumentation: %s from %s", testInfo[i].getInstrumentationPackage(),
               instrumentationApks[i].getAbsolutePath());
      }

      logDebug(debug, "Application: %s from %s", testInfo[0].getApplicationPackage(),
            applicationApk.getAbsolutePath());

      if (targetCount == 1) {
         // Since there is only one device just execute it synchronously in this process.
         String serial = serials.iterator().next();
         try {
            logDebug(debug, "[%s] Starting execution.", serial);
            for (AbstractCbtSpoonDeviceRunner testRunner : getTestRunner(serial, testInfo)) {
               summary.addResult(serial, testRunner.run(adb));
            }
         } catch (Exception e) {
            logDebug(debug, "[%s] Execution exception!", serial);
            e.printStackTrace(System.out);
            summary.addResult(serial, new DeviceResult.Builder().addException(e).build());
         } finally {
            logDebug(debug, "[%s] Execution done.", serial);
         }
      } else {
         // Spawn a new thread for each device and wait for them all to finish.
         final CountDownLatch done = new CountDownLatch(targetCount);
         final Set<String> remaining = synchronizedSet(new HashSet<String>(serials));
         for (final String serial : serials) {
            logDebug(debug, "[%s] Starting execution.", serial);
            new Thread(new Runnable() {
               @Override
               public void run() {
                  for (AbstractCbtSpoonDeviceRunner testRunner : getTestRunner(serial, testInfo)) {
                     try {
                        summary.addResult(serial, testRunner.runInNewProcess());
                     } catch (Exception e) {
                        summary.addResult(serial, new DeviceResult.Builder().addException(e).build());
                     } finally {
                        done.countDown();
                        remaining.remove(serial);
                        logDebug(debug, "[%s] Execution done. (%s remaining %s)", serial, done.getCount(),
                              remaining);
                     }
                  }
               }
            }).start();
         }

         try {
            done.await();
         } catch (InterruptedException e) {
            throw new RuntimeException(e);
         }
      }

      if (!debug) {
         // Clean up anything in the work directory.
         try {
            FileUtils.deleteDirectory(new File(output, SpoonDeviceRunner.TEMP_DIR));
         } catch (IOException ignored) {
         }
      }

      return summary.end().build();
   }

   private AbstractCbtSpoonDeviceRunner[] getTestRunner(String serial, SpoonInstrumentationInfo[] testInfo) {
      AbstractCbtSpoonDeviceRunner[] result;
      if (uiAutomator) {
         result = new AbstractCbtSpoonDeviceRunner[]{new UiAutomatorCbtSpoonDeviceRunner(androidSdk, applicationApk,
               output,
               serial,
               debug, noAnimations, adbTimeout, classpath, className, methodName, testSize,
               disableScreenshot).setTestApk(instrumentationApks).setInstrumentationInfo(testInfo)};
      } else {
         result = new AbstractCbtSpoonDeviceRunner[instrumentationApks.length];
         for (int i = 0; i < instrumentationApks.length; i++) {
            result[i] = new InstrumentationCbtSpoonDeviceRunner(androidSdk, applicationApk, instrumentationApks[i],
                  output,
                  serial,
                  debug, noAnimations, adbTimeout, classpath, testInfo[i], className, methodName, testSize,
                  disableScreenshot);
         }

      }
      return result;
   }
}
