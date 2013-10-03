package com.cbt.client.configuration;

/**
 * Class AbstractConfiguration
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public abstract class AbstractConfiguration implements Configuration {

   private long userId;

   /**
    * Get user id
    *
    * @return user id
    */
   public long getUserId() {
      return userId;
   }

   /**
    * Set user id
    *
    * @param userId
    */
   public void setUserId(long userId) {
      this.userId = userId;
   }
}
