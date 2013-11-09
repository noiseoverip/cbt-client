package com.squareup.spoon;


import com.android.ddmlib.AndroidDebugBridge;
import com.squareup.spoon.axmlparser.AXMLParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

   static String getAppPackage(File apkTestFile) {
      InputStream is = null;
      try {
         ZipFile zip = new ZipFile(apkTestFile);
         ZipEntry entry = zip.getEntry("AndroidManifest.xml");
         is = zip.getInputStream(entry);

         AXMLParser parser = new AXMLParser(is);
         int eventType = parser.getType();

         while (eventType != AXMLParser.END_DOCUMENT) {
            if (eventType == AXMLParser.START_TAG) {
               if ("manifest".equals(parser.getName())) {
                  for (int i = 0; i < parser.getAttributeCount(); i++) {
                     String parserAttributeName = parser.getAttributeName(i);
                     if ("package".equals(parserAttributeName)) {
                        return parser.getAttributeValueString(i);
                     }
                  }
               }
            }
            eventType = parser.next();
         }
      } catch (IOException e) {
         throw new RuntimeException("Unable to parse app AndroidManifest.xml.", e);
      } finally {
         IOUtils.closeQuietly(is);
      }
      return null;
   }
}

