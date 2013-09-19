package com.cbt.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.log4j.Logger;

/**
 * Helper class for executing CLI commands
 * 
 * @author SauliusAlisuaskas 2013-03-22 Initial version
 *
 */
public class CliExecutor {

	private static final Logger mLog = Logger.getLogger(CliExecutor.class);
	private DefaultExecutor mExecutor;
	private ByteArrayOutputStream mStdout;

	public int execute(CommandLine command) throws ExecuteException, IOException {
		mLog.info("exec sync: " + command.toString());
		mStdout = new ByteArrayOutputStream();
		getExecutor().setStreamHandler(new PumpStreamHandler(mStdout));
    return getExecutor().execute(command);
	}

	private DefaultExecutor getExecutor() {
		if (null == mExecutor) {
			try {
				init();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return mExecutor;
	}

	public String getOutput() {
		String output = mStdout.toString();
		try {
			mStdout.flush();
		} catch (IOException e) {
			mLog.error("Could not flush output stream");
		}
		return output;
	}

	private void init() throws IOException {
		ExecuteWatchdog watchDog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);

		// This is used to end the process when the JVM exits
		ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();

		// Our main command executor
		mExecutor = new DefaultExecutor();

		// Setting the properties
		mExecutor.setWatchdog(watchDog);
		// Setting the working directory
		// Use of recursion along with the ls makes this a long running process
		// mExecutor.setWorkingDirectory(new File(WORKING_DIR));
		mExecutor.setProcessDestroyer(processDestroyer);
	}

	public boolean isFailure(int exitValue) {
		return getExecutor().isFailure(exitValue);
	}
}
