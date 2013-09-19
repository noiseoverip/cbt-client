package com.cbt.client;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.Logger;

/**
 * Main CBT client class
 *
 * @author SauliusAlisauskas 2013-03-22 Initial version
 */
public class Main {

   private static final Logger mLog = Logger.getLogger(Main.class);

   public static void main(String[] args) {

      mLog.info("Starting application");

      Injector injector = Guice.createInjector(new GuiceModuleMain(), new GuiceModuleProperties());
      CbtClient client = injector.getInstance(CbtClient.class);
      client.start();

      mLog.info("Application finished");
   }

}
