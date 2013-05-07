package com.cbt.client;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.inject.Singleton;

import org.apache.log4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Guice for module binding properties
 * 
 * @author SauliusAlisauskas 2013-03-18 Initial version
 * 
 */
public class GModuleProperties extends AbstractModule {
	
	private final Logger mLogger = Logger.getLogger(GModuleProperties.class);
	
	@Override
	protected void configure() {
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(this.getClass().getResource("/client.properties").getPath()));
			Names.bindProperties(binder(), properties);
		} catch (FileNotFoundException e) {
			mLogger.error("The configuration file Test.properties can not be found", e);
		} catch (IOException e) {
			mLogger.error("I/O Exception during loading configuration", e);
		}		
		bind(Configuration.class).in(Singleton.class);
	}
}
