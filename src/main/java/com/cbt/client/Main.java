package com.cbt.client;

import com.beust.jcommander.IDefaultProvider;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.cbt.client.configuration.Configuration;
import com.cbt.client.guice.GuiceModuleConfiguration;
import com.cbt.client.guice.GuiceModuleMain;
import com.cbt.client.util.SupervisorFactory;
import com.cbt.client.util.Utils;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main CBT client class
 *
 * @author SauliusAlisauskas 2013-03-22 Initial version
 * @author iljabobkevic 2013-10-02 Run client in a separate, supervised thread, use jcommander for argument parsing
 */
public class Main {

   private static final int CLIENT_THREAD_POOL_SIZE = 1;
   private static Logger logger = Logger.getLogger(Main.class);

   public static void main(String... args) {
      logger.info("Starting application...");

      // Initialize guice injector
      Injector injector = Guice.createInjector(new GuiceModuleMain(), new GuiceModuleConfiguration());

      // Prepare for argument parsing
      Configuration parsedArgs = injector.getInstance(Configuration.class);
      JCommander jc = new JCommander(parsedArgs);
      jc.setDefaultProvider(injector.getInstance(IDefaultProvider.class));

      logger.info("Parsing arguments...");
      try {
         jc.parse(args);
      } catch (ParameterException e) {
         StringBuilder out = new StringBuilder(e.getLocalizedMessage()).append("\n\n");
         jc.usage(out);
         logger.error(out.toString());
         System.exit(1);
         return;
      }
      if (parsedArgs.isHelp()) {
         jc.usage();
         return;
      }

      // Initialize adb
      Utils.initAdb(parsedArgs.getSdk());

      // Start the client thread with supervision
      ExecutorService pool = Executors.newFixedThreadPool(CLIENT_THREAD_POOL_SIZE);

      // Supervision is done on the main thread, so it's blocking call
      SupervisorFactory.supervise(pool.submit(injector.getInstance(CbtClient.class)), true);

      // Terminate adb and stop
      Utils.terminateAdb();
      logger.info("Application finished");
   }
}