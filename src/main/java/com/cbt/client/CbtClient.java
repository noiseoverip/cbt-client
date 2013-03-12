package com.cbt.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cbt.annotations.UserId;
import com.cbt.client.adb.AdbApi;
import com.cbt.clientws.CbtWsClientApi;
import com.cbt.executor.ITestExecutor;
import com.cbt.installer.ApplicationInstaller;
import com.cbt.installer.IApplicationInstaller;
import com.cbt.ws.entity.Device;

public class CbtClient {

	private IApplicationInstaller mInstaller;
	private ITestExecutor mTestExecutor;
	private AdbApi mAdbApi;
	private CbtWsClientApi mWsApi;
	private Long mUserId;

	private static final Logger mLog = Logger.getLogger(ApplicationInstaller.class);

	@Inject
	public CbtClient(IApplicationInstaller installer, ITestExecutor testExecutor, AdbApi adbApi, CbtWsClientApi wsApi, @UserId Long userId) {
		mInstaller = installer;
		mTestExecutor = testExecutor;
		mAdbApi = adbApi;
		mWsApi = wsApi;
		mUserId = userId;
	}

	public void start() {
		
		// Scan devices
		List<String> deviceNames = null;
		try {
			deviceNames = mAdbApi.getDevices();
		} catch (Exception e) {
			mLog.error("Could not find any device attached");
			return;
		}
		for (String deviceName : deviceNames) {
			Device device = new Device();
			device.setUserId(mUserId);
			device.setDeviceTypeId(1L);
			device.setDeviceOsId(1L);
			
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				mLog.error(e);
			}
			
			//TODO: fix digest generation
			String uniqueId = md.digest(String.valueOf(mUserId + deviceName).getBytes()).toString();			
			device.setDeviceUniqueId(uniqueId);
			
			Long deviceId = mWsApi.registerDevice(device);
			if (null != deviceId && deviceId > 0) {
				mLog.info("Success register device:" + deviceName + " id:" + deviceId);
			}
		}
		
		// Register devices
		
//		TestPackage testPackage = new TestPackage();
//		testPackage.setAppFileName("OneButton.apk");
//		testPackage.setAppPackageName("com.onebutton");
//		testPackage.setTestFileName("OneButtonUiTest.jar");
//		testPackage.setTestClassName("com.test.UIPushButtonTest");
//		
//		mInstaller.setTestPackage(testPackage);
//		
//		// Install target application
//		mLog.info("Trying to install application on to device");
//		try {
//			mInstaller.installApp();
//		} catch (Exception e) {
//			mLog.error("Could not install application on to device", e);
//			return;
//		}
//
//		// Install test
//		mLog.info("Trying to install test JAR file");
//		try {
//			mInstaller.installTest();
//		} catch (Exception e) {
//			mLog.error("Could not install application on to device", e);
//			return;
//		}
//		
//		mTestExecutor.setTestPackage(testPackage);
//		try {
//			mTestExecutor.execute();
//		} catch (Exception e) {
//			mLog.error("Could not install application on to device", e);
//			return;
//		}
	}

}
