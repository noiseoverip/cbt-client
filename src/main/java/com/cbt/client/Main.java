package com.cbt.client;

import org.apache.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {
	
	private static final Logger mLog = Logger.getLogger(Main.class);
	
	public static void main(String[] args) {
		
		mLog.info("Starting application");
		
		Injector injector = Guice.createInjector(new GModuleMain(), new GModuleProperties());
	    CbtClient client = injector.getInstance(CbtClient.class);
	    client.start();
	    
	    mLog.info("Application finished");
	}

}
