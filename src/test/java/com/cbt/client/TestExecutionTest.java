package com.cbt.client;

import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.cbt.core.exceptions.CbtTestResultParseExeception;
import com.cbt.ws.entity.DeviceJobResult;
import com.cbt.ws.jooq.enums.DeviceJobResultState;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * UT for {@link TestExecutor}
 * 
 * @author SauliusAlisauskas 2013-03-22
 * 
 */
public class TestExecutionTest {

	// private final Logger mLogger = Logger.getLogger(TestExecutionTest.class);

	/**
	 * Test parsing of result with failure
	 * 
	 * @throws IOException
	 * @throws CbtTestResultParseExeception
	 */
	@Test
	public void testParsetOutputFailed() throws IOException, CbtTestResultParseExeception {
		DeviceJobResult testResult = new DeviceJobResult();
		String output = Resources.toString(Resources.getResource("adbOutputTestFailed.txt"), Charsets.UTF_8);
		Configuration config = mock(Configuration.class);
		TestExecutor executor = new TestExecutor(null, config);
		executor.parseTestOutput(output, testResult);
		Assert.assertEquals((Integer) 1, testResult.getTestsRun());
		Assert.assertEquals((Integer) 3, testResult.getTestsFailed());
		Assert.assertEquals((Integer) 100, testResult.getTestsErrors());
		Assert.assertEquals(DeviceJobResultState.FAILED, testResult.getState());

	}

	/**
	 * Test parsing of successful results from running single test
	 * 
	 * @throws IOException
	 * @throws CbtTestResultParseExeception
	 */
	@Test
	public void testParseOutputSuccess() throws IOException, CbtTestResultParseExeception {
		testParseOutputSuccess(Resources.toString(Resources.getResource("adbOutputTestSuccess.txt"), Charsets.UTF_8), 1);
	}

	/**
	 * Test parsing of successful results from running multiple tests
	 * 
	 * @throws IOException
	 * @throws CbtTestResultParseExeception
	 */
	@Test
	public void testParseOutputSuccessMultiple() throws IOException, CbtTestResultParseExeception {
		final int numberOfTests = 4;
		testParseOutputSuccess(
				Resources.toString(Resources.getResource("adbOutputTestSuccessMultipleTests.txt"), Charsets.UTF_8),
				numberOfTests);
	}

	/**
	 * Helper method for testing parsing of successful test result and variable number of test runs
	 * 
	 * @param testOutput
	 * @param testsRun
	 * @throws CbtTestResultParseExeception
	 */
	private void testParseOutputSuccess(String testOutput, int testsRun) throws CbtTestResultParseExeception {
		Configuration config = mock(Configuration.class);
		DeviceJobResult testResult = new DeviceJobResult();
		TestExecutor executor = new TestExecutor(null, config);
		executor.parseTestOutput(testOutput, testResult);
		Assert.assertEquals((Integer) testsRun, testResult.getTestsRun());
		Assert.assertEquals(DeviceJobResultState.PASSED, testResult.getState());
	}
}
