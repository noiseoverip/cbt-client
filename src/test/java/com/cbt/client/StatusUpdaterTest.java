package com.cbt.client;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.cbt.ws.entity.Device;
import com.cbt.ws.jooq.enums.DeviceState;

/**
 * Unit test for {@link StatusUpdater}
 * 
 * @author SauliusAlisauskas 2013-03-18 Initial version
 *
 */
public class StatusUpdaterTest {
	
	private StatusUpdater mUnit;
	private static final List<String> mTestDevices = Arrays.asList("myDevice1", "myDevice2");
	
	@Test
	public void test() throws Exception {
		// Initial setup
		Long userId = 1L;
		AdbApi adbApi = mock(AdbApi.class);
		Store store = new Store();
		CbtWsClientApi wsApi = mock(CbtWsClientApi.class);
		mUnit = new StatusUpdater(userId, adbApi, store, wsApi);		
		
		// Test that new device get registered
		when(adbApi.getDevices()).thenReturn(new ArrayList<String>(mTestDevices));
		when(wsApi.registerDevice(any(Device.class))).thenReturn(Long.valueOf(new Random().nextInt(100)));
		mUnit.run();		
		verify(adbApi).getDevices();
		verify(wsApi, times(mTestDevices.size())).registerDevice(any(Device.class));
		
		// Test that same devices get updated
		mUnit.run();
		verify(wsApi, times(mTestDevices.size())).updatedevice(any(Device.class));
		
		// Test that if new device is added it will get registered
		List<String> devices = new ArrayList<String>(mTestDevices);
		devices.add("myDevice3");
		when(adbApi.getDevices()).thenReturn(devices);		
		mUnit.run();
		verify(wsApi, times(mTestDevices.size() + 1)).registerDevice(any(Device.class));
		verify(wsApi, times(mTestDevices.size() * 2)).updatedevice(any(Device.class));
		
		// Test that if device is disconnected it's state will change to OFFLINE
		when(adbApi.getDevices()).thenReturn(new ArrayList<String>(0));
		mUnit.run();
		Assert.assertEquals(DeviceState.OFFLINE, store.getDevices().get(0).getState());
		Assert.assertEquals(DeviceState.OFFLINE, store.getDevices().get(1).getState());
		Assert.assertEquals(DeviceState.OFFLINE, store.getDevices().get(2).getState());		
	}
}
