package com.cbt.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.mockito.Mockito.*;

import com.cbt.ws.entity.DeviceJobResult;
import com.cbt.ws.exceptions.CbtTestResultParseExeception;

/**
 * Standalone test for {@link TestExecutor}
 * 
 * @author SauliusAlisauskas
 *
 */
public class TestExecutionTest {
	
	private final Logger mLogger = Logger.getLogger(TestExecutionTest.class);
	
	/**
	 * Test parsing of result with failure
	 * 
	 * @throws IOException
	 * @throws CbtTestResultParseExeception
	 */
	@Test 
	public void testParsetOutputFailed() throws IOException, CbtTestResultParseExeception {
		DeviceJobResult testResult = new DeviceJobResult();
		String output = getDummyOutput("testOutput1.txt");
		Configuration config = mock(Configuration.class);
		TestExecutor executor = new TestExecutor(null, config);
		executor.parseTestOutput(output, testResult);
		Assert.assertEquals((Integer)1, testResult.getTestsRun());
		Assert.assertEquals((Integer)3, testResult.getTestsFailed());
		Assert.assertEquals((Integer)100, testResult.getTestsErrors());
		Assert.assertEquals(DeviceJobResult.State.FAILED, testResult.getState());
		
	}
	
	/**
	 * Test parsing of successful results from running single test
	 * 
	 * @throws IOException
	 * @throws CbtTestResultParseExeception
	 */
	@Test
	public void testParseOutputSuccess() throws IOException, CbtTestResultParseExeception {
		testParseOutputSuccess(getDummyOutput("testOutputSuccess.txt"), 1);
	}
	
	/**
	 * Test parsing of successful results from running multiple tests
	 * 
	 * @throws IOException
	 * @throws CbtTestResultParseExeception
	 */
	@Test
	public void testParseOutputSuccessMultiple() throws IOException, CbtTestResultParseExeception {
		testParseOutputSuccess(getDummyOutput("testOutputSuccessMultipleFiles.txt"), 4);
	}
	
	private void testParseOutputSuccess(String testOutput, int testsRun) throws CbtTestResultParseExeception {
		DeviceJobResult testResult = new DeviceJobResult();
		Configuration config = mock(Configuration.class);
		TestExecutor executor = new TestExecutor(null, config);
		executor.parseTestOutput(testOutput, testResult);
		Assert.assertEquals((Integer)testsRun, testResult.getTestsRun());
		Assert.assertEquals(DeviceJobResult.State.PASSED, testResult.getState());
	}
	
	private String getDummyOutput(String fileName) throws IOException {
		String path = this.getClass().getClassLoader().getResource(fileName).getPath();
		FileInputStream stream = new FileInputStream(new File(path));
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			/* Instead of using default, pass in a decoder. */
			return Charset.defaultCharset().decode(bb).toString();
		} finally {
			stream.close();
		}
	}
}
