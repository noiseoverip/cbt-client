package com.cbt.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.exec.CommandLine;
import org.apache.log4j.Logger;

import com.cbt.core.exceptions.CbtTestResultParseExeception;
import com.cbt.ws.entity.DeviceJob;
import com.cbt.ws.entity.DeviceJobResult;
import com.cbt.ws.entity.DeviceJobResult.JunitTestSummary;
import com.cbt.ws.entity.TestPackage;
import com.cbt.ws.jooq.enums.DeviceJobResultState;
import com.google.common.base.Joiner;

/**
 * Class responsible for executing tests through ADB and parsing results
 * 
 * @author SauliusAlisauskas 2013-03-22 Initial version
 *
 */
public class TestExecutor {
	private CliExecutor mExecutor;
	private String mPathADB;
	private Pattern mPaternSuccess = Pattern.compile("^OK \\((\\d+) (test|tests)\\)$", Pattern.MULTILINE);
	private Pattern mPatternFailed = Pattern.compile("^Tests run: (\\d+),  Failures: (\\d+),  Errors: (\\d+)$");
	private final Logger mLogger = Logger.getLogger(TestExecutor.class);

	@Inject
	public TestExecutor(CliExecutor cliExecutor, Configuration config) {
		mExecutor = cliExecutor;
		mPathADB = config.getPathAndroidADB();
	}

	/**
	 * Execute tests cases specified in 
	 * 
	 * @return
	 * @throws Exception
	 */
	public DeviceJobResult execute(DeviceJob deviceJob, String deviceSerialNumber, TestPackage testPackage) throws Exception {
		mLogger.debug("Executing test on:" + deviceSerialNumber + deviceJob);
		/**
		 * call adb shell uiautomator runtest OneButtonUiTest.jar -c com.test.UIPushButtonTest
		 */
		String commandString = String.format(mPathADB + " -s " + deviceSerialNumber
				+ " shell uiautomator runtest %s -c %s", testPackage.getTestScriptFileName(),
				Joiner.on(",").join(deviceJob.getMetadata().getTestClasses()));
		CommandLine command = CommandLine.parse(commandString);

		int exitValue = mExecutor.execute(command);

		mLogger.debug("Exit value:" + exitValue);
		if (mExecutor.isFailure(exitValue)) {
			throw new Exception("Failed");
		} else {
			mLogger.debug("Success");
		}
		String output = mExecutor.getOutput();
		mLogger.debug("Test Output:\n" + output);

		DeviceJobResult testResult = new DeviceJobResult();
		testResult.setDevicejobId(testPackage.getDevicejobId());
		parseTestOutput(output, testResult);
		return testResult;
	}

	/**
	 * Parse test output;
	 * 
	 * @param output
	 * @param testResult
	 * @throws CbtTestResultParseExeception
	 */
	protected void parseTestOutput(String output, DeviceJobResult testResult) throws CbtTestResultParseExeception {
		testResult.setOutput(output);
		Matcher successMatcher = mPaternSuccess.matcher(output);
		if (successMatcher.find()) {
			mLogger.debug("Tests PASSED for");
			testResult.setTestsRun(Integer.valueOf(successMatcher.group(1)));
			testResult.setState(DeviceJobResultState.PASSED);
			testResult.setTestsErrors(0);
			testResult.setTestsFailed(0);
		} else {
			testResult.setState(DeviceJobResultState.FAILED);
			mLogger.debug("Tests FAILED");
			String[] lines = output.split("\n");
			for (String line : lines) {
				if (line.startsWith("Tests run:")) {
					/**
					 * Possible output line example: Tests run: 1, Failures: 3, Errors: 100 Need to extract numbers 1 , 3,
					 * 100
					 */
					Matcher matcher = mPatternFailed.matcher(line);
					if (matcher.groupCount() == 3 && matcher.find()) {
						for (int i = 0; i < JunitTestSummary.values().length; i++) {
							JunitTestSummary summaryItem = JunitTestSummary.values()[i];
							Integer value = Integer.valueOf(matcher.group(i + 1));
							switch (summaryItem) {
							case ERRORS:
								testResult.setTestsErrors(value);
								break;
							case FAILURES:
								testResult.setTestsFailed(value);
								break;
							case TESTSRUN:
								testResult.setTestsRun(value);
								break;
							}
						}
					} else {
						throw new CbtTestResultParseExeception("Unexpected test summary line format:" + line
								+ " matcherGroupCount=" + matcher.groupCount());
					}
				}
			}
			if (testResult.getTestsErrors() == null || testResult.getTestsFailed() == null || testResult.getTestsRun() == null) {
				throw new CbtTestResultParseExeception("Failed to parse FAILED result from:" + output);
			}
		}
	}
}
