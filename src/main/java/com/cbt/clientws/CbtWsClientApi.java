package com.cbt.clientws;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.cbt.annotations.CbtWsURI;
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
	
	public Long registerDevice(Device device) {
		WebResource webResource = getClient().resource(mWsUrl);
		ClientResponse response = webResource.path("device").type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.TEXT_HTML).put(ClientResponse.class, device);
		mLogger.debug(response);
		assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
		return Long.valueOf(response.getEntity(String.class));
	}
	
	private Client getClient() {
		if (null == mClient) {
			mClient = Client.create();
		}
		return mClient;
	}
}
