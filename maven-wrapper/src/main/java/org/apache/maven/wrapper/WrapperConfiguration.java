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

import java.net.URI;
import java.nio.file.Path;

/**
 * Wrapper configuration.
 */
public class WrapperConfiguration {
    public static final String ALWAYS_UNPACK_ENV = "MAVEN_WRAPPER_ALWAYS_UNPACK";

    public static final String ALWAYS_DOWNLOAD_ENV = "MAVEN_WRAPPER_ALWAYS_DOWNLOAD";

    public static final String JDK_VERSION_ENV = "MAVEN_WRAPPER_JDK_VERSION";

    public static final String JDK_DOWNLOAD_ENV = "MAVEN_WRAPPER_JDK_DOWNLOAD";

    public static final String TOOLCHAIN_JDK_ENV = "MAVEN_WRAPPER_TOOLCHAIN_JDK";

    private boolean alwaysUnpack = Boolean.parseBoolean(System.getenv(ALWAYS_UNPACK_ENV));

    private boolean alwaysDownload = Boolean.parseBoolean(System.getenv(ALWAYS_DOWNLOAD_ENV));

    private URI distribution;

    private String distributionBase = PathAssembler.MAVEN_USER_HOME_STRING;

    private Path distributionPath = Installer.DEFAULT_DISTRIBUTION_PATH;

    private String jdkVersion;

    private String jdkDistributionUrl;

    private String jdkSha256Sum;

    private String toolchainJdkVersion;

    private String toolchainJdkDistributionUrl;

    private String toolchainJdkSha256Sum;

    private String zipBase = PathAssembler.MAVEN_USER_HOME_STRING;

    private Path zipPath = Installer.DEFAULT_DISTRIBUTION_PATH;

    private boolean alwaysDownloadJdk;

    private String distributionSha256Sum;

    public boolean isAlwaysDownload() {
        return alwaysDownload;
    }

    public void setAlwaysDownload(boolean alwaysDownload) {
        this.alwaysDownload = alwaysDownload;
    }

    public boolean isAlwaysUnpack() {
        return alwaysUnpack;
    }

    public void setAlwaysUnpack(boolean alwaysUnpack) {
        this.alwaysUnpack = alwaysUnpack;
    }

    public URI getDistribution() {
        return distribution;
    }

    public void setDistribution(URI distribution) {
        this.distribution = distribution;
    }

    public String getDistributionBase() {
        return distributionBase;
    }

    public void setDistributionBase(String distributionBase) {
        this.distributionBase = distributionBase;
    }

    public Path getDistributionPath() {
        return distributionPath;
    }

    public void setDistributionPath(Path distributionPath) {
        this.distributionPath = distributionPath;
    }

    public String getZipBase() {
        return zipBase;
    }

    public void setZipBase(String zipBase) {
        this.zipBase = zipBase;
    }

    public Path getZipPath() {
        return zipPath;
    }

    public void setZipPath(Path zipPath) {
        this.zipPath = zipPath;
    }

    public String getDistributionSha256Sum() {
        return distributionSha256Sum;
    }

    public void setDistributionSha256Sum(String distributionSha256Sum) {
        this.distributionSha256Sum = distributionSha256Sum;
    }

    public String getJdkVersion() {
        return jdkVersion;
    }

    public void setJdkVersion(String jdkVersion) {
        this.jdkVersion = jdkVersion;
    }

    public String getJdkDistributionUrl() {
        return jdkDistributionUrl;
    }

    public void setJdkDistributionUrl(String jdkDistributionUrl) {
        this.jdkDistributionUrl = jdkDistributionUrl;
    }

    public String getJdkSha256Sum() {
        return jdkSha256Sum;
    }

    public void setJdkSha256Sum(String jdkSha256Sum) {
        this.jdkSha256Sum = jdkSha256Sum;
    }

    public String getToolchainJdkVersion() {
        return toolchainJdkVersion;
    }

    public void setToolchainJdkVersion(String toolchainJdkVersion) {
        this.toolchainJdkVersion = toolchainJdkVersion;
    }

    public String getToolchainJdkDistributionUrl() {
        return toolchainJdkDistributionUrl;
    }

    public void setToolchainJdkDistributionUrl(String toolchainJdkDistributionUrl) {
        this.toolchainJdkDistributionUrl = toolchainJdkDistributionUrl;
    }

    public String getToolchainJdkSha256Sum() {
        return toolchainJdkSha256Sum;
    }

    public void setToolchainJdkSha256Sum(String toolchainJdkSha256Sum) {
        this.toolchainJdkSha256Sum = toolchainJdkSha256Sum;
    }

    public boolean isAlwaysDownloadJdk() {
        return alwaysDownloadJdk;
    }

    public void setAlwaysDownloadJdk(boolean alwaysDownloadJdk) {
        this.alwaysDownloadJdk = alwaysDownloadJdk;
    }
}
