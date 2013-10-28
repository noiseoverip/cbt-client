package com.cbt.client.device;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.cbt.client.util.Utils;
import com.cbt.client.ws.CbtWsClientException;
import com.cbt.core.entity.Device;
import com.cbt.jooq.enums.DeviceDeviceState;
import com.google.inject.Inject;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Class DeviceMonitor contains implementation for monitoring connected devices. It should be executed periodically on
 * a separate thread. Interaction with the the client thread is done through the
 * {@link com.cbt.client.device.DeviceMonitor.Callback} class
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public class DeviceMonitor implements Callable<Void> {

   /**
    * Callback provides a well defined interface with the main client implementation
    */
   public interface Callback {
      Device deviceOnline(IDevice device) throws CbtWsClientException;

      void deviceUpdate(Device device);

      void deviceWorker(Device device);
   }

   private static Logger logger = Logger.getLogger(DeviceMonitor.class);
   private final Utils utils;
   private Callback callback;
   private Map<String, Device> devices;

   /**
    * Constructor to set utils reference
    */
   @Inject
   public DeviceMonitor(Utils utils) {
      this.devices = new HashMap<String, Device>();
      this.utils = utils;
   }

   /**
    * Get defined callback object
    *
    * @return callback
    */
   public Callback getCallback() {
      return callback;
   }

   /**
    * Set callback
    *
    * @param callback
    */
   public void setCallback(Callback callback) {
      this.callback = callback;
   }

   /**
    * Monitors connected devices through adb and maintains the device list
    *
    * @return null
    * @throws Exception
    */
   @Override
   public Void call() throws Exception {
      Map<String, IDevice> allDevices = utils.findAllDevices(AndroidDebugBridge.getBridge());

      // Update currently registered devices
      Set<String> allDeviceSerials = allDevices.keySet();
      Set<String> existingDeviceSerials = new HashSet<String>(devices.keySet());
      for (String serial : existingDeviceSerials) {
         Device cbtDevice = devices.get(serial);
         if (!allDeviceSerials.contains(serial)) {
            devices.remove(serial);
            cbtDevice.setState(DeviceDeviceState.OFFLINE);
         }

         callback.deviceUpdate(cbtDevice);
      }

      // Adding new online devices
      existingDeviceSerials = devices.keySet();
      for (String serial : allDeviceSerials) {
         IDevice device = allDevices.get(serial);
         if (device.isOnline() && !existingDeviceSerials.contains(serial)) {
            devices.put(serial, callback.deviceOnline(device));
         }
      }

      // Trigger devices worker
      for (Device device : devices.values()) {
         String title;
         synchronized (device) {
            title = device.getTitle();
         }
         if (!"BUSY".equals(title)) {
            callback.deviceWorker(device);
         }
      }

      logger.debug("Devices: " + devices);
      return null;
   }
}
