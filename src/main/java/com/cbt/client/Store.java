package com.cbt.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.cbt.ws.entity.Device;

/**
 * Dynamic storage of available devices
 * 
 * @author SauliusAlisauskas 2013-03-22 Initial version
 *
 */
public class Store {
	private ConcurrentHashMap<String, Device> mDevices = new ConcurrentHashMap<String, Device>(5);

	/**
	 * Register new device
	 * 
	 * @param device
	 */
	public void addDevice(Device device) {
		mDevices.put(device.getSerialNumber(), device);
	}

	public boolean contains(String deviceSerial) {
		return mDevices.containsKey(deviceSerial);
	}

	/**
	 * Return device id based on it's serial number if it is defined in file "devices"
	 * 
	 * @param deviceSerialNumber
	 * @return
	 * @deprecated
	 */
	public Long getDeviceId(String deviceSerialNumber) {
		if (null == deviceSerialNumber) {
			return null;
		}
		Properties prop = new Properties();
		InputStream in = getClass().getResourceAsStream("/devices");
		try {
			prop.load(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		String deviceId = prop.getProperty(deviceSerialNumber);
		return (null != deviceId) ? Long.valueOf(deviceId) : null;
	}

	/**
	 * Get all registered devices
	 * 
	 * @return
	 */
	public ConcurrentHashMap<String, Device> getDevices() {
		return mDevices;
	}

	public void remove(Device device) {
		mDevices.remove(device.getSerialNumber());
	}
}
