package com.cbt.client.configuration;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.cbt.core.utils.Utils;
import com.google.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class ConfigurationImpl
 *
 * @author iljabobkevic 2013-10-02 initial version
 */
@Singleton
public class ConfigurationImpl extends AbstractConfiguration {

   /**
    * <p>
    * File converter is used to convert strings into {@link java.io.File} objects.
    * </p>
    * <p>
    * JCommander deems it necessary that this class be public. Lame.
    * </p>
    */
   public static class FileConverter implements IStringConverter<File> {
      public static File cleanFile(String path) {
         if (path == null) {
            return null;
         }
         return new File(path);
      }

      @Override
      public File convert(String s) {
         return cleanFile(s);
      }
   }

   /**
    * Password converter used for md5 hashing the password
    */
   public static class PasswordConverter implements IStringConverter<String> {

      @Override
      public String convert(String value) {
         return Utils.md5(value);
      }
   }

   @Parameter(names = {"--workspace", "-w"}, description = "Client workspace path where temporary files will be stored")
   private File workspace = getTempDirectory();
   @Parameter(names = {"--sdk"}, converter = FileConverter.class, description = "Path to Android SDK")
   private File sdk = getSdkPath();
   @Parameter(names = {"--server", "-s"}, required = true, description = "Server URL")
   private String server;
   @Parameter(names = {"--username", "-u"}, required = true, description = "User name to use to login to the server")
   private String username;
   @Parameter(names = {"--password", "-p"}, converter = PasswordConverter.class, required = true, description = "User name to use to login to the server")
   private String password;
   @Parameter(names = {"--debug"}, hidden = true)
   private boolean debug;
   @Parameter(names = {"-h", "--help", "help"}, description = "Command help", help = true, hidden = true)
   private boolean help;

   /**
    * Get client's temporary directory
    *
    * @return temporary directory
    */
   private static File getTempDirectory() {
      Path tmpDir = null;
      try {
         tmpDir = Files.createTempDirectory("cbt-client");
      } catch (IOException e) {
         throw new RuntimeException("Failed creating temporary directory for workspace!", e);
      }
      return tmpDir.toFile();
   }

   /**
    * Get android sdk path
    *
    * @return android sdk path
    */
   private static File getSdkPath() {
      String env = System.getenv("ANDROID_HOME");
      String prop = System.getProperty("android.sdk.path");
      return FileConverter.cleanFile(prop != null ? prop : env);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public File getWorkspace() {
      return workspace;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public File getSdk() {
      return sdk;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getServer() {
      return server;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getUsername() {
      return username;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getPassword() {
      return password;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isDebug() {
      return debug;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isHelp() {
      return help;
   }
}