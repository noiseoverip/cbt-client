package com.cbt.client.util;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class Utils
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public class Utils {

   /**
    * Gson singleton instance for json serialization
    */
   public static final Gson GSON = new GsonBuilder() //
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

   /**
    * No instances allowed
    */
   private Utils() {
   }

   /**
    * Wait for adb connection. Exception will be thrown in case of the timeout.
    *
    * @param adb
    */
   private static void waitForAdb(AndroidDebugBridge adb) {

      int i = 1;
      try {
         while (!adb.isConnected() && i++ < 10) {
            TimeUnit.MILLISECONDS.sleep(100);
         }
      } catch (InterruptedException e) {
         throw new RuntimeException("Interrupted while waiting for adb to connect!", e);
      }

      throw new RuntimeException("Unable to connect to adb");
   }

   /**
    * Initialize adb and wait for established connection
    *
    * @param sdk - android sdk path
    * @return
    */
   public static AndroidDebugBridge initAdb(File sdk) {
      AndroidDebugBridge.initIfNeeded(false);
      File adbPath = FileUtils.getFile(sdk, "platform-tools", "adb");
      AndroidDebugBridge adb = AndroidDebugBridge.createBridge(adbPath.getAbsolutePath(), true);
      waitForAdb(adb);
      return adb;
   }

   /**
    * Disconnect adb and terminate the server
    */
   public static void terminateAdb() {
      AndroidDebugBridge.disconnectBridge();
      AndroidDebugBridge.terminate();
   }

   /**
    * Find all connected devices
    *
    * @param adb
    * @return
    */
   public static Map<String, IDevice> findAllDevices(AndroidDebugBridge adb) {
      Map<String, IDevice> devices = new HashMap<String, IDevice>();
      for (IDevice realDevice : adb.getDevices()) {
         devices.put(realDevice.getSerialNumber(), realDevice);
      }
      return devices;
   }
}
