package com.cbt.client;

import com.cbt.client.annotations.CbtWsURI;
import com.cbt.client.annotations.PathAndroidToolAdb;
import com.cbt.client.annotations.WorkspacePath;
import com.cbt.client.annotations.UserId;
import com.google.inject.AbstractModule;

/**
 * Guice for module binding properties
 * 
 * @author SauliusAlisauskas 2013-03-18 Initial version
 *
 */
public class GModuleProperties extends AbstractModule {

	@Override
	protected void configure() {
		//bind(String.class).annotatedWith(PathAndroidToolAdb.class).toInstance("C:\\Dev\\Tools\\android-sdk\\platform-tools\\adb");
		bind(String.class).annotatedWith(PathAndroidToolAdb.class).toInstance("/home/saulius/Documents/dev/adt-bundle-linux-x86_64-20130219/sdk/platform-tools/adb");
		bind(String.class).annotatedWith(WorkspacePath.class).toInstance("/home/saulius/Documents/cbt/client_workspace/");
		bind(Long.class).annotatedWith(UserId.class).toInstance(1L);
		bind(String.class).annotatedWith(CbtWsURI.class).toInstance("http://127.0.0.1:8081");		
	}
}
