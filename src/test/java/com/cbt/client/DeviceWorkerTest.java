package com.cbt.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.cbt.ws.entity.Device;
import com.cbt.ws.entity.DeviceJob;
import com.cbt.ws.entity.TestPackage;
import com.cbt.ws.jooq.enums.DeviceJobStatus;
import com.cbt.ws.jooq.enums.DeviceState;

/**
 * Unit test for {@link AdbMonitor}
 * 
 * @author SauliusAlisauskas 2013-03-18 Initial version
 * 
 */
public class DeviceWorkerTest {

	private DeviceWorker mUnit;
	private Device mDevice;
	private AdbApi mAdbApi;
	private CbtWsClientApi mWsApi;
	private ApplicationInstaller mInstaller;
	private TestExecutor mTestExecutor;
	private static final List<String> mTestDevices = Arrays.asList("myDevice1", "myDevice2");

	@Before
	public void before() {
		mDevice = new Device();
		mDevice.setState(DeviceState.OFFLINE);
		mDevice.setSerialNumber(mTestDevices.get(0));
		mDevice.setId(new Random().nextLong());
		mAdbApi = mock(AdbApi.class);
		mWsApi = mock(CbtWsClientApi.class);
		mInstaller = mock(ApplicationInstaller.class);
		mTestExecutor = mock(TestExecutor.class);
		mUnit = new DeviceWorker(mAdbApi, mWsApi, mInstaller, mTestExecutor);
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

	@Test
	public void testDeviceJobRun() throws Exception {
		DeviceJob testingDeviceJob = new DeviceJob();
		testingDeviceJob.setDeviceId(mDevice.getId());
		testingDeviceJob.setTestRunId(new Random().nextLong());
		testingDeviceJob.setStatus(DeviceJobStatus.WAITING);

		when(mAdbApi.getDevices()).thenReturn(new ArrayList<String>(mTestDevices));
		when(mWsApi.getWaitingJob(any(Device.class))).thenReturn(testingDeviceJob);

		// run
		mUnit.run();
		
		verify(mWsApi, times(1)).updatedevice(any(Device.class));
		verify(mWsApi, times(1)).getWaitingJob(any(Device.class));
		verify(mInstaller, times(1)).setTestPackage(any(TestPackage.class));
		verify(mInstaller, times(1)).installTestScript(any(String.class));
		verify(mInstaller, times(1)).installApp(any(String.class));
		verify(mTestExecutor, times(1)).execute(any(DeviceJob.class), any(String.class), any(TestPackage.class));		
	}
}
