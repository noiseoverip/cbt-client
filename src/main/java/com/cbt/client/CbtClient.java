package com.cbt.client;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cbt.annotations.UserId;
import com.cbt.client.adb.AdbApi;
import com.cbt.clientws.CbtClientException;
import com.cbt.clientws.CbtWsClientApi;
import com.cbt.clientws.StatusUpdater;
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
	private Store mStore;
	private ScheduledExecutorService mDeviceMonitorExecutor;
	private StatusUpdater mStatusUpdater;

	private static final Logger mLog = Logger.getLogger(ApplicationInstaller.class);

	@Inject
	public CbtClient(StatusUpdater statusUpdater, IApplicationInstaller installer, ITestExecutor testExecutor, AdbApi adbApi, CbtWsClientApi wsApi, @UserId Long userId, Store store) {
		mInstaller = installer;
		mTestExecutor = testExecutor;
		mAdbApi = adbApi;
		mWsApi = wsApi;
		mUserId = userId;
		mStore = store;
		mStatusUpdater = statusUpdater;
	}

	public void start() {		
		
		//TODO: make this a periodic task
		// Scan devices
		List<String> deviceNames = null;
		try {
			deviceNames = mAdbApi.getDevices();
		} catch (Exception e) {
			mLog.error("Could not find any device attached");
			return;
		}
		for (String deviceName : deviceNames) {
			mLog.info("Checking device:" + deviceName);
			Device device = new Device();
			Long deviceId = mStore.getDeviceId(deviceName);
			if (deviceId != null) {
				mLog.info("Device found to be registered, name: " + deviceName + " id:" + deviceId);
				device.setId(deviceId);
			}			
			device.setUserId(mUserId);
			device.setDeviceTypeId(1L);
			device.setDeviceOsId(1L);		
			device.setSerialNumber(deviceName);
			
			if (null == deviceId) {
				try {
					deviceId = mWsApi.registerDevice(device);
				} catch (CbtClientException e) {
					mLog.error("Could not registerdevice:" + device);
				}
			}
			if (null != deviceId && deviceId > 0) {
				mLog.info("Success register/loaded device:" + deviceName + " id:" + deviceId);
				mStore.addDevice(device);
			}
		}
		
		mDeviceMonitorExecutor = Executors.newScheduledThreadPool(1);
		mDeviceMonitorExecutor.scheduleAtFixedRate(mStatusUpdater, 1, 2, TimeUnit.SECONDS);
		
//		try {
//			TimeUnit.SECONDS.sleep(30);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
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
