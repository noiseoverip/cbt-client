package com.cbt.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.cbt.client.AdbMonitor.Callback;
import com.cbt.ws.entity.Device;

/**
 * Unit test for {@link AdbMonitor}
 * 
 * @author SauliusAlisauskas 2013-03-18 Initial version
 *
 */
public class AdbMonitorTest {
	
	private AdbMonitor mUnit;
	private static final List<String> mTestDevices = Arrays.asList("myDevice1", "myDevice2");
	
	@Test
	public void test() throws Exception {
//		// Initial setup
//		final Long userId = 1L;
//		final AdbApi adbApi = mock(AdbApi.class);
//		final Store store = new Store();
//		CbtWsClientApi wsApi = mock(CbtWsClientApi.class);
//		mUnit = new AdbMonitor(userId, adbApi, store, wsApi);
//		mUnit.setCallback(new Callback() {			
//			@Override
//			public void onNewDeviceFound(Device device) {
//				store.addDevice(device);				
//			}
//		});
//		
//		// Test that new device get registered
//		when(adbApi.getDevices()).thenReturn(new ArrayList<String>(mTestDevices));
//		when(wsApi.registerDevice(any(Device.class))).thenReturn(Long.valueOf(new Random().nextInt(100)));
//		mUnit.run();		
//		verify(adbApi).getDevices();
//		verify(wsApi, times(mTestDevices.size())).registerDevice(any(Device.class));		
//		
//		// Test that if new device is added it will get registered and already existing ones will not invoke registration process
//		List<String> devices = new ArrayList<String>(mTestDevices);
//		devices.add("myDevice3");
//		when(adbApi.getDevices()).thenReturn(devices);		
//		mUnit.run();
//		verify(wsApi, times(mTestDevices.size() + 1)).registerDevice(any(Device.class));
	}
}
