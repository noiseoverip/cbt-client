package com.cbt.client;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cbt.executor.ITestExecutor;
import com.cbt.installer.ApplicationInstaller;
import com.cbt.installer.IApplicationInstaller;
import com.cbt.model.TestPackage;

public class CbtClient {

	private IApplicationInstaller mInstaller;
	private ITestExecutor mTestExecutor;

	private static final Logger mLog = Logger.getLogger(ApplicationInstaller.class);

	@Inject
	public CbtClient(IApplicationInstaller installer, ITestExecutor testExecutor) {
		mInstaller = installer;
		mTestExecutor = testExecutor;
	}

	public void start() {

		TestPackage testPackage = new TestPackage();
		testPackage.setAppFileName("OneButton.apk");
		testPackage.setAppPackageName("com.onebutton");
		testPackage.setTestFileName("OneButtonUiTest.jar");
		testPackage.setTestClassName("com.test.UIPushButtonTest");
		
		mInstaller.setTestPackage(testPackage);
		
		// Install target application
		mLog.info("Trying to install application on to device");
		try {
			mInstaller.installApp();
		} catch (Exception e) {
			mLog.error("Could not install application on to device", e);
			return;
		}

		// Install test
		mLog.info("Trying to install test JAR file");
		try {
			mInstaller.installTest();
		} catch (Exception e) {
			mLog.error("Could not install application on to device", e);
			return;
		}
		
		mTestExecutor.setTestPackage(testPackage);
		try {
			mTestExecutor.execute();
		} catch (Exception e) {
			mLog.error("Could not install application on to device", e);
			return;
		}
	}

}
