package com.cbt.client;

import java.util.List;

import org.apache.log4j.Logger;

import com.cbt.client.annotations.UserId;
import com.cbt.ws.entity.Device;
import com.cbt.ws.jooq.enums.DeviceState;
import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientHandlerException;

//TODO: should periodically check for new devices, register them, put into Store, spawn DeviceWorker for that device, deviceWorker would be responsible for updating status
/**
 * Runnable responsible for updating device status
 * 
 * @author SauliusAlisauskas 2013-03-08 Initial version
 * 
 */
public class AdbMonitor implements Runnable {

	public interface Callback {
		void onNewDeviceFound(Device device);
	}

	private static final Logger mLog = Logger.getLogger(AdbMonitor.class);
	private AdbApi mAdbApi;
	private Callback mCallback;
	private Store mStore;
	private Long mUserId;

	private CbtWsClientApi mWsApi;

	@Inject
	public AdbMonitor(@UserId Long userId, AdbApi adbApi, Store store, CbtWsClientApi wsApi) {
		mStore = store;
		mAdbApi = adbApi;
		mWsApi = wsApi;
		mUserId = userId;
	}

	/**
	 * Run necessary test on device to determine it's properties
	 * 
	 * @param device
	 */
	private void getDeviceProperties(Device device) {
		mLog.info("Scaning device:" + device);
		// TODO: run test on device to determine it's properties
		device.setDeviceTypeId(1L);
		device.setDeviceOsId(1L);
	}

	/**
	 * Register devices with specified serial numbers and store them in Store
	 * 
	 * @param deviceNames
	 * @throws CbtWsClientException
	 * @throws ClientHandlerException
	 */
	private Device registerDevice(String deviceName) throws ClientHandlerException, CbtWsClientException {
		mLog.info("Registering device:" + deviceName);
		Device device = new Device();
		device.setUserId(mUserId);
		device.setSerialNumber(deviceName);
		device.setState(DeviceState.ONLINE);
		getDeviceProperties(device);
		Long deviceId = mWsApi.registerDevice(device);
		device.setId(deviceId);
		return device;

	}

	@Override
	public void run() {
		List<String> deviceNames = null;
		try {
			deviceNames = mAdbApi.getDevices();
		} catch (Exception e) {
		} finally {
			if (null == deviceNames) {
				mLog.info("Could not find any device attached");
				return;
			}
		}

		for (String deviceSerial : deviceNames) {
			if (!mStore.contains(deviceSerial)) {
				try {
					Device newDevice = registerDevice(deviceSerial);
					if (null != mCallback) {
						mCallback.onNewDeviceFound(newDevice);
					} else {
						mLog.warn("Callback not set");
					}
				} catch (CbtWsClientException e) {
					mLog.error("Could not register device:" + deviceSerial, e);
				} catch (ClientHandlerException connectionException) {
					mLog.error("Connection problem", connectionException);
				}
			}
		}
	}

	public void setCallback(Callback callback) {
		mCallback = callback;
	}
}
