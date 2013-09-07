package com.cbt.client;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Cookie;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * Jersey Client filter for handling authentication towards CBT web service
 * 
 * @author SauliusALisauskas
 *
 */
public class ClientAuthFilter extends ClientFilter {
	//private static final Logger logger = Logger.getLogger(ClientAuthFilter.class);
	private Configuration mConfig;
	private Cookie mAuthCookie;
	private List<Object> mCookies = new ArrayList<Object>(1);
	
	@Inject
	public ClientAuthFilter(Configuration config) {
		mConfig = config;
	}
	
	@Override
	public ClientResponse handle(ClientRequest request) {
		if (null == mAuthCookie) {
			mAuthCookie = createAuthCookie(mConfig.getUserName(), mConfig.getUserPassword());
			mCookies.add(mAuthCookie);
		}
		request.getHeaders().put("Cookie", mCookies);
		return getNext().handle(request);
	}
	
	private Cookie createAuthCookie(String username, String passoword) {
		return new Cookie("auth", username + ":" + passoword);
	}
}
