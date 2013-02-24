package com.cbg.guicemodules;

import com.cbt.annotations.PathAndroidToolAdb;
import com.google.inject.AbstractModule;

public class PropertiesModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(String.class).annotatedWith(PathAndroidToolAdb.class).toInstance("C:\\Dev\\Tools\\android-sdk\\platform-tools\\adb");
	}

	

}
