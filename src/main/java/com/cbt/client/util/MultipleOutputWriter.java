package com.cbt.client.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Class MultipleOutputWriter
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
public class MultipleOutputWriter extends OutputStream {

   private OutputStream[] outs;

   public MultipleOutputWriter(OutputStream... outs) {
      this.outs = outs;
   }

   @Override
   public void write(int b) throws IOException {
      for (OutputStream out : outs) {
         out.write(b);
      }
   }

   public void flush() throws IOException {
      for (OutputStream out : outs) {
         out.flush();
      }
   }

   public void close() throws IOException {
      for (OutputStream out : outs) {
         try {
            flush();
         } catch (IOException ignored) {
         }
         out.close();
      }
   }
}
