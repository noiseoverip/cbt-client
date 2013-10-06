package com.cbt.client;

import com.android.ddmlib.IDevice;
import com.cbt.client.configuration.Configuration;
import com.cbt.client.device.DeviceMonitor;
import com.cbt.client.device.DeviceWorker;
import com.cbt.client.ws.CbtWsClientException;
import com.cbt.client.ws.WsClient;
import com.cbt.core.entity.Device;
import com.cbt.core.entity.DeviceType;
import com.cbt.jooq.enums.DeviceState;
import com.google.inject.Injector;
import com.sun.jersey.api.client.ClientHandlerException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CbtClient Tester.
 *
 * @author iljabobkevic 2013-10-04 initial version
 */
public class CbtClientTest {

   private static final Map<String, Object> DUMMY_USER_PROPERTIES = new HashMap<String, Object>();
   private static final long DUMMY_ID = 1L;
   private static final String DUMMY_VALUE = "DUMMY_VALUE";
   private final Configuration conf;
   private final DeviceMonitor monitor;
   private final Injector injector;
   private final WsClient wsClient;

   public CbtClientTest() {
      DUMMY_USER_PROPERTIES.put("id", DUMMY_ID);
      monitor = mock(DeviceMonitor.class);
      injector = mock(Injector.class);
      conf = mock(Configuration.class);
      wsClient = mock(WsClient.class);
   }

   /**
    * Method: setStopped()
    */
   @Test
   public void testStopped() throws Exception {
      CbtClient client = new CbtClient(conf, wsClient, monitor, injector);
      Assert.assertFalse("Client should NOT be marked as stopped initially!", client.isStopped());
      client.setStopped();
      Assert.assertTrue("Client should be marked as stopped!", client.isStopped());
   }

   /**
    * Method: call()
    */
   @Test
   public void testCall() throws Exception {
      CbtClient client = new CbtClient(conf, wsClient, monitor, injector);
      when(wsClient.getUserByName(anyString())).thenReturn(null);
      Assert.assertFalse("Call should return false due to false authentication", client.call());

      when(wsClient.getUserByName(any(String.class))).thenReturn(DUMMY_USER_PROPERTIES);
      client.setStopped();
      Assert.assertTrue("Call should be successful if authentication passed", client.call());
   }

   /**
    * Method: deviceOnline(IDevice device)
    */
   @Test
   public void testDeviceOnline() throws Exception {
      CbtClient client = new CbtClient(conf, wsClient, monitor, injector);
      when(wsClient.registerDevice(any(Device.class))).thenReturn(DUMMY_ID);
      when(conf.getUserId()).thenReturn(DUMMY_ID);

      DeviceType deviceType = mock(DeviceType.class);
      when(deviceType.getId()).thenReturn(DUMMY_ID);
      when(wsClient.syncDeviceType(any(DeviceType.class))).thenReturn(deviceType);

      IDevice idevice = mock(IDevice.class);
      when(idevice.getSerialNumber()).thenReturn(DUMMY_VALUE);

      CbtClient.DeviceMonitorCallback callback = client.new DeviceMonitorCallback();
      Device device = callback.deviceOnline(idevice);

      Assert.assertNotNull("Device object should NOT be null", device);
      Assert.assertEquals("Wrong user id!", Long.valueOf(device.getUserId()), Long.valueOf(DUMMY_ID));
      Assert.assertEquals("Wrong owner id!", Long.valueOf(device.getOwnerId()), Long.valueOf(DUMMY_ID));
      Assert.assertEquals("Wrong serial number!", device.getSerialNumber(), DUMMY_VALUE);
      Assert.assertEquals("Wrong device state!", device.getState(), DeviceState.ONLINE);
      Assert.assertEquals("Wrong device os id!", Long.valueOf(device.getDeviceOsId()), Long.valueOf(1L));
      Assert.assertEquals("Wrong device type id!", Long.valueOf(device.getDeviceTypeId()), Long.valueOf(DUMMY_ID));
      Assert.assertEquals("Wrong device id!", Long.valueOf(device.getId()), Long.valueOf(DUMMY_ID));
   }

   /**
    * Method: deviceUpdate(Device device)
    */
   @Test
   public void testDeviceUpdate() throws Exception {
      CbtClient client = new CbtClient(conf, wsClient, monitor, injector);
      CbtClient.DeviceMonitorCallback callback = client.new DeviceMonitorCallback();
      Device device = mock(Device.class);

      // Make sure plain call doesn't result any exceptions
      callback.deviceUpdate(device);

      // Make sure CbtWsClientException is handled
      doThrow(CbtWsClientException.class).doNothing().when(wsClient).updateDevice(any(Device.class));
      callback.deviceUpdate(device);

      // Make sure ClientHandlerException is handled
      doThrow(ClientHandlerException.class).doNothing().when(wsClient).updateDevice(any(Device.class));
      callback.deviceUpdate(device);
   }

   /**
    * Method: deviceWorker(Device device)
    */
   @Test
   public void testDeviceWorker() throws Exception {
      when(injector.getInstance(DeviceWorker.class)).thenReturn(mock(DeviceWorker.class));
      CbtClient client = new CbtClient(conf, wsClient, monitor, injector);
      CbtClient.DeviceMonitorCallback callback = client.new DeviceMonitorCallback();
      Device device = new Device();
      callback.deviceWorker(device);
      Assert.assertEquals("Wrong device title", device.getTitle(), CbtClient.DEVICE_TITLE_BUSY);
   }

   /**
    * Method: authenticate()
    */
   @Test
   public void testAuthenticate() throws Exception {
      CbtClient client = new CbtClient(conf, wsClient, monitor, injector);
      when(wsClient.getUserByName(anyString())).thenReturn(null);
      Assert.assertFalse("Call should return false due to false authentication", client.authenticate());
      when(wsClient.getUserByName(anyString())).thenReturn(DUMMY_USER_PROPERTIES);
      Assert.assertTrue("Authentication should return true if user properties are set", client.authenticate());
   }

} 
