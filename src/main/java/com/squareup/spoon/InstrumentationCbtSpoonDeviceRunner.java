package com.squareup.spoon;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;

import java.io.File;

import static com.squareup.spoon.SpoonLogger.logInfo;

/**
 * Class InstrumentationCbtSpoonDeviceRunner
 *
 * @author iljabobkevic 2013-10-20 initial version
 */
public class InstrumentationCbtSpoonDeviceRunner extends AbstractCbtSpoonDeviceRunner {

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
   InstrumentationCbtSpoonDeviceRunner(File sdk, File apk, File testApk, File output, String serial, boolean debug, boolean noAnimations, int adbTimeout, String classpath, SpoonInstrumentationInfo instrumentationInfo, String className, String methodName, IRemoteAndroidTestRunner.TestSize testSize, boolean disableScreenshot) {
      super(sdk, apk, output, serial, debug, noAnimations, adbTimeout, classpath, className, methodName, testSize, disableScreenshot);
      setTestApk(testApk);
      setInstrumentationInfo(instrumentationInfo);
   }

   @Override
   protected void installTestPackage(IDevice device, File testApk) throws InstallException {
      String installError = device.installPackage(testApk.getAbsolutePath(), true);
      if (installError != null) {
         logInfo("[%s] test apk install failed.  Error [%s]", getSerial(), installError);
         throw new InstallException(installError, null);
      }
   }

   @Override
   protected IRemoteAndroidTestRunner getRemoteTestRunner(String testPackage, String testRunner, IDevice device) {
      return new RemoteAndroidTestRunner(testPackage, testRunner, device);
   }
}
