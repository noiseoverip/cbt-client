package com.squareup.spoon;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.Log;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.squareup.spoon.uiautomator.RemoteUiAutomatorTestRunner;

import java.io.File;

import static com.squareup.spoon.SpoonLogger.logError;

/**
 * Class UiAutomatorCbtSpoonDeviceRunner
 *
 * @author iljabobkevic 2013-10-20 initial version
 */
public class UiAutomatorCbtSpoonDeviceRunner extends AbstractCbtSpoonDeviceRunner {
   private static final String TEST_PACKAGE_PATH = "/data/local/tmp/";

   /**
    * Create a test runner for a single device.
    *
    * @param sdk        Path to the local Android SDK directory.
    * @param apk        Path to application APK.
    * @param output     Path to output directory.
    * @param serial     Device to run the test on.
    * @param debug      Whether or not debug logging is enabled.
    * @param adbTimeout time in ms for longest test execution
    * @param classpath  Custom JVM classpath or {@code null}.
    * @param className  Test class name to run or {@code null} to run all tests.
    * @param methodName Test method name to run or {@code null} to run all tests.  Must also pass
    *                   {@code className}.
    */
   UiAutomatorCbtSpoonDeviceRunner(File sdk, File apk, File output, String serial, boolean debug, boolean noAnimations, int adbTimeout, String classpath, String className, String methodName, IRemoteAndroidTestRunner.TestSize testSize, boolean disableScreenshot) {
      super(sdk, apk, output, serial, debug, noAnimations, adbTimeout, classpath, className, methodName, testSize, disableScreenshot);

   }

   @Override
   protected void removeTestPackage(IDevice device, String testPackages) {
      MultiLineReceiver receiver = new MultiLineReceiver() {
         @Override
         public void processNewLines(String[] lines) {
            for (String line : lines) {
               Log.v(this.getClass().getName(), line);
            }
         }

         @Override
         public boolean isCancelled() {
            return false;
         }
      };
      for (String testPackage : testPackages.split(" ")) {
         try {
            device.executeShellCommand(String.format("rm %s%s", TEST_PACKAGE_PATH, testPackage), receiver);
         } catch (Exception e) {
            logError("[%s] test apk removal failed.  Error [%s]", device.getSerialNumber(), e.getMessage());
         }
      }
   }

   @Override
   protected void installTestPackage(IDevice device, File testApk) throws InstallException {
      try {
         device.pushFile(testApk.getAbsolutePath(), TEST_PACKAGE_PATH + testApk.getName());
      } catch (Exception e) {
         throw new InstallException(FAILED_INSTALLATION_MSG, e);
      }
   }

   @Override
   protected IRemoteAndroidTestRunner getRemoteTestRunner(String testPackge, String testRunner, IDevice device) {
      return new RemoteUiAutomatorTestRunner(testPackge, testRunner, device);
   }

}
