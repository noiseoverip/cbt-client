package com.cbt.client;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.exec.CommandLine;
import org.apache.log4j.Logger;

import com.cbt.client.annotations.PathAndroidToolAdb;
import com.cbt.ws.entity.TestPackage;


public class AdbApi {

	private static final Logger mLog = Logger.getLogger(AdbApi.class);
	private CliExecutor mExecutor;
	private String mPathAdb;
	private TestPackage mTestPkg;

	@Inject
	public AdbApi(CliExecutor cliExecutor, @PathAndroidToolAdb String pathADB) {
		mExecutor = cliExecutor;
		mPathAdb = pathADB;
	}

	private void execHandleExitValue(final String commandString) throws Exception {
		final CommandLine command = CommandLine.parse(commandString);
		final int exitValue = mExecutor.execute(command);
		mLog.info("Exit value:" + exitValue);
		if (mExecutor.isFailure(exitValue)) {
			throw new Exception("Failed");
		} else {
			mLog.info("Success");
		}
	}
	
	// TODO: parse version properly
	/**
	 * Get adb version
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getAdbVersion() throws Exception {
		String commandString = String.format(mPathAdb + " version");
		CommandLine command = CommandLine.parse(commandString);
		int exitValue = mExecutor.execute(command);
		mLog.info("Exit value:" + exitValue);
		if (mExecutor.isFailure(exitValue)) {
			throw new Exception("Failed");
		} else {
			mLog.info("Success");
		}
		mLog.info("output:\n" + mExecutor.getOutput());
		String version = mExecutor.getOutput();
		return mExecutor.getOutput();
	}

	/**
	 * Get list of device id's
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<String> getDevices() throws Exception {		
		execHandleExitValue(String.format(mPathAdb + " devices"));		
		List<String> devices = null;
		String output = mExecutor.getOutput();
		if (null != output && output.length() > 0) {
			String[] lines = output.split("\\r?\\n");
			if (lines.length > 1) {
				devices = new ArrayList<>(lines.length - 1);
				for (String line : lines) {
					String[] elements = line.split("\\s+");
					if (elements.length == 2) {
						devices.add(elements[0].trim());
					}
				}
			}
		}
		return devices;
	}

}
