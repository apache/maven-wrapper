/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import static org.apache.maven.wrapper.MavenWrapperMain.MVNW_PASSWORD;
import static org.apache.maven.wrapper.MavenWrapperMain.MVNW_USERNAME;

/**
 * @author Hans Dockter
 */
public class DefaultDownloader implements Downloader {
    private final String applicationName;

    private final String applicationVersion;

    public DefaultDownloader(String applicationName, String applicationVersion) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
        configureProxyAuthentication();
        configureAuthentication();
    }

    private void configureProxyAuthentication() {
        if (System.getProperty("http.proxyUser") != null) {
            Authenticator.setDefault(new SystemPropertiesProxyAuthenticator());
        }
    }

    private void configureAuthentication() {
        if (System.getenv(MVNW_USERNAME) != null
                && System.getenv(MVNW_PASSWORD) != null
                && System.getProperty("http.proxyUser") == null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            System.getenv(MVNW_USERNAME),
                            System.getenv(MVNW_PASSWORD).toCharArray());
                }
            });
        }
    }

    @Override
    public void download(URI address, Path destination) throws Exception {
        if (Files.exists(destination)) {
            return;
        }
        Files.createDirectories(destination.getParent());

        if (!"https".equals(address.getScheme())) {
            Logger.warn("Using an insecure connection to download the Maven distribution."
                    + " Please consider using HTTPS.");
        }
        downloadInternal(address, destination);
    }

    private void downloadInternal(URI address, Path destination) throws IOException {
        URL url = address.toURL();
        URLConnection conn = url.openConnection();
        addBasicAuthentication(address, conn);
        final String userAgentValue = calculateUserAgent();
        conn.setRequestProperty("User-Agent", userAgentValue);

        Path temp = Files.createTempDirectory(destination.getParent(), "wrapper-dl");
        try (InputStream inStream = conn.getInputStream()) {
            Files.copy(inStream, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void addBasicAuthentication(URI address, URLConnection connection) {
        String userInfo = calculateUserInfo(address);
        if (userInfo == null) {
            return;
        }
        connection.setRequestProperty("Authorization", "Basic " + base64Encode(userInfo));
    }

    /**
     * Base64 encode user info for HTTP Basic Authentication. Try to use {@literal java.util.Base64} encoder which is
     * available starting with Java 8. Fallback to {@literal javax.xml.bind.DatatypeConverter} from JAXB which is
     * available starting with Java 6 but is not anymore in Java 9. Fortunately, both of these two Base64 encoders
     * implement the right Base64 flavor, the one that does not split the output in multiple lines.
     *
     * @param userInfo user info
     * @return Base64 encoded user info
     * @throws RuntimeException if no public Base64 encoder is available on this JVM
     */
    private String base64Encode(String userInfo) {
        ClassLoader loader = getClass().getClassLoader();
        try {
            Method getEncoderMethod = loader.loadClass("java.util.Base64").getMethod("getEncoder");
            Method encodeMethod =
                    loader.loadClass("java.util.Base64$Encoder").getMethod("encodeToString", byte[].class);
            Object encoder = getEncoderMethod.invoke(null);
            return (String) encodeMethod.invoke(encoder, new Object[] {userInfo.getBytes(StandardCharsets.UTF_8)});
        } catch (Exception java7OrEarlier) {
            try {
                Method encodeMethod = loader.loadClass("javax.xml.bind.DatatypeConverter")
                        .getMethod("printBase64Binary", byte[].class);
                return (String) encodeMethod.invoke(null, new Object[] {userInfo.getBytes(StandardCharsets.UTF_8)});
            } catch (Exception java5OrEarlier) {
                throw new RuntimeException(
                        "Downloading Maven distributions with HTTP Basic Authentication"
                                + " is not supported on your JVM.",
                        java5OrEarlier);
            }
        }
    }

    private String calculateUserInfo(URI uri) {
        String username = System.getenv(MVNW_USERNAME);
        String password = System.getenv(MVNW_PASSWORD);
        if (username != null && password != null) {
            return username + ':' + password;
        }
        return uri.getUserInfo();
    }

    private String calculateUserAgent() {
        String appVersion = applicationVersion;

        String javaVendor = System.getProperty("java.vendor");
        String javaVersion = System.getProperty("java.version");
        String javaVendorVersion = System.getProperty("java.vm.version");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        return String.format(
                Locale.ROOT,
                "%s/%s (%s;%s;%s) (%s;%s;%s)",
                applicationName,
                appVersion,
                osName,
                osVersion,
                osArch,
                javaVendor,
                javaVersion,
                javaVendorVersion);
    }

    private static class SystemPropertiesProxyAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(
                    System.getProperty("http.proxyUser"),
                    System.getProperty("http.proxyPassword", "").toCharArray());
        }
    }
}
