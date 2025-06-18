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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

/**
 * Caches JDK version resolution results with Maven-style update policies.
 * Supports update policies: never, daily, always, interval:X
 */
class JdkVersionCache {
    private static final int HOURS_PER_DAY = 24;

    private final Path cacheDir;
    private final String updatePolicy;

    JdkVersionCache(Path cacheDir, String updatePolicy) {
        this.cacheDir = cacheDir;
        this.updatePolicy = updatePolicy != null ? updatePolicy : "daily";
    }

    /**
     * Gets cached version resolution if available and not expired.
     *
     * @param majorVersion the major version (e.g., "17")
     * @param vendor the vendor (e.g., "temurin")
     * @return cached resolved version or null if not cached or expired
     */
    String getCachedVersion(String majorVersion, String vendor) {
        if ("always".equals(updatePolicy)) {
            return null; // Never use cache
        }

        try {
            Path cacheFile = getCacheFile(majorVersion, vendor);
            if (!Files.exists(cacheFile)) {
                return null;
            }

            Properties props = new Properties();
            props.load(Files.newBufferedReader(cacheFile));

            String cachedVersion = props.getProperty("version");
            String timestampStr = props.getProperty("timestamp");

            if (cachedVersion == null || timestampStr == null) {
                return null;
            }

            // Check if cache is expired
            if (isCacheExpired(timestampStr)) {
                return null;
            }

            return cachedVersion;

        } catch (Exception e) {
            Logger.warn("Failed to read JDK version cache: " + e.getMessage());
            return null;
        }
    }

    /**
     * Caches a version resolution result.
     *
     * @param majorVersion the major version
     * @param vendor the vendor
     * @param resolvedVersion the resolved specific version
     */
    void cacheVersion(String majorVersion, String vendor, String resolvedVersion) {
        if ("never".equals(updatePolicy)) {
            return; // Don't cache if policy is never
        }

        try {
            Files.createDirectories(cacheDir);

            Path cacheFile = getCacheFile(majorVersion, vendor);
            Properties props = new Properties();
            props.setProperty("version", resolvedVersion);
            props.setProperty("timestamp", String.valueOf(Instant.now().toEpochMilli()));
            props.setProperty("updatePolicy", updatePolicy);

            props.store(
                    Files.newBufferedWriter(cacheFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                    "JDK version cache for " + majorVersion + " " + vendor);

        } catch (Exception e) {
            Logger.warn("Failed to cache JDK version: " + e.getMessage());
        }
    }

    /**
     * Checks if the cache entry is expired based on the update policy.
     */
    private boolean isCacheExpired(String timestampStr) {
        if ("never".equals(updatePolicy)) {
            return false; // Never expires
        }

        try {
            long timestamp = Long.parseLong(timestampStr);
            Instant cacheTime = Instant.ofEpochMilli(timestamp);
            Instant now = Instant.now();

            if ("daily".equals(updatePolicy)) {
                return ChronoUnit.HOURS.between(cacheTime, now) >= HOURS_PER_DAY;
            } else if (updatePolicy.startsWith("interval:")) {
                String intervalStr = updatePolicy.substring("interval:".length());
                int intervalMinutes = Integer.parseInt(intervalStr);
                return ChronoUnit.MINUTES.between(cacheTime, now) >= intervalMinutes;
            }

            return false; // Unknown policy, don't expire

        } catch (Exception e) {
            Logger.warn("Failed to parse cache timestamp: " + e.getMessage());
            return true; // Expire on error
        }
    }

    /**
     * Gets the cache file path for a specific major version and vendor.
     */
    private Path getCacheFile(String majorVersion, String vendor) {
        String fileName = "jdk-" + majorVersion + "-" + vendor + ".properties";
        return cacheDir.resolve(fileName);
    }

    /**
     * Clears all cached version resolutions.
     */
    void clearCache() {
        try {
            if (Files.exists(cacheDir)) {
                Files.walk(cacheDir)
                        .filter(path -> path.getFileName().toString().startsWith("jdk-")
                                && path.getFileName().toString().endsWith(".properties"))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                Logger.warn("Failed to delete cache file: " + path);
                            }
                        });
            }
        } catch (Exception e) {
            Logger.warn("Failed to clear JDK version cache: " + e.getMessage());
        }
    }

    /**
     * Validates the update policy format.
     */
    static boolean isValidUpdatePolicy(String policy) {
        if (policy == null) {
            return false;
        }

        switch (policy) {
            case "never":
            case "daily":
            case "always":
                return true;
            default:
                if (policy.startsWith("interval:")) {
                    try {
                        String intervalStr = policy.substring("interval:".length());
                        int interval = Integer.parseInt(intervalStr);
                        return interval > 0;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                return false;
        }
    }
}
