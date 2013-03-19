package com.cbt.client;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.cbt.client.annotations.CbtWsURI;
import com.cbt.ws.entity.Device;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Cbt webservcie client api
 * 
 * @author SauliusAlisauskas 2013-03-12 Initial version
 *
 */
public class CbtWsClientApi {
	private Client mClient;
	private String mWsUrl;
	private Logger mLogger = Logger.getLogger(CbtWsClientApi.class);
	
	@Inject
	public CbtWsClientApi(@CbtWsURI String wsUrl) {
		mWsUrl = wsUrl;
	}
	
	/**
	 * Register new device
	 * 
	 * @param device
	 * @return
	 * @throws CbtWsClientException
	 */
	public Long registerDevice(Device device) throws CbtWsClientException {
		WebResource webResource = getClient().resource(mWsUrl);
		ClientResponse response = webResource.path("device").type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.TEXT_HTML).put(ClientResponse.class, device);
		mLogger.debug(response);
		if (response.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
			throw new CbtWsClientException("Failed to register new device");
		}
		return Long.valueOf(response.getEntity(String.class));
	}
	
	/**
	 * Update device
	 * 
	 * @param device
	 * @throws CbtWsClientException
	 */
	public void updatedevice(Device device) throws CbtWsClientException {
		WebResource webResource = getClient().resource(mWsUrl);
		ClientResponse response = webResource.path("device/" + device.getId()).type(MediaType.APPLICATION_JSON_TYPE)
				.post(ClientResponse.class, device);		
		if (response.getStatus() != ClientResponse.Status.NO_CONTENT.getStatusCode()) {
			throw new CbtWsClientException("Failed to update device, response:" + response);
		}
	}
	
	private Client getClient() {
		if (null == mClient) {
			mClient = Client.create();
		}
		return mClient;
	}
}
