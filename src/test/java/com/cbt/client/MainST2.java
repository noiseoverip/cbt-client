package com.cbt.client;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.beust.jcommander.IDefaultProvider;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.cbt.client.configuration.Configuration;
import com.cbt.client.device.DeviceWorker;
import com.cbt.client.guice.GuiceModuleConfiguration;
import com.cbt.client.guice.GuiceModuleMain;
import com.cbt.client.util.SupervisorFactory;
import com.cbt.client.util.Utils;
import com.cbt.client.ws.WsClient;
import com.cbt.core.entity.Device;
import com.cbt.core.entity.DeviceJob;
import com.cbt.core.entity.DeviceJobResult;
import com.cbt.jooq.enums.DeviceJobResultState;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Class CbtClientST
 *
 * @author iljabobkevic 2013-10-19 initial version
 */
public class MainST2 {
   private static final int CLIENT_THREAD_POOL_SIZE = 1;
   private static final int NUMBER_OF_DEVICES = 5;
   private final Logger logger = Logger.getLogger(MainST2.class);

   private AbstractModule getMockedUtilsModule() {
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

      // Mock utils
      final Utils utils = mock(Utils.class);
      when(utils.findAllDevices(any(AndroidDebugBridge.class))).thenReturn(deviceMap);

      return new AbstractModule() {
         @Override
         protected void configure() {
            bind(Utils.class).toInstance(utils);
         }
      };
   }

   private AbstractModule getSpiedDeviceJobModule(Injector injector) throws IOException {
      final ArgumentCaptor<DeviceJob> deviceJobArgument = ArgumentCaptor.forClass(DeviceJob.class);
      final DeviceWorker deviceWorker = spy(new DeviceWorker(injector.getInstance(Configuration.class),
            injector.getInstance(WsClient.class), injector.getInstance(Utils.class)));
      doAnswer(new Answer<DeviceJobResult>() {
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
      }).when(deviceWorker).runTest(any(Device.class), any(DeviceJob.class), anyBoolean());

      AbstractModule spiedDeviceWorkerModule = new AbstractModule() {
         @Override
         protected void configure() {
            bind(DeviceWorker.class).toInstance(deviceWorker);
         }
      };

      return spiedDeviceWorkerModule;
   }

   private void parseArguments(Injector injector, String[] args) {
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
   }

   /**
    * Method: main()
    */
   @Test
   public void main() throws IOException, InterruptedException {
      String[] args = {};
      logger.info("Starting application...");

      Module mainModuleOverride = Modules.override(new GuiceModuleMain()).with(getMockedUtilsModule());
      Injector injector = Guice.createInjector(mainModuleOverride, new GuiceModuleConfiguration());
      parseArguments(injector, args);
      injector = Guice.createInjector(Modules.override(mainModuleOverride).with(getSpiedDeviceJobModule(injector)),
            new GuiceModuleConfiguration());
      parseArguments(injector, args);

      // Start the client thread with supervision
      ExecutorService pool = Executors.newFixedThreadPool(CLIENT_THREAD_POOL_SIZE);

      // Supervision is done on the main thread, so it's blocking call
      SupervisorFactory.supervise(pool.submit(injector.getInstance(CbtClient.class)), true);

      pool.shutdownNow();
      logger.info("Application finished");
   }

}
