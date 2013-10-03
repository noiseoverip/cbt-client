//package com.cbt.client;
//
//import com.cbt.client.configuration.Configuration;
//import com.cbt.ws.entity.Device;
//import com.cbt.ws.entity.DeviceType;
//import org.testng.annotations.Test;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Random;
//
//import static org.mockito.Mockito.*;
//
///**
// * UT for {@link AdbMonitor}
// *
// * @author SauliusAlisauskas 2013-03-18 Initial version
// */
//public class AdbMonitorTest {
//
//   private static final List<String> DUMMY_DEVICE_NAME = Arrays.asList("myDevice1", "myDevice2");
//
//   @Test
//   public void test() throws Exception {
//
//      final Configuration config = mock(Configuration.class);
//
//      final AdbApi adbApi = mock(AdbApi.class);
//      when(adbApi.getDevices()).thenReturn(DUMMY_DEVICE_NAME);
//
//      final Store store = new Store();
//
//      final DeviceInfoCollector deviceInfoCollector = mock(DeviceInfoCollector.class);
//      DeviceType deviceType = new DeviceType("dummymanufacture", "dummymodel");
//      deviceType.setId(1L);
//      when(deviceInfoCollector.getDeviceTypeInfo(any(String.class))).thenReturn(deviceType);
//
//      final CbtWsClientApi wsApi = mock(CbtWsClientApi.class);
//      when(wsApi.registerDevice(any(Device.class))).thenReturn(Long.valueOf(new Random().nextInt(100)));
//      when(wsApi.syncDeviceType(deviceType)).thenReturn(deviceType);
//
//      AdbMonitor monitor = new AdbMonitor(config, adbApi, store, wsApi, deviceInfoCollector);
//      monitor.setCallback(new AdbMonitor.Callback() {
//
//         @Override
//         public void onNewDeviceFound(Device device) {
//            store.addDevice(device);
//         }
//      });
//
//      // Run and verify that call to scan for new devices was sent to adb and that expected number of device
//      // registration were sent
//      monitor.run();
//      verify(adbApi).getDevices();
//      verify(wsApi, times(DUMMY_DEVICE_NAME.size())).registerDevice(any(Device.class));
//
//      // Test that if new device is added it will get registered and already existing ones will not invoke
//      // registration process
//      List<String> devices = new ArrayList<String>(DUMMY_DEVICE_NAME);
//      devices.add("myDevice3");
//      when(adbApi.getDevices()).thenReturn(devices);
//      monitor.run();
//      verify(wsApi, times(DUMMY_DEVICE_NAME.size() + 1)).registerDevice(any(Device.class));
//   }
//}
