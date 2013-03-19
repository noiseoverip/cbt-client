package com.cbt.client;

import javax.inject.Inject;

import org.apache.commons.exec.CommandLine;
import org.apache.log4j.Logger;

import com.cbt.client.annotations.PathAndroidToolAdb;

public class TestExecutor {

	
	private String mPathADB;
	private TestPackage mTestPkg;
	private CliExecutor mExecutor;	
			
	@Inject
	public TestExecutor(CliExecutor cliExecutor, @PathAndroidToolAdb String pathADB) {
		mExecutor = cliExecutor;
		mPathADB = pathADB;
	}

	private static final Logger mLog = Logger.getLogger(ApplicationInstaller.class);

	public void setTestPackage(TestPackage testPkg) {
		mTestPkg = testPkg;
	}

	public void execute() throws Exception {
		/**
		 * call adb shell uiautomator runtest OneButtonUiTest.jar -c com.test.UIPushButtonTest
		 */
		String commandString = String.format(mPathADB + " shell uiautomator runtest %s -c %s", mTestPkg.getTestFileName(), mTestPkg.getTestClassName());
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

}
