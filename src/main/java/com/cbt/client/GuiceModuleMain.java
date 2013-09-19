package com.cbt.client;

import com.google.inject.AbstractModule;

import javax.inject.Singleton;

/**
 * Main guice module
 *
 * @author SauliusAlisauskas 2013-03-18 Initial version
 */
public class GuiceModuleMain extends AbstractModule {

   @Override
   protected void configure() {
      bind(CbtClient.class);
      bind(CliExecutor.class);
      bind(AdbApi.class);
      bind(CbtWsClientApi.class);
      bind(Store.class).in(Singleton.class);
      bind(ClientAuthFilter.class).in(Singleton.class);
   }
}
