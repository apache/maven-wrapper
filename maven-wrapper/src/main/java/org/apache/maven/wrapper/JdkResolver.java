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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

/**
 * Resolves JDK versions and vendors to download URLs and checksums.
 * Supports multiple JDK vendors and platforms using SDKMAN API with Maven-style caching.
 */
class JdkResolver {
    private static final int CONNECTION_TIMEOUT_MS = 10000; // 10 seconds
    private static final int READ_TIMEOUT_MS = 30000; // 30 seconds

    private final JdkVersionCache versionCache;

    /**
     * Represents JDK metadata including download URL and checksum.
     */
    static class JdkMetadata {
        private final URI downloadUrl;
        private final String sha256Sum;
        private final String version;
        private final String vendor;

        JdkMetadata(URI downloadUrl, String sha256Sum, String version, String vendor) {
            this.downloadUrl = downloadUrl;
            this.sha256Sum = sha256Sum;
            this.version = version;
            this.vendor = vendor;
        }

        public URI getDownloadUrl() {
            return downloadUrl;
        }

        public String getSha256Sum() {
            return sha256Sum;
        }

        public String getVersion() {
            return version;
        }

        public String getVendor() {
            return vendor;
        }
    }

    /**
     * Creates a JdkResolver with the specified cache directory and update policy.
     */
    JdkResolver(Path cacheDir, String updatePolicy) {
        this.versionCache = new JdkVersionCache(cacheDir, updatePolicy);
    }

    /**
     * Creates a JdkResolver with default caching (daily updates).
     */
    JdkResolver() {
        this(null, "daily");
    }

    /**
     * Resolves JDK metadata for the given version and vendor using SDKMAN API.
     *
     * @param version JDK version (e.g., "17", "21", "11.0.19")
     * @param vendor JDK vendor (e.g., "temurin", "corretto", "zulu", "liberica")
     * @return JDK metadata including download URL and checksum
     * @throws IOException if resolution fails
     */
    JdkMetadata resolveJdk(String version, String vendor) throws IOException {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("JDK version cannot be null or empty");
        }

        String normalizedVendor = normalizeVendor(vendor);
        String sdkmanVersion = resolveSdkmanVersion(version, normalizedVendor);
        String platform = detectSdkmanPlatform();

        return resolveJdkFromSdkman(sdkmanVersion, platform);
    }

    /**
     * Resolves JDK metadata using default vendor (Temurin).
     */
    JdkMetadata resolveJdk(String version) throws IOException {
        return resolveJdk(version, "temurin");
    }

    private String normalizeVendor(String vendor) {
        if (vendor == null || vendor.trim().isEmpty()) {
            return "temurin"; // Default to Eclipse Temurin
        }

        String normalized = vendor.toLowerCase(Locale.ROOT).trim();

        // Handle common aliases
        switch (normalized) {
            case "adoptium":
            case "adoptopenjdk":
            case "eclipse":
                return "temurin";
            case "amazon":
            case "aws":
                return "corretto";
            case "azul":
                return "zulu";
            case "bellsoft":
                return "liberica";
            case "oracle":
                return "oracle";
            case "microsoft":
            case "ms":
                return "microsoft";
            case "ibm":
            case "semeru":
                return "semeru";
            case "graal":
            case "graalvm":
                return "graalvm";
            default:
                return normalized;
        }
    }

    /**
     * Detects the SDKMAN platform identifier for the current system.
     * Uses the same logic as SDKMAN's infer_platform() function.
     */
    private String detectSdkmanPlatform() {
        String kernel = System.getProperty("os.name");
        String machine = System.getProperty("os.arch");

        if (kernel.startsWith("Linux")) {
            switch (machine.toLowerCase(Locale.ROOT)) {
                case "i686":
                    return "linuxx32";
                case "x86_64":
                case "amd64":
                    return "linuxx64";
                case "armv6l":
                case "armv7l":
                case "armv8l":
                    return "linuxarm32hf";
                case "aarch64":
                case "arm64":
                    return "linuxarm64";
                default:
                    return "exotic";
            }
        } else if (kernel.startsWith("Mac OS X") || kernel.startsWith("Darwin")) {
            switch (machine.toLowerCase(Locale.ROOT)) {
                case "x86_64":
                case "amd64":
                    return "darwinx64";
                case "arm64":
                case "aarch64":
                    return "darwinarm64";
                default:
                    return "darwinx64";
            }
        } else if (kernel.startsWith("Windows")) {
            switch (machine.toLowerCase(Locale.ROOT)) {
                case "x86_64":
                case "amd64":
                    return "windowsx64";
                default:
                    return "exotic";
            }
        } else {
            return "exotic";
        }
    }

    /**
     * Resolves SDKMAN version identifier from user-friendly version and vendor.
     */
    private String resolveSdkmanVersion(String version, String vendor) throws IOException {
        // Map vendor to SDKMAN suffix
        String suffix = getSdkmanVendorSuffix(vendor);

        // Handle major version resolution by querying SDKMAN API
        if (version.matches("\\d+")) {
            String latestVersion = getLatestVersionFromSdkman(version, vendor);
            return latestVersion + suffix;
        }

        // For specific versions, append the vendor suffix
        return version + suffix;
    }

    /**
     * Gets the SDKMAN vendor suffix for the given vendor.
     */
    private String getSdkmanVendorSuffix(String vendor) {
        switch (vendor) {
            case "temurin":
                return "-tem";
            case "corretto":
                return "-amzn";
            case "zulu":
                return "-zulu";
            case "liberica":
                return "-librca";
            case "oracle":
                return "-oracle";
            case "microsoft":
                return "-ms";
            case "semeru":
                return "-sem";
            case "graalvm":
                return "-grl";
            default:
                return "-tem"; // Default to Temurin
        }
    }

    /**
     * Resolves JDK metadata using SDKMAN API.
     */
    private JdkMetadata resolveJdkFromSdkman(String sdkmanVersion, String platform) throws IOException {
        if ("exotic".equals(platform)) {
            throw new IOException("Unsupported platform: " + platform
                    + ". SDKMAN JDK resolution is not available for this platform.");
        }

        // Build SDKMAN download URL
        String sdkmanApiUrl = "https://api.sdkman.io/2/broker/download/java/" + sdkmanVersion + "/" + platform;

        try {
            // Make HTTP request to SDKMAN API to get the actual download URL
            String actualDownloadUrl = makeHttpRequest(sdkmanApiUrl);

            if (actualDownloadUrl == null || actualDownloadUrl.trim().isEmpty()) {
                throw new IOException(
                        "SDKMAN API returned empty download URL for " + sdkmanVersion + " on " + platform);
            }

            // Extract version and vendor from sdkmanVersion
            String[] parts = sdkmanVersion.split("-");
            String version = parts[0];
            String vendor = parts.length > 1 ? mapSdkmanSuffixToVendor(parts[1]) : "temurin";

            // SDKMAN doesn't provide checksums in the download API, so we return null for checksum
            return new JdkMetadata(URI.create(actualDownloadUrl.trim()), null, version, vendor);

        } catch (Exception e) {
            throw new IOException("Failed to resolve JDK from SDKMAN API: " + e.getMessage(), e);
        }
    }

    /**
     * Makes an HTTP GET request to SDKMAN API.
     * For download URLs: returns the redirect location (302 redirect).
     * For version lists: returns the response body content.
     */
    private String makeHttpRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(30000); // 30 seconds
            connection.setInstanceFollowRedirects(false); // Don't follow redirects automatically

            // Set User-Agent to identify as Maven Wrapper
            connection.setRequestProperty("User-Agent", "Maven-Wrapper/3.3.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {

                // Get the redirect location (for download URLs)
                String location = connection.getHeaderField("Location");
                if (location != null && !location.trim().isEmpty()) {
                    return location.trim();
                } else {
                    throw new IOException("SDKMAN API returned redirect without location header");
                }
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response body (for version lists and direct responses)
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString().trim();
                }
            } else {
                throw new IOException(
                        "SDKMAN API request failed with response code: " + responseCode + " for URL: " + urlString);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Maps SDKMAN vendor suffix back to vendor name.
     */
    private String mapSdkmanSuffixToVendor(String suffix) {
        switch (suffix) {
            case "tem":
                return "temurin";
            case "amzn":
                return "corretto";
            case "zulu":
                return "zulu";
            case "librca":
                return "liberica";
            case "oracle":
                return "oracle";
            case "ms":
                return "microsoft";
            case "sem":
                return "semeru";
            case "grl":
                return "graalvm";
            default:
                return "temurin";
        }
    }

    /**
     * Gets the latest version for a major version and vendor by querying SDKMAN API with caching.
     */
    private String getLatestVersionFromSdkman(String majorVersion, String vendor) throws IOException {
        // Check cache first
        String cachedVersion = versionCache.getCachedVersion(majorVersion, vendor);
        if (cachedVersion != null) {
            Logger.info("Using cached JDK version: " + majorVersion + " -> " + cachedVersion);
            return cachedVersion;
        }

        String platform = detectSdkmanPlatform();
        if ("exotic".equals(platform)) {
            throw new IOException(
                    "Unsupported platform for JDK resolution. " + "SDKMAN API is not available for this platform. "
                            + "Please specify an exact JDK version with jdkDistributionUrl instead of using major version resolution.");
        }

        try {
            // Get all available versions for the platform
            String versionsApiUrl = "https://api.sdkman.io/2/candidates/java/" + platform + "/versions/all";
            String allVersions = makeHttpRequest(versionsApiUrl);

            if (allVersions == null || allVersions.trim().isEmpty()) {
                throw new IOException(
                        "SDKMAN API returned empty version list. "
                                + "Please check your internet connection or specify an exact JDK version with jdkDistributionUrl.");
            }

            // Parse versions and find the latest for the major version and vendor
            String vendorSuffix = getSdkmanVendorSuffix(vendor);
            String latestVersion = findLatestVersionForMajor(allVersions, majorVersion, vendorSuffix);

            if (latestVersion != null) {
                // Cache the resolved version
                versionCache.cacheVersion(majorVersion, vendor, latestVersion);
                Logger.info("Resolved JDK version from SDKMAN: " + majorVersion + " -> " + latestVersion);
                return latestVersion;
            }

            throw new IOException("No JDK version found for " + majorVersion + " with vendor " + vendor + ". "
                    + "Available vendors: temurin, corretto, zulu, liberica, oracle, microsoft, semeru, graalvm");

        } catch (IOException e) {
            throw e; // Re-throw IOException as-is
        } catch (Exception e) {
            throw new IOException(
                    "Failed to resolve JDK version from SDKMAN API: " + e.getMessage() + ". "
                            + "Please check your internet connection or specify an exact JDK version with jdkDistributionUrl.",
                    e);
        }
    }

    /**
     * Parses SDKMAN versions response and finds the latest version for major version and vendor.
     */
    private String findLatestVersionForMajor(String allVersions, String majorVersion, String vendorSuffix) {
        String[] versions = allVersions.split(",");
        String latestVersion = null;

        for (String version : versions) {
            version = version.trim();
            if (version.endsWith(vendorSuffix) && version.startsWith(majorVersion + ".")) {
                // Remove vendor suffix to get just the version
                String versionOnly = version.substring(0, version.length() - vendorSuffix.length());

                // Simple version comparison - take the first match as SDKMAN usually returns them in order
                if (latestVersion == null) {
                    latestVersion = versionOnly;
                }
            }
        }

        return latestVersion;
    }
}
