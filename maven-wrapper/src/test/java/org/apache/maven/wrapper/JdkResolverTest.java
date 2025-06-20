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
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for JdkResolver functionality.
 */
class JdkResolverTest {

    /**
     * Checks if SDKMAN API is available for network-dependent tests.
     * This prevents test failures in CI environments where the API might be unavailable.
     */
    static boolean isSdkmanApiAvailable() {
        try {
            URL url = new URL("https://api.sdkman.io/2/candidates/java/linuxx64/versions/all");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000); // 5 second timeout
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Maven-Wrapper-Test/3.3.0");

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            // Consider API available if we get any response (200, 302, etc.) but not 503/502/504
            return responseCode != 503 && responseCode != 502 && responseCode != 504;
        } catch (Exception e) {
            // API is not available
            return false;
        }
    }

    @Test
    @EnabledIf("isSdkmanApiAvailable")
    void testResolveTemurinJdk() throws IOException {
        JdkResolver resolver = new JdkResolver();

        JdkResolver.JdkMetadata metadata = resolver.resolveJdk("17", "temurin");

        assertNotNull(metadata);
        assertEquals("temurin", metadata.getVendor());
        assertNotNull(metadata.getDownloadUrl());
        assertTrue(metadata.getDownloadUrl().toString().contains("adoptium"));
        assertTrue(metadata.getDownloadUrl().toString().contains("17"));
    }

    @Test
    @EnabledIf("isSdkmanApiAvailable")
    void testResolveJdkWithDefaultVendor() throws IOException {
        JdkResolver resolver = new JdkResolver();

        JdkResolver.JdkMetadata metadata = resolver.resolveJdk("21");

        assertNotNull(metadata);
        assertEquals("temurin", metadata.getVendor());
        assertNotNull(metadata.getDownloadUrl());
    }

    @Test
    void testResolveJdkWithInvalidVersion() {
        JdkResolver resolver = new JdkResolver();

        assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolveJdk(null, "temurin");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolveJdk("", "temurin");
        });
    }

    @Test
    @EnabledIf("isSdkmanApiAvailable")
    void testResolveJdkWithUnsupportedVendor() {
        JdkResolver resolver = new JdkResolver();

        // Unsupported vendors are normalized to known vendors, so this should not throw
        // Instead, test that it resolves to a default vendor (temurin)
        assertDoesNotThrow(() -> {
            JdkResolver.JdkMetadata metadata = resolver.resolveJdk("17", "unsupported-vendor");
            // The vendor should be normalized to temurin (the default)
            assertEquals("temurin", metadata.getVendor());
        });
    }

    @Test
    @EnabledIf("isSdkmanApiAvailable")
    void testResolveMajorVersionQueriesApi() throws IOException {
        JdkResolver resolver = new JdkResolver();

        // Test that major version resolution actually queries SDKMAN API
        // This test makes a real API call but is conditional on API availability
        // to prevent flaky failures in CI environments
        JdkResolver.JdkMetadata metadata = resolver.resolveJdk("17", "temurin");

        assertNotNull(metadata);
        assertEquals("temurin", metadata.getVendor());
        assertNotNull(metadata.getDownloadUrl());

        // The version should be a specific version, not just "17"
        assertFalse("17".equals(metadata.getVersion()));
        assertTrue(metadata.getVersion().startsWith("17."));

        // The download URL should be a real GitHub URL for Temurin
        assertTrue(metadata.getDownloadUrl().toString().contains("github.com"));
        assertTrue(metadata.getDownloadUrl().toString().contains("adoptium"));
    }
}
