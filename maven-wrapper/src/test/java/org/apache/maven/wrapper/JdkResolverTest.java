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

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for JdkResolver functionality.
 */
class JdkResolverTest {

    @Test
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
    void testResolveJdkWithUnsupportedVendor() {
        JdkResolver resolver = new JdkResolver();

        assertThrows(IOException.class, () -> {
            resolver.resolveJdk("17", "unsupported-vendor");
        });
    }

    @Test
    void testResolveMajorVersionQueriesApi() throws IOException {
        JdkResolver resolver = new JdkResolver();

        // Test that major version resolution actually queries SDKMAN API
        // This test will make a real API call - in a production test suite,
        // you might want to mock this
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
