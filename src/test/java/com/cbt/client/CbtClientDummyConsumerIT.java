package com.cbt.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.android.ddmlib.IDevice;
import com.beust.jcommander.IDefaultProvider;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.cbt.client.configuration.Configuration;
import com.cbt.client.configuration.ConfigurationImpl;
import com.cbt.client.device.AdbWrapper;
import com.cbt.client.device.DeviceMonitor;
import com.cbt.client.guice.GuiceModuleConfiguration;
import com.cbt.client.util.SupervisorFactory;
import com.cbt.client.util.Utils;
import com.cbt.client.ws.ClientAuthFilter;
import com.cbt.client.ws.WsClient;
import com.cbt.core.entity.Device;
import com.cbt.core.entity.DeviceJob;
import com.cbt.core.entity.DeviceJobResult;
import com.cbt.jooq.enums.DeviceJobResultState;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Device simulator tool. Will register devices and consume {@link DeviceJob}'s provided by server. This one requires
 * server to be running as well as valid parameters
 * 
 * @author SauliusAlisauskas 2013-10-13 Initial version
 * 
 */
public class CbtClientDummyConsumerIT {
   private static final int CLIENT_THREAD_POOL_SIZE = 1;
   private final static Logger logger = Logger.getLogger(CbtClientDummyConsumerIT.class);
   private static final int NUMBER_OF_DEVICES = 5;

   @Test
   public void run() throws IOException, InterruptedException {

      String[] args = { "--username", "testuser1", "--password", "testuser1", "--server", "http://127.0.0.1:9090",
            "--sdk", "C:\\Dev\\Tools\\android-sdk" };

      logger.info("Starting application...");

      // Mock a number of devices
      Map<String, IDevice> deviceMap = new HashMap<String, IDevice>();
      for (int i = 0; i < NUMBER_OF_DEVICES; i++) {
         IDevice device = mock(IDevice.class);
         when(device.isOnline()).thenReturn(true);
         when(device.getSerialNumber()).thenReturn(UUID.randomUUID().toString());
         when(device.getProperty("ro.product.manufacturer")).thenReturn("HTC");
         when(device.getProperty("ro.product.model")).thenReturn("somemodel");
         deviceMap.put(device.getSerialNumber(), device);
      }

      // Mock querying ADB for connected devices
      final AdbWrapper mockedAdb = mock(AdbWrapper.class);
      when(mockedAdb.findAllDevices()).thenReturn(deviceMap);

      // Mock DeviceJob execution and result retrieval from device

      final ArgumentCaptor<DeviceJob> deviceJobArgument = ArgumentCaptor.forClass(DeviceJob.class);
      when(mockedAdb.runTest(any(Device.class), deviceJobArgument.capture(), any(Boolean.class))).thenAnswer(
            new Answer<DeviceJobResult>() {

               @Override
               public DeviceJobResult answer(InvocationOnMock invocation) throws Throwable {
                  DeviceJobResult jobResult = new DeviceJobResult();
                  jobResult.setTestsErrors(0);
                  jobResult.setTestsFailed(0);
                  jobResult.setState(DeviceJobResultState.FAILED);
                  jobResult.setTestsFailed(1);
                  jobResult.setDevicejobId(deviceJobArgument.getValue().getId());
                  jobResult.setTestsRun(3);
                  jobResult.setOutput("This is magically generated output");
                  jobResult.setState(DeviceJobResultState.PASSED);
                  return jobResult;
               }

            });

      // Initialize guice injector
      Injector injector = Guice.createInjector(new AbstractModule() {

         @Override
         protected void configure() {
            bind(CbtClient.class);
            bind(DeviceMonitor.class);
            bind(WsClient.class);
            bind(ClientAuthFilter.class).in(Singleton.class);
            bind(Configuration.class).to(ConfigurationImpl.class).in(Singleton.class);
            bind(AdbWrapper.class).toInstance(mockedAdb);

         }
      }, new GuiceModuleConfiguration());

      // Prepare for argument parsing
      Configuration parsedArgs = injector.getInstance(Configuration.class);
      JCommander jc = new JCommander(parsedArgs);
      jc.setDefaultProvider(injector.getInstance(IDefaultProvider.class));

      logger.info("Parsing arguments...");
      try {
         jc.parse(args);
      } catch (ParameterException e) {
         StringBuilder out = new StringBuilder(e.getLocalizedMessage()).append("\n\n");
         jc.usage(out);
         logger.error(out.toString());
         return;
      }
      if (parsedArgs.isHelp()) {
         jc.usage();
         return;
      }

      if (parsedArgs.isDebug()) {
         Logger.getRootLogger().setLevel(Level.DEBUG);
      }

      // Initialize adb
      Utils.initAdb(parsedArgs.getSdk());

      // Start the client thread with supervision
      ExecutorService pool = Executors.newFixedThreadPool(CLIENT_THREAD_POOL_SIZE);

      // Supervision is done on the main thread, so it's blocking call
      SupervisorFactory.supervise(pool.submit(injector.getInstance(CbtClient.class)), true);

      // Terminate adb and stop
      Utils.terminateAdb();
      pool.shutdownNow();
      logger.info("Application finished");
      return;

   }

}
