package com.cbt.client.util;

import org.apache.log4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * <p>
 * Class SupervisorFactory provides easy API for scheduled {@link java.util.concurrent.Future} supervision. {@link
 * java.util.concurrent.Future#get()} is mainly used for registering possible exceptions which occurred during thread
 * execution.
 * </p>
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public class SupervisorFactory {
   /**
    * Supervisor implementation for non blocking supervision
    */
   private static class Supervisor implements Runnable {

      private Future<?> future;

      /**
       * Constructor for setting the future which should be supervised
       *
       * @param future
       */
      public Supervisor(Future<?> future) {
         this.future = future;
      }

      /**
       * Call {@link java.util.concurrent.Future#get()} and catch any occurring exception
       */
      @Override
      public void run() {
         logger.debug("Supervising " + future);
         try {
            future.get();
         } catch (InterruptedException e) {
            // TODO: Implement bug reporting
            e.printStackTrace();
         } catch (ExecutionException e) {
            e.printStackTrace();
         }
         logger.debug(future + " supervision ended!");
      }
   }

   private static Logger logger = Logger.getLogger(SupervisorFactory.class);
   /**
    * Executor services used for starting supervision threads
    */
   private static ExecutorService executor = Executors.newCachedThreadPool();

   /**
    * No instances allowed
    */
   private SupervisorFactory() {
   }

   /**
    * Non-blocking supervision. Each {@link java.util.concurrent.Future} will result additional supervision thread.
    *
    * @param future
    */
   public static final void supervise(Future<?> future) {
      supervise(future, false);
   }

   /**
    * Generic supervision call
    *
    * @param future
    * @param blocking - if false supervision will be done on the same thread
    */
   public static final void supervise(Future<?> future, boolean blocking) {
      Supervisor supervisor = new Supervisor(future);
      if (blocking) {
         supervisor.run();
      } else {
         executor.submit(supervisor);
      }

   }
}