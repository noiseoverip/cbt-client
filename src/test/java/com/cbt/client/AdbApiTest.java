package com.cbt.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.mockito.Mockito;

public class AdbApiTest {
	private Logger mLogger = Logger.getLogger(AdbApiTest.class);
	private static final String ADB_PATH = "blablabla";
	/**
	 * Test {@link DeviceApi#getDevices()}
	 * 
	 * Prerequisite: need to have at least one device connected
	 * @throws Exception 
	 */
	@Test
	public void testGetDevices() throws Exception {
		CliExecutor cliExecutor = Mockito.mock(CliExecutor.class);
		Configuration config = Mockito.mock(Configuration.class);
		AdbApi api = new AdbApi(cliExecutor, config);
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
