package com.cbt.client;

import javax.inject.Named;

import com.cbt.ws.utils.Utils;
import com.google.inject.Inject;

public class Configuration {
	private String pathAndroidADB;
	private String pathWorkspace;
	private String uriCbtWebservice;
	private Long userId;
	private String userName;
	private String userPassword;
	private boolean traceRestClient;

	public boolean isTraceRestClient() {
		return traceRestClient;
	}

	public void setTraceRestClient(@Named("debug_rest")  boolean traceRestClient) {
		this.traceRestClient = traceRestClient;
	}

	public String getPathAndroidADB() {
		return pathAndroidADB;
	}

	public String getPathWorkspace() {
		return pathWorkspace;
	}

	public String getUriCbtWebservice() {
		return uriCbtWebservice;
	}

	public Long getUserId() {
		return userId;
	}

	public String getUserName() {
		return userName;
	}

	public String getUserPassword() {
		return userPassword;
	}

	@Inject
	public void setPathAndroidADB(@Named("path_adb") String pathAndroidADB) {
		this.pathAndroidADB = pathAndroidADB;
	}

	@Inject
	public void setPathWorkspace(@Named("path_workspace") String pathWorkspace) {
		this.pathWorkspace = pathWorkspace;
	}

	@Inject
	public void setUriCbtWebservice(@Named("uri_server") String uriCbtWebservice) {
		this.uriCbtWebservice = uriCbtWebservice;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Inject
	public void setUserName(@Named("username") String userName) {
		this.userName = userName;
	}

	@Inject
	public void setUserPassword(@Named("password") String userPassword) {
		this.userPassword = Utils.Md5(userPassword);
	}
}
