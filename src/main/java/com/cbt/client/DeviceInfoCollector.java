package com.cbt.client;

import com.cbt.ws.entity.DeviceType;
import org.apache.commons.exec.CommandLine;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.io.StringReader;
import java.util.Properties;

/**
 * Device information collector class
 *
 * @author SauliusAlisauskas 2013-03-22 Initial version
 */
public class DeviceInfoCollector {

   private static final Logger mLog = Logger.getLogger(DeviceInfoCollector.class);

   private CliExecutor mExecutor;
   private String mPathADB;

   @Inject
   public DeviceInfoCollector(CliExecutor cliExecutor, Configuration config) {
      mExecutor = cliExecutor;
      mPathADB = config.getPathAndroidADB();
   }

   /**
    * Query device information through ADB
    *
    * @param deviceSerial
    * @return
    * @throws Exception
    */
   public DeviceType getDeviceTypeInfo(String deviceSerial) throws Exception {
      String commandString = mPathADB + " -s " + deviceSerial + " shell cat /system/build.prop";
      CommandLine command = CommandLine.parse(commandString);
      int exitValue = mExecutor.execute(command);
      mLog.info("Exit value:" + exitValue);
      if (mExecutor.isFailure(exitValue)) {
         throw new Exception("Failed");
      } else {
         mLog.info("Success");
      }
      mLog.info("output:\n" + mExecutor.getOutput());
      Properties properties = new Properties();
      properties.load(new StringReader(mExecutor.getOutput()));

      DeviceType deviceType = new DeviceType();
      deviceType.setManufacture(properties.getProperty("ro.product.manufacturer"));
      deviceType.setModel(properties.getProperty("ro.product.model"));
      return deviceType;
   }
}
