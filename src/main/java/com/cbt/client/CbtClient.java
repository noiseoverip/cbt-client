package com.cbt.client;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cbt.client.annotations.UserId;
import com.cbt.ws.entity.Device;

/**
 * Main CBT client class
 * 
 * @author SauliusAlisauskas 2013-03-19 Initial version
 *
 */
public class CbtClient {
	private ApplicationInstaller mInstaller;
	private TestExecutor mTestExecutor;
	private AdbApi mAdbApi;
	private CbtWsClientApi mWsApi;
	private Long mUserId;
	private Store mStore;
	private ScheduledExecutorService mDeviceMonitorExecutor;
	private StatusUpdater mStatusUpdater;

	private static final Logger mLog = Logger.getLogger(ApplicationInstaller.class);

	@Inject
	public CbtClient(StatusUpdater statusUpdater, ApplicationInstaller installer, TestExecutor testExecutor, AdbApi adbApi, CbtWsClientApi wsApi, @UserId Long userId, Store store) {
		mInstaller = installer;
		mTestExecutor = testExecutor;
		mAdbApi = adbApi;
		mWsApi = wsApi;
		mUserId = userId;
		mStore = store;
		mStatusUpdater = statusUpdater;
	}

	public void start() {		
		
		mDeviceMonitorExecutor = Executors.newScheduledThreadPool(1);
		mDeviceMonitorExecutor.scheduleAtFixedRate(mStatusUpdater, 1, 2, TimeUnit.SECONDS);
		
		try {
			TimeUnit.SECONDS.sleep(30);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mDeviceMonitorExecutor.shutdownNow();
		
		
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
