package com.cbt.installer;

import javax.inject.Inject;

import org.apache.commons.exec.CommandLine;
import org.apache.log4j.Logger;

import com.cbt.annotations.PathAndroidToolAdb;
import com.cbt.cliexecutor.ICliExecutor;
import com.cbt.model.TestPackage;

public class ApplicationInstaller implements IApplicationInstaller {

	private static final Logger mLog = Logger.getLogger(ApplicationInstaller.class);

	private ICliExecutor mExecutor;
	private TestPackage mTestPkg;
	private String mPathADB;

	/**
	 * Workspace environmental variable
	 */
	private static final String ENV_CBT_WS = "c:\\Dev\\CBT\\";

	@Inject
	public ApplicationInstaller(ICliExecutor cliExecutor, @PathAndroidToolAdb String pathAdb) {
		mExecutor = cliExecutor;
		mPathADB = pathAdb;
	}

	@Override
	public void setTestPackage(TestPackage testPkg) {
		mTestPkg = testPkg;
	}

	@Override
	public void installApp() throws Exception {

		String commandString = mPathADB + " install -r " + ENV_CBT_WS + "apps\\" + mTestPkg.getAppFileName();		
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

	@Override
	public void installTest() throws Exception {
		String commandString = String.format(mPathADB + " push " + ENV_CBT_WS + "tests\\%s /data/local/tmp",
				mTestPkg.getTestFileName());	
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
