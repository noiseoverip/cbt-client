package com.cbt.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cbt.client.annotations.UserId;
import com.cbt.ws.entity.Device;
import com.google.inject.Injector;

/**
 * Main CBT client class
 * 
 * @author SauliusAlisauskas 2013-03-19 Initial version
 * 
 */
public class CbtClient implements AdbMonitor.Callback, DeviceWorker.Callback {
	private AdbMonitor mAdbMonitor;
	private ScheduledExecutorService mDeviceMonitorExecutor;
	private Map<String, ScheduledFuture<?>> mDeviceWorkers = new HashMap<String, ScheduledFuture<?>>();
	private Injector mInjector;
	private final Logger mLog = Logger.getLogger(ApplicationInstaller.class);
	private Store mStore;
	private Long mUserId;

	@Inject
	public CbtClient(AdbMonitor statusUpdater, @UserId Long userId, Store store, Injector injector) {		
		mUserId = userId;
		mStore = store;
		mAdbMonitor = statusUpdater;
		mInjector = injector;
	}

	private synchronized void addDeviceWorkerFuture(Device device, ScheduledFuture<?> future) {
		mDeviceWorkers.put(device.getSerialNumber(), future);
	}

	private synchronized ScheduledFuture<?> getDeviceWorkerFuture(Device device) {
		return mDeviceWorkers.get(device.getSerialNumber());
	}

	@Override
	public void onDeviceOffline(Device device) {
		mLog.info("Removing " + device);
		ScheduledFuture<?> future = getDeviceWorkerFuture(device);
		future.cancel(true);
		mStore.remove(device);
		
	}
	
	@Override
	public void onNewDeviceFound(Device device) {
		mLog.info("Adding device worker " + device);
		device.setUserId(mUserId);
		mStore.addDevice(device);
		DeviceWorker worker = mInjector.getInstance(DeviceWorker.class);
		worker.setCallback(this);
		worker.setDevice(device);
		worker.setCallback(this);
		ScheduledFuture<?> future = mDeviceMonitorExecutor.scheduleAtFixedRate(worker, 1, 3, TimeUnit.SECONDS);
		addDeviceWorkerFuture(device, future);

	}
	
	public void start() {
		
		mAdbMonitor.setCallback(this);
		mDeviceMonitorExecutor = Executors.newScheduledThreadPool(4);
		mDeviceMonitorExecutor.scheduleAtFixedRate(mAdbMonitor, 1, 20, TimeUnit.SECONDS);

		try {
			TimeUnit.SECONDS.sleep(9000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

}
