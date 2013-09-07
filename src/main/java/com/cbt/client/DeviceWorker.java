package com.cbt.client;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.cbt.ws.entity.Device;
import com.cbt.ws.entity.DeviceJob;
import com.cbt.ws.entity.DeviceJobResult;
import com.cbt.ws.entity.TestPackage;
import com.cbt.ws.jooq.enums.DeviceState;
import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientHandlerException;

/**
 * Class responsible for updating particular device state, fetching and executing jobs. It support one device only,
 * therefore, separate instances must be run for each device
 * 
 * @author SauliusAlisauskas 2013-03-22 Initial version
 * 
 */
public class DeviceWorker implements Runnable {
	public interface Callback {
		void onDeviceOffline(Device device);
	}

	private static final Logger mLogger = Logger.getLogger(DeviceWorker.class);
	private AdbApi mAdbApi;
	private Callback mCallback;
	private Device mDevice;
	private AndroidApplicationInstaller mInstaller;
	private TestExecutor mTestExecutor;
	private CbtWsClientApi mWsApi;

	@Inject
	public DeviceWorker(AdbApi adbApi, CbtWsClientApi wsApi, AndroidApplicationInstaller installer, TestExecutor testExecutor) {
		mAdbApi = adbApi;
		mWsApi = wsApi;
		mInstaller = installer;
		mTestExecutor = testExecutor;
	}

	/**
	 * Update device state, check for waiting jobs, execute if any found and send results
	 */
	@Override
	public void run() {
		mLogger.info("Checking device state, " + mDevice);
		// Check device status
		List<String> deviceNames = null;
		try {
			deviceNames = mAdbApi.getDevices();
		} catch (Exception e) {
			mLogger.error("Could not find any device attached");
		} finally {
			if (null != deviceNames && deviceNames.indexOf(mDevice.getSerialNumber()) > -1) {
				mDevice.setState(DeviceState.ONLINE);
			} else {
				mLogger.warn("Device wen offline, " + mDevice);
				mDevice.setState(DeviceState.OFFLINE);
				if (null != mCallback) {
					mCallback.onDeviceOffline(mDevice);
				}
			}
			sendDeviceUpdate();
		}

		// Check for available jobs
		if (mDevice.getState().equals(DeviceState.ONLINE)) {
			mLogger.info("Checking jobs for " + mDevice);
			DeviceJob job = mWsApi.getWaitingJob(mDevice);
			if (null != job) {
				mLogger.info("Found job " + job);

				TestPackage testPackage = fetchTestPackage(job);
				mInstaller.setTestPackage(testPackage);

				// Install target application
				mLogger.info("Trying to install application on to device");
				try {
					mInstaller.installApp(mDevice.getSerialNumber());
				} catch (Exception e) {
					exitJobRun("Could not install application on to device", e);
				}

				// Install test
				mLogger.info("Trying to install test JAR file");
				try {
					mInstaller.installTestScript(mDevice.getSerialNumber());
				} catch (Exception e) {
					exitJobRun("Could not install test script on to device", e);
				}

				// Set information needed for test execution
				DeviceJobResult result = null;
				mLogger.info("Executing devicejob:" + job + " on:" + mDevice.getSerialNumber() + " with:" + testPackage);
				try {
					result = mTestExecutor.execute(job, mDevice.getSerialNumber(), testPackage);
				} catch (Exception e) {
					exitJobRun("Could not execute test on to device", e);
				}

				mLogger.info("Publishing results:" + result);
				publishTestResult(result);

			} else {
				mLogger.info("No jobs found");
			}
		}

	}

	/**
	 * Set callback implementation
	 * 
	 * @param callback
	 */
	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	/**
	 * Set device
	 * 
	 * @param device
	 */
	public void setDevice(Device device) {
		mDevice = device;
	}

	/**
	 * Handle abnormal exit of device job execution
	 * 
	 * @param message
	 */
	private void exitJobRun(String message, Throwable e) {
		mLogger.error(message, e);
		// TODO: send result of abnormal exit to server
		throw new RuntimeException(message);
	}

	/**
	 * Retrieve test package for specified device job
	 * 
	 * @see {@link CbtWsClientApi#checkoutTestPackage(Long)}
	 * 
	 * @param job
	 * @return
	 * @throws CbtWsClientException
	 * @throws IOException
	 */
	private TestPackage fetchTestPackage(DeviceJob job) {
		TestPackage testPackage = null;
		try {
			testPackage = mWsApi.checkoutTestPackage(job.getId());
		} catch (CbtWsClientException | IOException e) {
			exitJobRun("Error while checking out files", e);
		}
		return testPackage;
	}

	/**
	 * Helper method for handling publishing of test results
	 * 
	 * @param result
	 */
	private void publishTestResult(DeviceJobResult result) {
		try {
			mWsApi.publishDeviceJobResult(result);
		} catch (CbtWsClientException e) {
			exitJobRun("Could not publish job result", e);
		}
	}

	/**
	 * Helper method for sending device state update
	 */
	private void sendDeviceUpdate() {
		try {
			mWsApi.updatedevice(mDevice);
		} catch (CbtWsClientException e) {
			mLogger.error("Could not update device:" + mDevice);
		} catch (ClientHandlerException connectionException) {
			mLogger.error("Connection problem", connectionException);
		}
	}
}
