package com.cbt.client.guice;

import com.cbt.client.CbtClient;
import com.cbt.client.device.DeviceMonitor;
import com.cbt.client.device.DeviceWorker;
import com.cbt.client.util.Utils;
import com.cbt.client.ws.ClientAuthFilter;
import com.cbt.client.ws.WsClient;
import com.google.inject.AbstractModule;

import javax.inject.Singleton;

/**
 * Main guice module
 *
 * @author SauliusAlisauskas 2013-03-18 Initial version
 * @author iljabobkevic 2013-10-02 update according to redesigned classes
 */
public class GuiceModuleMain extends AbstractModule {

   @Override
   protected void configure() {
      bind(CbtClient.class);
      bind(DeviceMonitor.class);
      bind(DeviceWorker.class);
      bind(WsClient.class);
      bind(ClientAuthFilter.class).in(Singleton.class);
      bind(Utils.class).in(Singleton.class);
   }
}
