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

/**
 * Downloads and manages JDK installations for Maven Wrapper.
 * Supports automatic JDK resolution, download, extraction, and toolchain integration.
 */
class JdkDownloader {

    private final JdkResolver jdkResolver;
    private final BinaryDownloader binaryDownloader;
    private final ToolchainManager toolchainManager;
    private final PathAssembler pathAssembler;

    JdkDownloader(Downloader downloader, Verifier verifier, PathAssembler pathAssembler) {
        this(downloader, verifier, pathAssembler, "daily");
    }

    JdkDownloader(Downloader downloader, Verifier verifier, PathAssembler pathAssembler, String updatePolicy) {
        // Use pathAssembler base directory for JDK version cache
        Path cacheDir = pathAssembler.getBaseDir().resolve("wrapper").resolve("cache");
        this.jdkResolver = new JdkResolver(cacheDir, updatePolicy);
        this.binaryDownloader = new DefaultBinaryDownloader(downloader, verifier);
        this.toolchainManager = new ToolchainManager();
        this.pathAssembler = pathAssembler;
    }

    /**
     * Downloads and installs a JDK based on the wrapper configuration.
     *
     * @param configuration the wrapper configuration containing JDK settings
     * @return path to the installed JDK home directory
     * @throws IOException if JDK download or installation fails
     */
    Path downloadAndInstallJdk(WrapperConfiguration configuration) throws IOException {
        String jdkVersion = configuration.getJdkVersion();
        if (jdkVersion == null || jdkVersion.trim().isEmpty()) {
            return null; // No JDK configuration
        }

        Logger.info("Setting up JDK " + jdkVersion);

        // Check if we have a direct URL or need to resolve
        URI jdkUrl = configuration.getJdkDistributionUrl();
        String sha256Sum = configuration.getJdkSha256Sum();
        String vendor = configuration.getJdkVendor();

        if (jdkUrl == null) {
            // Resolve JDK metadata from version and vendor
            JdkResolver.JdkMetadata metadata = jdkResolver.resolveJdk(jdkVersion, vendor);
            jdkUrl = metadata.getDownloadUrl();
            if (sha256Sum == null || sha256Sum.trim().isEmpty()) {
                sha256Sum = metadata.getSha256Sum();
            }
            if (vendor == null || vendor.trim().isEmpty()) {
                vendor = metadata.getVendor();
            }
        }

        // Determine JDK installation directory
        Path jdkInstallDir = getJdkInstallDirectory(jdkVersion, vendor);

        // Check if JDK is already installed and not forcing re-download
        if (Files.exists(jdkInstallDir) && !configuration.isAlwaysDownloadJdk()) {
            Logger.info("JDK " + jdkVersion + " already installed at " + jdkInstallDir);
            return findJdkHome(jdkInstallDir);
        }

        // Download and extract JDK
        Logger.info("Downloading JDK " + jdkVersion + " from " + jdkUrl);
        Path extractedDir = binaryDownloader.downloadAndExtract(jdkUrl, jdkInstallDir, sha256Sum);

        // Find the actual JDK home directory
        Path jdkHome = findJdkHome(extractedDir);

        // Update toolchains.xml if enabled
        if (configuration.isUpdateToolchains()) {
            updateToolchain(jdkVersion, vendor, jdkHome);
        }

        Logger.info("JDK " + jdkVersion + " installed successfully at " + jdkHome);
        return jdkHome;
    }

    /**
     * Downloads and installs a toolchain JDK based on the wrapper configuration.
     *
     * @param configuration the wrapper configuration containing toolchain JDK settings
     * @return path to the installed toolchain JDK home directory
     * @throws IOException if toolchain JDK download or installation fails
     */
    Path downloadAndInstallToolchainJdk(WrapperConfiguration configuration) throws IOException {
        String toolchainJdkVersion = configuration.getToolchainJdkVersion();
        if (toolchainJdkVersion == null || toolchainJdkVersion.trim().isEmpty()) {
            return null; // No toolchain JDK configuration
        }

        Logger.info("Setting up toolchain JDK " + toolchainJdkVersion);

        // Check if we have a direct URL or need to resolve
        URI toolchainJdkUrl = configuration.getToolchainJdkDistributionUrl();
        String sha256Sum = configuration.getToolchainJdkSha256Sum();
        String vendor = configuration.getToolchainJdkVendor();

        if (toolchainJdkUrl == null) {
            // Resolve JDK metadata from version and vendor
            JdkResolver.JdkMetadata metadata = jdkResolver.resolveJdk(toolchainJdkVersion, vendor);
            toolchainJdkUrl = metadata.getDownloadUrl();
            if (sha256Sum == null || sha256Sum.trim().isEmpty()) {
                sha256Sum = metadata.getSha256Sum();
            }
            if (vendor == null || vendor.trim().isEmpty()) {
                vendor = metadata.getVendor();
            }
        }

        // Determine toolchain JDK installation directory
        Path toolchainJdkInstallDir = getJdkInstallDirectory(toolchainJdkVersion, vendor);

        // Check if toolchain JDK is already installed and not forcing re-download
        if (Files.exists(toolchainJdkInstallDir) && !configuration.isAlwaysDownloadJdk()) {
            Logger.info("Toolchain JDK " + toolchainJdkVersion + " already installed at " + toolchainJdkInstallDir);
            return findJdkHome(toolchainJdkInstallDir);
        }

        // Download and extract toolchain JDK
        Logger.info("Downloading toolchain JDK " + toolchainJdkVersion + " from " + toolchainJdkUrl);
        Path extractedDir = binaryDownloader.downloadAndExtract(toolchainJdkUrl, toolchainJdkInstallDir, sha256Sum);

        // Find the actual JDK home directory
        Path jdkHome = findJdkHome(extractedDir);

        // Always update toolchains.xml for toolchain JDKs
        updateToolchain(toolchainJdkVersion, vendor, jdkHome);

        Logger.info("Toolchain JDK " + toolchainJdkVersion + " installed successfully at " + jdkHome);
        return jdkHome;
    }

    /**
     * Determines the installation directory for a JDK.
     */
    private Path getJdkInstallDirectory(String version, String vendor) {
        String jdkDirName = "jdk-" + version + "-" + (vendor != null ? vendor : "unknown");
        return pathAssembler.getBaseDir().resolve("jdks").resolve(jdkDirName);
    }

    /**
     * Finds the JDK home directory within the extracted directory.
     * Looks for common JDK directory structures up to 3 levels deep to handle
     * various archive formats (e.g., macOS Contents/Home structure).
     */
    private Path findJdkHome(Path extractedDir) throws IOException {
        if (!Files.exists(extractedDir)) {
            throw new IOException("Extracted directory does not exist: " + extractedDir);
        }

        // Search for JDK home up to 3 levels deep to handle various archive structures
        Path jdkHome = findJdkHomeRecursive(extractedDir, 0, 3);
        if (jdkHome != null) {
            return jdkHome;
        }

        throw new IOException("Could not find JDK home directory in " + extractedDir);
    }

    /**
     * Recursively searches for JDK home directory up to the specified depth.
     */
    private Path findJdkHomeRecursive(Path dir, int currentDepth, int maxDepth) throws IOException {
        if (currentDepth > maxDepth || !Files.isDirectory(dir)) {
            return null;
        }

        // Check if current directory is JDK home
        if (isJdkHome(dir)) {
            return dir;
        }

        // If we haven't reached max depth, search subdirectories
        if (currentDepth < maxDepth) {
            try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        Path result = findJdkHomeRecursive(entry, currentDepth + 1, maxDepth);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if a directory is a valid JDK home by looking for key files/directories.
     */
    private boolean isJdkHome(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }

        // Check for bin directory and java executable
        Path binDir = dir.resolve("bin");
        if (!Files.isDirectory(binDir)) {
            return false;
        }

        // Check for java executable (with or without .exe extension)
        Path javaExe = binDir.resolve("java");
        Path javaExeWindows = binDir.resolve("java.exe");

        return Files.exists(javaExe) || Files.exists(javaExeWindows);
    }

    /**
     * Updates the toolchains.xml file with the installed JDK.
     */
    private void updateToolchain(String version, String vendor, Path jdkHome) {
        try {
            String normalizedVendor = vendor != null ? vendor : "unknown";
            ToolchainManager.JdkToolchain toolchain =
                    toolchainManager.createJdkToolchain(version, normalizedVendor, jdkHome);
            toolchainManager.addOrUpdateJdkToolchain(toolchain);
        } catch (IOException e) {
            Logger.warn("Failed to update toolchains.xml: " + e.getMessage());
        }
    }
}
