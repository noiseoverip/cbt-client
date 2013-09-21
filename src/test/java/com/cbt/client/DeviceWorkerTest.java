package com.cbt.client;

import com.cbt.ws.entity.Device;
import com.cbt.ws.jooq.enums.DeviceState;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UT for {@link DeviceWorker}
 *
 * @author SauliusAlisauskas 2013-03-18 Initial version
 */
public class DeviceWorkerTest {

   private DeviceWorker mUnit;
   private Device mDevice;
   private AdbApi mAdbApi;
   private CbtWsClientApi mWsApi;
   private static final List<String> mTestDevices = Arrays.asList("myDevice1", "myDevice2");
   private Configuration mConfig;

   @BeforeMethod
   public void before() {
      mDevice = new Device();
      mDevice.setState(DeviceState.OFFLINE);
      mDevice.setSerialNumber(mTestDevices.get(0));
      mDevice.setId(new Random().nextLong());
      mAdbApi = mock(AdbApi.class);
      mWsApi = mock(CbtWsClientApi.class);
      mConfig = mock(Configuration.class);
      mUnit = new DeviceWorker(mAdbApi, mWsApi, mConfig);
      mUnit.setDevice(mDevice);
   }

   @Test
   public void testRunDeviceFound() throws Exception {
      when(mAdbApi.getDevices()).thenReturn(new ArrayList<String>(mTestDevices));

      // run
      mUnit.run();

      verify(mWsApi, times(1)).updatedevice(any(Device.class));
      verify(mWsApi, times(1)).getWaitingJob(any(Device.class));
      Assert.assertEquals(DeviceState.ONLINE, mDevice.getState());
   }

   @Test
   public void testRunDeviceNotFOund() throws Exception {
      when(mAdbApi.getDevices()).thenReturn(new ArrayList<String>());

      // run
      mUnit.run();

      verify(mWsApi, times(1)).updatedevice(any(Device.class));
      verify(mWsApi, times(0)).getWaitingJob(any(Device.class));
      Assert.assertEquals(DeviceState.OFFLINE, mDevice.getState());
   }

// Fails with spoon for now
//	@Test
//	public void testDeviceJobRun() throws Exception {
//		DeviceJob testingDeviceJob = new DeviceJob();
//		testingDeviceJob.setDeviceId(mDevice.getId());
//		testingDeviceJob.setTestRunId(new Random().nextLong());
//		testingDeviceJob.setStatus(DeviceJobStatus.WAITING);
//
//		when(mAdbApi.getDevices()).thenReturn(new ArrayList<String>(mTestDevices));
//		when(mWsApi.getWaitingJob(any(Device.class))).thenReturn(testingDeviceJob);
//
//		// run
//		mUnit.run();
//
//		verify(mWsApi, times(1)).updatedevice(any(Device.class));
//		verify(mWsApi, times(1)).getWaitingJob(any(Device.class));
//	}
}
