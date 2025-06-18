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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

import static org.apache.maven.wrapper.MavenWrapperMain.MVNW_REPOURL;

/**
 * Wrapper executor, running {@link Installer} to get a Maven distribution ready, followed by
 * {@link BootstrapMainStarter} to launch the Maven bootstrap.
 *
 * @author Hans Dockter
 */
public class WrapperExecutor {
    public static final String DISTRIBUTION_URL_PROPERTY = "distributionUrl";

    public static final String DISTRIBUTION_BASE_PROPERTY = "distributionBase";

    public static final String ZIP_STORE_BASE_PROPERTY = "zipStoreBase";

    public static final String DISTRIBUTION_PATH_PROPERTY = "distributionPath";

    public static final String ZIP_STORE_PATH_PROPERTY = "zipStorePath";

    public static final String DISTRIBUTION_SHA_256_SUM = "distributionSha256Sum";

    public static final String ALWAYS_DOWNLOAD = "alwaysDownload";

    public static final String ALWAYS_UNPACK = "alwaysUnpack";

    // JDK-related property constants
    public static final String JDK_VERSION_PROPERTY = "jdkVersion";
    public static final String JDK_VENDOR_PROPERTY = "jdkVendor";
    public static final String JDK_DISTRIBUTION_URL_PROPERTY = "jdkDistributionUrl";
    public static final String JDK_SHA_256_SUM = "jdkSha256Sum";
    public static final String ALWAYS_DOWNLOAD_JDK = "alwaysDownloadJdk";
    public static final String UPDATE_TOOLCHAINS = "updateToolchains";
    public static final String JDK_UPDATE_POLICY = "jdkUpdatePolicy";

    // Toolchain JDK property constants
    public static final String TOOLCHAIN_JDK_VERSION_PROPERTY = "toolchainJdkVersion";
    public static final String TOOLCHAIN_JDK_VENDOR_PROPERTY = "toolchainJdkVendor";
    public static final String TOOLCHAIN_JDK_DISTRIBUTION_URL_PROPERTY = "toolchainJdkDistributionUrl";
    public static final String TOOLCHAIN_JDK_SHA_256_SUM = "toolchainJdkSha256Sum";

    private final Properties properties;

    private final Path propertiesFile;

    private final WrapperConfiguration config = new WrapperConfiguration();

    public static WrapperExecutor forProjectDirectory(Path projectDir) {
        return new WrapperExecutor(projectDir.resolve("maven/wrapper/maven-wrapper.properties"), new Properties());
    }

    public static WrapperExecutor forWrapperPropertiesFile(Path propertiesFile) {
        if (Files.notExists(propertiesFile)) {
            throw new RuntimeException(
                    String.format(Locale.ROOT, "Wrapper properties file '%s' does not exist.", propertiesFile));
        }
        return new WrapperExecutor(propertiesFile, new Properties());
    }

    WrapperExecutor(Path propertiesFile, Properties properties) {
        this.properties = properties;
        this.propertiesFile = propertiesFile;
        if (Files.exists(propertiesFile)) {
            try {
                loadProperties(propertiesFile, properties);
                config.setDistribution(prepareDistributionUri());
                config.setDistributionBase(getProperty(DISTRIBUTION_BASE_PROPERTY, config.getDistributionBase()));
                config.setDistributionPath(Paths.get(getProperty(
                        DISTRIBUTION_PATH_PROPERTY, config.getDistributionPath().toString())));
                config.setZipBase(getProperty(ZIP_STORE_BASE_PROPERTY, config.getZipBase()));
                config.setZipPath(Paths.get(
                        getProperty(ZIP_STORE_PATH_PROPERTY, config.getZipPath().toString())));
                config.setDistributionSha256Sum(getProperty(DISTRIBUTION_SHA_256_SUM, ""));
                config.setAlwaysUnpack(Boolean.parseBoolean(getProperty(ALWAYS_UNPACK, Boolean.FALSE.toString())));
                config.setAlwaysDownload(Boolean.parseBoolean(getProperty(ALWAYS_DOWNLOAD, Boolean.FALSE.toString())));

                // Load JDK-related properties
                loadJdkProperties();
            } catch (Exception e) {
                throw new RuntimeException(
                        String.format(Locale.ROOT, "Could not load wrapper properties from '%s'.", propertiesFile), e);
            }
        }
    }

    protected String getEnv(String key) {
        return System.getenv(key);
    }

    private URI prepareDistributionUri() throws URISyntaxException {
        URI source = readDistroUrl();
        if (source.getScheme() == null) {
            // no scheme means someone passed a relative url. In our context only file relative urls make sense.
            return propertiesFile
                    .getParent()
                    .resolve(source.getSchemeSpecificPart())
                    .toUri();
        } else {
            String mvnwRepoUrl = getEnv(MVNW_REPOURL);
            if (mvnwRepoUrl != null && !mvnwRepoUrl.isEmpty()) {
                Logger.info("Detected MVNW_REPOURL environment variable " + mvnwRepoUrl);
                if (mvnwRepoUrl.endsWith("/")) {
                    mvnwRepoUrl = mvnwRepoUrl.substring(0, mvnwRepoUrl.length() - 1);
                }
                String distributionPath = source.getPath();
                int index = distributionPath.indexOf("org/apache/maven");
                if (index > 1) {
                    distributionPath = "/".concat(distributionPath.substring(index));
                } else {
                    Logger.warn("distributionUrl don't contain package name " + source.getPath());
                }
                return new URI(mvnwRepoUrl + distributionPath);
            }

            return source;
        }
    }

    private URI readDistroUrl() throws URISyntaxException {
        return new URI(getProperty(DISTRIBUTION_URL_PROPERTY));
    }

    private static void loadProperties(Path propertiesFile, Properties properties) throws IOException {
        try (InputStream inStream = Files.newInputStream(propertiesFile)) {
            properties.load(inStream);
        }
    }

    /**
     * Returns the Maven distribution which this wrapper will use. Returns null if no wrapper meta-data was found in the
     * specified project directory.
     *
     * @return the Maven distribution which this wrapper will use
     */
    public URI getDistribution() {
        return config.getDistribution();
    }

    /**
     * Returns the configuration for this wrapper.
     *
     * @return the configuration for this wrapper
     */
    public WrapperConfiguration getConfiguration() {
        return config;
    }

    public void execute(String[] args, Installer install, BootstrapMainStarter bootstrapMainStarter) throws Exception {
        Path mavenHome = install.createDist(config);
        bootstrapMainStarter.start(args, mavenHome);
    }

    private String getProperty(String propertyName) {
        return getProperty(propertyName, null);
    }

    private String getProperty(String propertyName, String defaultValue) {
        String value = properties.getProperty(propertyName, defaultValue);
        if (value == null) {
            reportMissingProperty(propertyName);
        }
        return value;
    }

    private void reportMissingProperty(String propertyName) {
        throw new RuntimeException(String.format(
                Locale.ROOT,
                "No value with key '%s' specified in wrapper properties file '%s'.",
                propertyName,
                propertiesFile));
    }

    /**
     * Loads JDK-related properties from the wrapper properties file.
     */
    private void loadJdkProperties() {
        // Load JDK properties with environment variable fallbacks
        String jdkVersion = getProperty(JDK_VERSION_PROPERTY, getEnv(WrapperConfiguration.JDK_VERSION_ENV));
        if (jdkVersion != null && !jdkVersion.trim().isEmpty()) {
            config.setJdkVersion(jdkVersion.trim());
        }

        String jdkVendor = getProperty(JDK_VENDOR_PROPERTY, getEnv(WrapperConfiguration.JDK_VENDOR_ENV));
        if (jdkVendor != null && !jdkVendor.trim().isEmpty()) {
            config.setJdkVendor(jdkVendor.trim());
        }

        String jdkDistributionUrl = getProperty(JDK_DISTRIBUTION_URL_PROPERTY, null);
        if (jdkDistributionUrl != null && !jdkDistributionUrl.trim().isEmpty()) {
            try {
                config.setJdkDistributionUrl(new URI(jdkDistributionUrl.trim()));
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid JDK distribution URL: " + jdkDistributionUrl, e);
            }
        }

        String jdkSha256Sum = getProperty(JDK_SHA_256_SUM, null);
        if (jdkSha256Sum != null && !jdkSha256Sum.trim().isEmpty()) {
            config.setJdkSha256Sum(jdkSha256Sum.trim());
        }

        config.setAlwaysDownloadJdk(
                Boolean.parseBoolean(getProperty(ALWAYS_DOWNLOAD_JDK, getEnv(WrapperConfiguration.JDK_DOWNLOAD_ENV))));

        config.setUpdateToolchains(Boolean.parseBoolean(getProperty(UPDATE_TOOLCHAINS, "true")));

        String jdkUpdatePolicy = getProperty(JDK_UPDATE_POLICY, "daily");
        if (JdkVersionCache.isValidUpdatePolicy(jdkUpdatePolicy)) {
            config.setJdkUpdatePolicy(jdkUpdatePolicy);
        } else {
            Logger.warn("Invalid JDK update policy: " + jdkUpdatePolicy + ". Using default 'daily'.");
            config.setJdkUpdatePolicy("daily");
        }

        // Load toolchain JDK properties
        loadToolchainJdkProperties();
    }

    /**
     * Loads toolchain JDK properties from the wrapper properties file.
     */
    private void loadToolchainJdkProperties() {
        String toolchainJdkVersion =
                getProperty(TOOLCHAIN_JDK_VERSION_PROPERTY, getEnv(WrapperConfiguration.TOOLCHAIN_JDK_ENV));
        if (toolchainJdkVersion != null && !toolchainJdkVersion.trim().isEmpty()) {
            config.setToolchainJdkVersion(toolchainJdkVersion.trim());
        }

        String toolchainJdkVendor = getProperty(TOOLCHAIN_JDK_VENDOR_PROPERTY, null);
        if (toolchainJdkVendor != null && !toolchainJdkVendor.trim().isEmpty()) {
            config.setToolchainJdkVendor(toolchainJdkVendor.trim());
        }

        String toolchainJdkDistributionUrl = getProperty(TOOLCHAIN_JDK_DISTRIBUTION_URL_PROPERTY, null);
        if (toolchainJdkDistributionUrl != null
                && !toolchainJdkDistributionUrl.trim().isEmpty()) {
            try {
                config.setToolchainJdkDistributionUrl(new URI(toolchainJdkDistributionUrl.trim()));
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid toolchain JDK distribution URL: " + toolchainJdkDistributionUrl, e);
            }
        }

        String toolchainJdkSha256Sum = getProperty(TOOLCHAIN_JDK_SHA_256_SUM, null);
        if (toolchainJdkSha256Sum != null && !toolchainJdkSha256Sum.trim().isEmpty()) {
            config.setToolchainJdkSha256Sum(toolchainJdkSha256Sum.trim());
        }
    }
}
