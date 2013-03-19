package com.cbt.client;

import java.util.List;

import org.apache.log4j.Logger;

import com.cbt.client.annotations.UserId;
import com.cbt.ws.entity.Device;
import com.cbt.ws.jooq.enums.DeviceState;
import com.google.inject.Inject;

/**
 * Runnable responsible for updating device status
 * 
 * @author SauliusAlisauskas 2013-03-08 Initial version
 * 
 */
public class StatusUpdater implements Runnable {

	private Long mUserId;
	private Store mStore;
	private AdbApi mAdbApi;
	private CbtWsClientApi mWsApi;
	private static final Logger mLog = Logger.getLogger(StatusUpdater.class);

	@Inject
	public StatusUpdater(@UserId Long userId, AdbApi adbApi, Store store, CbtWsClientApi wsApi) {
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
	 */
	private void registerDevices(List<String> deviceNames) {
		for (String deviceName : deviceNames) {
			mLog.info("Registering device:" + deviceName);
			Device device = new Device();
			device.setUserId(mUserId);
			device.setSerialNumber(deviceName);
			device.setState(DeviceState.ONLINE);
			getDeviceProperties(device);
			Long deviceId = null;
			try {
				deviceId = mWsApi.registerDevice(device);
			} catch (CbtWsClientException e) {
				mLog.error("Could not register device:" + device, e);
			}		
			
			if (null != deviceId && deviceId > 0) {
				mLog.info("Device registered:" + device);
				device.setId(deviceId);
				mStore.addDevice(device);
			} else {
				mLog.info("Failed to register device:" + device + " id:" + deviceId);
			}
		}
	}

	@Override
	public void run() {
		List<String> deviceNames = null;
		try {
			deviceNames = mAdbApi.getDevices();
		} catch (Exception e) {
			mLog.error("Could not find any device attached");
			// TODO: should update all status to OFFLINE
			return;
		}

		List<Device> devicesInMemory = mStore.getDevices();
		for (Device device : devicesInMemory) {
			if (deviceNames.contains(device.getSerialNumber())) {
				if (device.getState().equals(DeviceState.OFFLINE)) {
					mLog.info("Device:" + device + " went ONLINE");
				}
				device.setState(DeviceState.ONLINE);				
				deviceNames.remove(deviceNames.indexOf(device.getSerialNumber()));
			} else {
				if (device.getState().equals(DeviceState.ONLINE)) {
					mLog.info("Device:" + device + " went OFFLINE");
				}
				device.setState(DeviceState.OFFLINE);
			}
			try {
				mWsApi.updatedevice(device);
			} catch (CbtWsClientException e) {
				mLog.error("Could not update device:" + device);
			}
		}

		if (deviceNames.size() > 0) {
			registerDevices(deviceNames);
		}		
	}
}
