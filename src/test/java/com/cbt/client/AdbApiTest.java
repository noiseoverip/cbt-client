package com.cbt.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.cbt.client.AdbApi;
import com.cbt.client.CliExecutor;

public class AdbApiTest {
	private Logger mLogger = Logger.getLogger(AdbApiTest.class);
	
	//private String mPathAdb = "$ANDROID_HOME/platform-tools/adb";
	private String mPathAdb = "/home/saulius/Documents/dev/adt-bundle-linux-x86_64-20130219/sdk/platform-tools/adb";
	//private String pathStorage = "/home/saulius/Documents/cbt";
	
	/**
	 * Test {@link DeviceApi#getDevices()}
	 * 
	 * Prerequisite: need to have at least one device connected
	 * @throws Exception 
	 */
	@Test
	public void testGetDevices() throws Exception {
		AdbApi api = new AdbApi(new CliExecutor(), mPathAdb);
		List<String> devices = api.getDevices();
		assertNotNull("Returned NULL", devices);
		assertTrue("Found 0 devices",devices.size() > 0);
		mLogger.info("Found devices:" + devices);
	}
	
//	@Test
//	public void testGetVersion() throws Exception {
//		AdbApi api = new AdbApi(new CliExecutor(), mPathAdb);
//		String version = api.getAdbVersion();
//		assertNotNull("Returned NULL", version);
//		mLogger.info("Version:" + version);
//	}
}
