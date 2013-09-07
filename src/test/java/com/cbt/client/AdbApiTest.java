package com.cbt.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * UT for {@link AdbApi}
 * 
 * @author SauliusAlisauskas 2013-03-22 Initial version
 *
 */
public class AdbApiTest {
	Configuration mConfiguration;
	CliExecutor mCliExecutor;
	
	@BeforeMethod
	public void before() {
		// Mock/re-mock redquired object before each test method
		mCliExecutor = mock(CliExecutor.class);
		mConfiguration = mock(Configuration.class);
	}
	
	/**
	 * Test {@link AdbApi#getDevices()}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetDevices() throws Exception {
		AdbApi api = new AdbApi(mCliExecutor, mConfiguration);
		
		when(mCliExecutor.getOutput()).thenReturn(
				Resources.toString(Resources.getResource("adbOutputDevices.txt"), Charsets.UTF_8));
		
		List<String> devices = api.getDevices();
		assertNotNull(devices, "Returned NULL");
		
		final int expectedNumberOfDevicesInList = 3;
		assertEquals(devices.size(), expectedNumberOfDevicesInList);		
	}	
}
