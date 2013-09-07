package com.cbt.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
public class GuiceModuleProperties extends AbstractModule {

	private final Logger mLogger = Logger.getLogger(GuiceModuleProperties.class);
	private static final String configFilePath = "/client.properties";
	
	@Override
	protected void configure() {
		Properties properties = new Properties();
		try {
			// Try local file
			InputStream input = Configuration.class.getResourceAsStream(configFilePath);
			if (null == input) {
				// Try absolute path
				mLogger.info("Trying:" + new File(configFilePath).getAbsolutePath());
				input = new FileInputStream(new File(configFilePath).getAbsolutePath());
			}
			if (null != input) {
				properties.load(input);
			}
			Names.bindProperties(binder(), properties);
		} catch (FileNotFoundException e) {
			mLogger.error("The configuration file Test.properties can not be found", e);
		} catch (IOException e) {
			mLogger.error("I/O Exception during loading configuration", e);
		}
		bind(Configuration.class).in(Singleton.class);
	}
}
