package com.cbt.client;

import javax.inject.Inject;

import org.apache.commons.exec.CommandLine;
import org.apache.log4j.Logger;

import com.cbt.ws.entity.TestPackage;

/**
 * Helper class for installing files and application on to Android device
 * 
 * @author SauliusAlisauskas
 *
 */
public class AndroidApplicationInstaller {

	private static final Logger mLog = Logger.getLogger(AndroidApplicationInstaller.class);

	private CliExecutor mExecutor;
	private String mPathADB;
	private TestPackage mTestPkg;
	private String mWorkspacePath;

	@Inject
	public AndroidApplicationInstaller(CliExecutor cliExecutor, Configuration config) {
		mExecutor = cliExecutor;
		mPathADB = config.getPathAndroidADB();
		mWorkspacePath = config.getPathWorkspace();
	}

	public void installApp(String deviceSerial) throws Exception {
		String commandString = mPathADB + " -s " + deviceSerial + " install -r " + mWorkspacePath
				+ mTestPkg.getTestTargetFileName();
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

	public void installTestScript(String deviceSerial) throws Exception {
		String commandString = String.format(mPathADB + " -s " + deviceSerial + " push " + mWorkspacePath
				+ "%s /data/local/tmp", mTestPkg.getTestScriptFileName());
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

	public void setTestPackage(TestPackage testPkg) {
		mTestPkg = testPkg;
	}
}
