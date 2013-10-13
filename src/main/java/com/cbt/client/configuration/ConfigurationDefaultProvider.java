package com.cbt.client.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import com.beust.jcommander.IDefaultProvider;
import com.google.inject.Inject;

/**
 * Class ConfigurationDefaultProvider used for combining guice named binding injections with jcommander's
 * default value provider implementation.
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public class ConfigurationDefaultProvider implements IDefaultProvider {

   private static final int EXPECTED_CONFIG_COUNT = 5;
   private Map<String, String> config = new HashMap<String, String>(EXPECTED_CONFIG_COUNT);

   @Inject(optional = true)
   public void setWorkspace(@Named("path_workspace") String workspace) {
      config.put("--workspace", workspace);
   }

   @Inject(optional = true)
   public void setServer(@Named("uri_server") String server) {
      config.put("--server", server);
   }

   @Inject(optional = true)
   public void setUsername(@Named("username") String username) {
      config.put("--username", username);
   }

   @Inject(optional = true)
   public void setPassword(@Named("password") String password) {
      config.put("--password", password);
   }

   @Inject(optional = true)
   public void setDebug(@Named("debug_rest") String debug) {
      config.put("--debug", debug);
   }

   @Inject(optional = true)
   public void setSdk(@Named("path_sdk") String sdk) {
      config.put("--sdk", sdk);
   }

   @Override
   public String getDefaultValueFor(String s) {
      return config.get(s);
   }
}
