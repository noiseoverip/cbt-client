package com.cbt.client;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

public class MainModule extends AbstractModule {

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
