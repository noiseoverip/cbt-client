package com.cbt.client;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.cbt.ws.entity.Device;
import com.cbt.ws.entity.DeviceJob;
import com.cbt.ws.entity.TestPackage;
import com.cbt.ws.jooq.enums.DeviceState;
import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientHandlerException;

/**
 * Runnable responsible for updating particular device state, fetching an executing jobs
 * 
 * @author SauliusAlisauskas 2013-03-22 Initial version
 * 
 */
public class DeviceWorker implements Runnable {
	private AdbApi mAdbApi;
	private CbtWsClientApi mWsApi;
	private static final Logger mLog = Logger.getLogger(DeviceWorker.class);
	private Device mDevice;
	private Callback mCallback;
	private ApplicationInstaller mInstaller;
	private TestExecutor mTestExecutor;

	public interface Callback {
		void onDeviceOffline(Device device);
	}

	@Inject
	public DeviceWorker(AdbApi adbApi, CbtWsClientApi wsApi, ApplicationInstaller installer, TestExecutor testExecutor) {
		mAdbApi = adbApi;
		mWsApi = wsApi;
		mInstaller = installer;
		mTestExecutor = testExecutor;
	}

	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	public void setDevice(Device device) {
		mDevice = device;
	}

	public void sendDeviceUpdate() {
		try {
			mWsApi.updatedevice(mDevice);
		} catch (CbtWsClientException e) {
			mLog.error("Could not update device:" + mDevice);
		} catch (ClientHandlerException connectionException) {
			mLog.error("Connection problem", connectionException);
		}
	}

	@Override
	public void run() {
		mLog.info("Checking device state, " + mDevice);
		// Check device status
		List<String> deviceNames = null;
		try {
			deviceNames = mAdbApi.getDevices();
		} catch (Exception e) {
			mLog.error("Could not find any device attached");
		} finally {
			if (null != deviceNames && deviceNames.indexOf(mDevice.getSerialNumber()) > -1) {
				mDevice.setState(DeviceState.ONLINE);
			} else {
				mLog.warn("Device wen offline, " + mDevice);
				mDevice.setState(DeviceState.OFFLINE);
				if (null != mCallback) {
					mCallback.onDeviceOffline(mDevice);
				}
			}
			sendDeviceUpdate();
		}

		// Check for available jobs
		if (mDevice.getState().equals(DeviceState.ONLINE)) {
			mLog.info("Checking jobs for " + mDevice);
			DeviceJob job = mWsApi.getWaitingJob(mDevice);
			if (null != job) {
				mLog.info("Found job " + job);

				TestPackage testPackage = null;
				try {
					testPackage = fetchTestPackage(job);
				} catch (CbtWsClientException | IOException e) {
					mLog.error("Error while checking out files", e);
					return;
				}

				mInstaller.setTestPackage(testPackage);

				// Install target application
				mLog.info("Trying to install application on to device");
				try {
					mInstaller.installApp(mDevice.getSerialNumber());
				} catch (Exception e) {
					mLog.error("Could not install application on to device", e);
					return;
				}

				// Install test
				mLog.info("Trying to install test JAR file");
				try {
					mInstaller.installTest(mDevice.getSerialNumber());
				} catch (Exception e) {
					mLog.error("Could not install test script on to device", e);
					return;
				}

				mTestExecutor.setTestPackage(testPackage);
				mTestExecutor.setDeviceSerial(mDevice.getSerialNumber());
				try {
					mTestExecutor.execute();
				} catch (Exception e) {
					mLog.error("Could not execute test on to device", e);
					return;
				}
			} else {
				mLog.info("No jobs found");
			}
		}

	}

	private TestPackage fetchTestPackage(DeviceJob job) throws CbtWsClientException, IOException {
		TestPackage testPackage = mWsApi.checkoutTestPackage(job.getId());
		return testPackage;
	}
}
