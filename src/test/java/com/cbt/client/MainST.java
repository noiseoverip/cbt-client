package com.cbt.client;

import org.junit.Test;

/**
 * Main application system tests
 *
 * @author iljabobkevic 2013-10-04 initial version
 */
public class MainST {

   /**
    * Method: main(String... args). Time out after 2 minutes
    */
   @Test(timeout = 120000L)
   public void testMain() throws Exception {
      Main.main();
   }

} 
