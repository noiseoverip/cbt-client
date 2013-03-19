package com.cbt.clientws;

import java.util.List;

import org.apache.log4j.Logger;

import com.cbt.client.Store;
import com.cbt.client.adb.AdbApi;
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
	
	private Store mStore;
	private AdbApi mAdbApi;
	private CbtWsClientApi mWsApi;
	private static final Logger mLog = Logger.getLogger(StatusUpdater.class);
	
	@Inject
	public StatusUpdater(AdbApi adbApi, Store store, CbtWsClientApi wsApi) {
		mStore = store;
		mAdbApi = adbApi;
		mWsApi = wsApi;
	}
	
	@Override
	public void run() {		
		List<Device> devicesInMemory = mStore.getDevices();
		List<String> deviceNames = null;
		try {
			deviceNames = mAdbApi.getDevices();
		} catch (Exception e) {
			mLog.error("Could not read from ADB");
			return;
		}
		for (Device device : devicesInMemory) {
			if (deviceNames.contains(device.getSerialNumber())) {
				device.setState(DeviceState.ONLINE);
			} else {
				device.setState(DeviceState.OFFLINE);
			}
			try {
				mWsApi.updatedevice(device);
			} catch (CbtClientException e) {
				mLog.error("Could not update device:" + device);
			}			
		}
	}

}
