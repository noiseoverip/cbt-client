package com.cbt.client;

import javax.inject.Inject;

import org.apache.commons.exec.CommandLine;
import org.apache.log4j.Logger;

import com.cbt.client.annotations.PathAndroidToolAdb;
import com.cbt.ws.entity.TestPackage;

public class TestExecutor {	
	private static final Logger mLog = Logger.getLogger(ApplicationInstaller.class);
	private String mDeviceSerialNumber;
	private CliExecutor mExecutor;
	private String mPathADB;
			
	private TestPackage mTestPkg;

	@Inject
	public TestExecutor(CliExecutor cliExecutor, @PathAndroidToolAdb String pathADB) {
		mExecutor = cliExecutor;
		mPathADB = pathADB;
	}

	// TODO: work on executing different classes
	public void execute() throws Exception {
		/**
		 * call adb shell uiautomator runtest OneButtonUiTest.jar -c com.test.UIPushButtonTest
		 */
		String commandString = String.format(mPathADB + " -s " + mDeviceSerialNumber + " shell uiautomator runtest %s -c %s", mTestPkg.getTestScriptFileName(), "com.test.UIPushButtonTest");
		CommandLine command = CommandLine.parse(commandString);
		
		int exitValue = mExecutor.execute(command);

		mLog.info("Exit value:" + exitValue);
		if (mExecutor.isFailure(exitValue)) {
			throw new Exception("Failed");
		} else {
			mLog.info("Success");
		}
		mLog.info("output:\n" + mExecutor.getOutput());
	}
	
	public void setDeviceSerial(String deviceSerialNumber) {
		mDeviceSerialNumber = deviceSerialNumber;
	}
	
	public void setTestPackage(TestPackage testPkg) {
		mTestPkg = testPkg;
	}

}
