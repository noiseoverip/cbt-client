package com.cbt.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.cbt.ws.entity.Device;

public class Store {
	private Vector<Device> mDevices = new Vector<Device>(5);
	
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
	public List<Device> getDevices() {
		return mDevices;
	}
	
	/**
	 * Register new device
	 * 
	 * @param device
	 */
	public void addDevice(Device device) {		
		mDevices.add(device);
	}
	
}
