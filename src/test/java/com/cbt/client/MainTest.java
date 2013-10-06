package com.cbt.client;

import org.junit.Test;

/**
 * Main application unit tests
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public class MainTest {

   /**
    * Method: main(String... args) with bad sdk path defined
    */
   @Test(expected = RuntimeException.class, timeout = 5000L)
   public void testMainBadSdk() throws Exception {
      Main.main("--sdk", "blah");
   }


} 
