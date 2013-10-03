package com.cbt.client.configuration;

import java.io.File;

/**
 * Interface Configuration
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public interface Configuration {

   /**
    * Get client's workspace path
    *
    * @return workspace path
    */
   File getWorkspace();

   /**
    * Get android sdk path
    *
    * @return andoird sdk path
    */
   File getSdk();

   /**
    * Get server url
    *
    * @return server url
    */
   String getServer();

   /**
    * Get user name
    *
    * @return user name
    */
   String getUsername();

   /**
    * Get password
    *
    * @return password
    */
   String getPassword();

   /**
    * True if debug is enabled
    *
    * @return true or false
    */
   boolean isDebug();

   /**
    * True if help output requested
    *
    * @return true or false
    */
   boolean isHelp();

   /**
    * Get user id received after authentication
    *
    * @return user id
    */
   long getUserId();

   /**
    * Set user id
    *
    * @param userId
    */
   void setUserId(long userId);
}
