package com.cbt.executor;

import javax.inject.Inject;

import org.apache.commons.exec.CommandLine;
import org.apache.log4j.Logger;

import com.cbt.annotations.PathAndroidToolAdb;
import com.cbt.cliexecutor.ICliExecutor;
import com.cbt.installer.ApplicationInstaller;
import com.cbt.model.TestPackage;

public class TestExecutor implements ITestExecutor {

	
	private String mPathADB;
	private TestPackage mTestPkg;
	private ICliExecutor mExecutor;	
			
	@Inject
	public TestExecutor(ICliExecutor cliExecutor, @PathAndroidToolAdb String pathADB) {
		mExecutor = cliExecutor;
		mPathADB = pathADB;
	}

	private static final Logger mLog = Logger.getLogger(ApplicationInstaller.class);

	@Override
	public void setTestPackage(TestPackage testPkg) {
		mTestPkg = testPkg;
	}

	@Override
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
