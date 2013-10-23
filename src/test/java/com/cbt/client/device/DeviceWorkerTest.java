package com.cbt.client.device;

import com.cbt.client.configuration.Configuration;
import com.cbt.client.util.Utils;
import com.cbt.client.ws.WsClient;
import com.cbt.core.entity.Device;
import com.cbt.core.entity.DeviceJob;
import com.cbt.core.entity.DeviceJobResult;
import com.cbt.core.entity.TestScript;
import com.cbt.jooq.enums.TestscriptTestscriptType;
import com.google.common.io.Files;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * DeviceWorker Tester.
 *
 * @author iljabobkevic 2013-10-17 initial version
 */
public class DeviceWorkerTest {

   private static final long DUMMY_LONG = 123L;
   private static final String DUMMY_STRING = "dummy";
   private final Logger logger = Logger.getLogger(DeviceWorkerTest.class);
   private Configuration conf;
   private WsClient wsClient;
   private Utils utils;

   @Before
   public void before() {
      conf = mock(Configuration.class);
      wsClient = mock(WsClient.class);
      utils = mock(Utils.class);
   }

   /**
    * Method: getDevice()
    * Method: setDevice(Device device)
    */
   @Test
   public void testGetSetDevice() throws Exception {
      DeviceWorker worker = new DeviceWorker(conf, wsClient, utils);
      Device device = mock(Device.class);
      worker.setDevice(device);
      Assert.assertEquals("Device should be equal to the one defined through the setter!", device, worker.getDevice());
   }

   /**
    * Method: call()
    */
   @Test
   public void testCall() throws Exception {
      DeviceWorker worker = spy(new DeviceWorker(conf, wsClient, utils));
      when(conf.getWorkspace()).thenReturn(Files.createTempDir());
      DeviceJobResult deviceJobResult = mock(DeviceJobResult.class);
      doReturn(deviceJobResult).when(worker).runTest(any(Device.class), any(DeviceJob.class), anyBoolean());

      Device device = mock(Device.class);
      worker.setDevice(device);

      // Handle null DeviceJob
      when(wsClient.getWaitingJob(any(Device.class))).thenReturn(null);

      // Test
      Assert.assertNull(worker.call());
      verify(wsClient).getWaitingJob(device);
      verify(device).setTitle(DeviceWorker.DEVICE_TITLE_FREE);

      // Handle not null DeviceJob
      // TestScript
      TestScript testScript = mock(TestScript.class);
      when(testScript.getTestScriptType()).thenReturn(TestscriptTestscriptType.UIAUTOMATOR);

      // DeviceJob
      DeviceJob deviceJob = mock(DeviceJob.class);
      when(deviceJob.getId()).thenReturn(DUMMY_LONG);
      when(deviceJob.getTestScript()).thenReturn(testScript);

      // Device
      when(device.getSerialNumber()).thenReturn(DUMMY_STRING);

      // WsClient
      when(wsClient.getWaitingJob(any(Device.class))).thenReturn(deviceJob);

      // Test with uiautomator
      Assert.assertNull(worker.call());
      verify(wsClient).receiveTestPackage(DUMMY_LONG, DUMMY_STRING);
      verify(wsClient).publishDeviceJobResult(deviceJobResult);
      verify(wsClient, times(2)).getWaitingJob(device);
      verify(worker).runTest(device, deviceJob, true);
      verify(device, times(2)).setTitle(DeviceWorker.DEVICE_TITLE_FREE);

      // Test with instrumentation
      when(testScript.getTestScriptType()).thenReturn(TestscriptTestscriptType.INSTRUMENTATION);
      Assert.assertNull(worker.call());
      verify(wsClient, times(2)).receiveTestPackage(DUMMY_LONG, DUMMY_STRING);
      verify(wsClient, times(2)).publishDeviceJobResult(deviceJobResult);
      verify(wsClient, times(3)).getWaitingJob(device);
      verify(worker).runTest(device, deviceJob, false);
      verify(device, times(3)).setTitle(DeviceWorker.DEVICE_TITLE_FREE);
   }
}
