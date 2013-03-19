package com.cbt.client;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

/**
 * Main guice module
 * 
 * @author SauliusAlisauskas 2013-03-18 Initial version
 *
 */
public class GModuleMain extends AbstractModule {

	@Override
	protected void configure() {
		bind(CbtClient.class);
		bind(ApplicationInstaller.class);
		bind(CliExecutor.class);
		bind(TestExecutor.class);
		bind(AdbApi.class);
		bind(CbtWsClientApi.class);
		bind(Store.class).in(Singleton.class);
	}	
}
