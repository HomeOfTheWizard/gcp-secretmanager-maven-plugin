package com.homeofthewizard.maven.plugins.gcp.secretmanager.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public enum OutputMethod {
  MavenProperties{
    @Override
    public void flush(Properties properties, Map<String, String> secrets, Mapping mapping, String storePassword, String existingStorePath, String storeType) {
      setMavenProperties(properties, secrets, mapping);
    }
  },
  SystemProperties{
    @Override
    public void flush(Properties properties, Map<String, String> secrets, Mapping mapping, String storePassword, String existingStorePath, String storeType) {
      setSystemProperties(secrets, mapping);
    }
  },
  EnvFile{
    @Override
    public void flush(Properties properties, Map<String, String> secrets, Mapping mapping, String storePassword, String existingStorePath, String storeType) {
      createEnvFile(secrets, mapping);
    }
  },
  File{
    @Override
    public void flush(Properties properties, Map<String, String> secrets, Mapping mapping, String storePassword, String existingStorePath, String storeType) {
      createFile(secrets, mapping);
    }
  },
  TrustStore{
   @Override
   public void flush(Properties properties, Map<String, String> secrets, Mapping mapping, String storePassword, String existingStorePath, String storeType) {
     OutputMethod.createTrustStore(secrets, mapping, storePassword, existingStorePath, storeType);
   }
  };

  public abstract void flush(Properties properties, Map<String, String> secrets, Mapping mapping, String storePassword, String existingStorePath, String storeType);

  /**
   * Creates an .envFile and put the secrets in it, respecting the key/property mapping definition given.
   * @param secrets secrets fetched from Vault.
   * @param mapping mapping defined in maven project.
   */
  private static void createEnvFile(Map<String, String> secrets, Mapping mapping) {
    try (FileWriter fileWriter = new FileWriter(".env", true)) {
      var buffer = new BufferedWriter(fileWriter);
      var printer = new PrintWriter(buffer);
      printer.format("%s=%s\n",mapping.getProperty(),secrets.get(mapping.getKey()));
      printer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void createFile(Map<String, String> secrets, Mapping mapping) {
    try (FileWriter fileWriter = new FileWriter(mapping.getProperty(), true)) {
      var buffer = new BufferedWriter(fileWriter);
      var printer = new PrintWriter(buffer);
      printer.format("%s",secrets.get(mapping.getKey()));
      printer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void createTrustStore(Map<String, String> secrets, Mapping mapping, String storePassword, String existingStorePath, String storeType) {
    var storePasswordCharArr = Objects.isNull(storePassword) ? new char[0] : storePassword.toCharArray();
    var type = !Objects.isNull(storeType) ? storeType : KeyStore.getDefaultType();

    try (FileInputStream storeInputStream = existingStorePath!=null ? new FileInputStream(existingStorePath) : null;
         InputStream certInputStream = new ByteArrayInputStream(secrets.get(mapping.getKey()).getBytes(StandardCharsets.UTF_8));
         FileOutputStream storeOutputStream = new FileOutputStream("truststore." + type.toLowerCase(Locale.ROOT))) {

      var keystore = KeyStore.getInstance(type);
      keystore.load(storeInputStream, storePasswordCharArr);

      CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      Certificate certificate = certificateFactory.generateCertificate(certInputStream);

      keystore.setCertificateEntry(mapping.getProperty(), certificate);
      keystore.store(storeOutputStream, storePasswordCharArr);

    } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Sets the secrets in System.getProperties(), respecting the key/property mapping definition given.
   * @param secrets secrets fetched from Vault.
   * @param mapping mapping defined in maven project.
   */
  private static void setSystemProperties(Map<String, String> secrets, Mapping mapping) {
    System.setProperty(mapping.getProperty(), secrets.get(mapping.getKey()));
  }

  /**
   * Sets the secrets in mavenProject.properties, respecting the key/property mapping definition given.
   * @param properties maven project properties
   * @param secrets secrets fetched from Vault.
   * @param mapping mapping defined in maven project.
   */
  private static void setMavenProperties(Properties properties, Map<String, String> secrets, Mapping mapping) {
    properties.setProperty(mapping.getProperty(), secrets.get(mapping.getKey()));
  }
}
