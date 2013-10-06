package com.cbt.client.guice;

import com.beust.jcommander.IDefaultProvider;
import com.cbt.client.configuration.Configuration;
import com.cbt.client.configuration.ConfigurationDefaultProvider;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;

import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Class GuiceModuleConfiguration
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public class GuiceModuleConfiguration extends AbstractModule {

   private static final String DEFAULT_CONFIG_PATH = "client.properties";
   private final Logger logger = Logger.getLogger(GuiceModuleConfiguration.class);

   @Override
   protected void configure() {
      Properties properties = new Properties();
      try {
         // Try class loader (file should be in the class path)
         InputStream input = Configuration.class.getResourceAsStream("/" + DEFAULT_CONFIG_PATH);
         if (null == input) {
            // Try file input stream (file should be in the *current* directory)
            logger.trace("Trying: " + new File(DEFAULT_CONFIG_PATH));
            input = new FileInputStream(new File(DEFAULT_CONFIG_PATH));
         }
         if (null != input) {
            properties.load(input);
         }
         Names.bindProperties(binder(), properties);
      } catch (FileNotFoundException e) {
         logger.trace("The configuration file Test.properties can not be found", e);
      } catch (IOException e) {
         logger.error("I/O Exception during loading configuration", e);
      }
      bind(IDefaultProvider.class).to(ConfigurationDefaultProvider.class).in(Singleton.class);
   }
}
