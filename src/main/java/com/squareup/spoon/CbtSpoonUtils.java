package com.squareup.spoon;


import com.android.ddmlib.AndroidDebugBridge;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Class CbtSpoonUtils - utilities for executing instrumentation tests on devices.
 *
 * @author iljabobkevic 2013-10-20 initial version
 */
final class CbtSpoonUtils {

   private CbtSpoonUtils() {
      // No instances.
   }

   /**
    * Get an {@link com.android.ddmlib.AndroidDebugBridge} instance given an SDK path.
    */
   static AndroidDebugBridge initAdb(File sdk) {
      AndroidDebugBridge.initIfNeeded(false);
      File adbPath = FileUtils.getFile(sdk, "platform-tools", "adb");
      // Forcing the bridge creation might cause problems trying to manage it externally
      AndroidDebugBridge adb = AndroidDebugBridge.createBridge(adbPath.getAbsolutePath(), false);
      waitForAdb(adb);
      return adb;
   }

   private static void waitForAdb(AndroidDebugBridge adb) {
      for (int i = 1; i < 10; i++) {
         try {
            Thread.sleep(i * 100);
         } catch (InterruptedException e) {
            throw new RuntimeException(e);
         }
         if (adb.isConnected()) {
            return;
         }
      }
      throw new RuntimeException("Unable to connect to adb.");
   }
}

