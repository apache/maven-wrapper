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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles downloading and installing JDKs
 */
public class JdkDownloader {
    private final Downloader downloader;
    private final PathAssembler pathAssembler;
    private final Verifier verifier;

    public JdkDownloader(Downloader downloader, PathAssembler pathAssembler, Verifier verifier) {
        this.downloader = downloader;
        this.pathAssembler = pathAssembler;
        this.verifier = verifier;
    }

    public Path download(URI jdkDistribution, String jdkVersion, boolean isToolchain, String sha256Sum)
            throws Exception {
        PathAssembler.LocalDistribution localDistribution =
                getLocalDistribution(jdkDistribution, jdkVersion, isToolchain);

        Path jdkDir = localDistribution.getDistributionDir();
        Path jdkHome = findJdkHome(jdkDir);

        if (jdkHome != null && Files.exists(jdkHome)) {
            return jdkHome;
        }

        Path zipFile = localDistribution.getZipFile();

        boolean downloaded = false;
        if (!Files.exists(zipFile) || Files.size(zipFile) == 0) {
            Files.createDirectories(zipFile.getParent());
            downloader.download(jdkDistribution, zipFile);
            downloaded = true;
        }

        // Validate checksum if provided
        if (sha256Sum != null && !sha256Sum.isEmpty()) {
            try {
                verifier.verify(zipFile, "jdkSha256Sum", Verifier.SHA_256_ALGORITHM, sha256Sum);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Error: Failed to validate JDK SHA-256, your JDK distribution might be compromised.", e);
            }
        }

        // Extract JDK
        if (downloaded || !Files.exists(jdkDir) || Files.list(jdkDir).count() == 0) {
            Files.createDirectories(jdkDir);
            extractJdk(zipFile, jdkDir);
        }

        jdkHome = findJdkHome(jdkDir);
        if (jdkHome == null) {
            throw new RuntimeException("Could not locate JDK home directory inside distribution");
        }

        return jdkHome;
    }

    private PathAssembler.LocalDistribution getLocalDistribution(
            URI jdkDistribution, String jdkVersion, boolean isToolchain) {
        String prefix = isToolchain ? "toolchain-" : "";
        String name = prefix + "jdk-" + jdkVersion;

        // Create a temporary configuration for JDK distribution
        WrapperConfiguration jdkConfig = new WrapperConfiguration();
        jdkConfig.setDistribution(jdkDistribution);
        jdkConfig.setDistributionBase(PathAssembler.MAVEN_USER_HOME_STRING);
        jdkConfig.setDistributionPath(Paths.get("wrapper", "jdks"));
        jdkConfig.setZipBase(PathAssembler.MAVEN_USER_HOME_STRING);
        jdkConfig.setZipPath(Paths.get("wrapper", "jdks"));

        return pathAssembler.getDistribution(jdkConfig);
    }

    private Path findJdkHome(Path extractDir) throws IOException {
        // Look for bin/java or bin/javac to identify JDK home
        if (Files.exists(extractDir.resolve("bin/java")) || Files.exists(extractDir.resolve("bin/javac"))) {
            return extractDir;
        }

        // Check if there's a single directory that contains the JDK
        try {
            return Files.list(extractDir)
                    .filter(Files::isDirectory)
                    .filter(dir -> Files.exists(dir.resolve("bin/java")) || Files.exists(dir.resolve("bin/javac")))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private void extractJdk(Path zipFile, Path extractDir) throws IOException {
        System.out.println("Extracting JDK to " + extractDir);

        // Create a temporary installer instance for extraction
        Installer installer = new Installer(downloader, verifier, pathAssembler);
        installer.unzip(zipFile, extractDir);
    }
}
