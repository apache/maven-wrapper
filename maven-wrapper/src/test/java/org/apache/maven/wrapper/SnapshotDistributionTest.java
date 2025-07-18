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
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for snapshot distribution handling.
 * This test verifies that the wrapper can handle cases where the distribution
 * filename contains timestamp information but the extracted directory uses
 * the SNAPSHOT version.
 */
class SnapshotDistributionTest {

    @TempDir
    Path tempDir;

    private Path propertiesFile;

    @BeforeEach
    void setUp() throws IOException {
        Path wrapperDir = tempDir.resolve(".mvn").resolve("wrapper");
        Files.createDirectories(wrapperDir);
        propertiesFile = wrapperDir.resolve("maven-wrapper.properties");
    }

    @Test
    void testSnapshotUrlParsing() throws Exception {
        // Test that PathAssembler can handle snapshot URLs
        String snapshotUrl =
                "https://repository.apache.org/content/repositories/snapshots/org/apache/maven/apache-maven/4.1.0-SNAPSHOT/apache-maven-4.1.0-20250710.120440-1-bin.zip";

        PathAssembler pathAssembler = new PathAssembler(tempDir.resolve(".m2"));
        WrapperConfiguration config = new WrapperConfiguration();
        config.setDistribution(new URI(snapshotUrl));

        PathAssembler.LocalDistribution localDist = pathAssembler.getDistribution(config);

        // Verify that the path is created correctly
        assertTrue(localDist.getDistributionDir().toString().contains("apache-maven-4.1.0-20250710.120440-1"));
        assertTrue(localDist.getZipFile().toString().contains("apache-maven-4.1.0-20250710.120440-1-bin.zip"));
    }

    @Test
    void testWrapperExecutorWithSnapshotUrl() throws Exception {
        // Create properties file with snapshot URL
        String snapshotUrl =
                "https://repository.apache.org/content/repositories/snapshots/org/apache/maven/apache-maven/4.1.0-SNAPSHOT/apache-maven-4.1.0-20250710.120440-1-bin.zip";

        Properties props = new Properties();
        props.setProperty("distributionUrl", snapshotUrl);
        props.setProperty("distributionBase", "MAVEN_USER_HOME");
        props.setProperty("distributionPath", "wrapper/dists");
        props.setProperty("zipStoreBase", "MAVEN_USER_HOME");
        props.setProperty("zipStorePath", "wrapper/dists");

        try (OutputStream out = Files.newOutputStream(propertiesFile)) {
            props.store(out, "Test properties");
        }

        WrapperExecutor executor = WrapperExecutor.forWrapperPropertiesFile(propertiesFile);

        // Verify that the distribution URL is parsed correctly
        assertEquals(new URI(snapshotUrl), executor.getDistribution());
        assertEquals(new URI(snapshotUrl), executor.getConfiguration().getDistribution());
    }

    @Test
    void testRegularReleaseUrlStillWorks() throws Exception {
        // Ensure regular releases still work as before
        String releaseUrl =
                "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip";

        PathAssembler pathAssembler = new PathAssembler(tempDir.resolve(".m2"));
        WrapperConfiguration config = new WrapperConfiguration();
        config.setDistribution(new URI(releaseUrl));

        PathAssembler.LocalDistribution localDist = pathAssembler.getDistribution(config);

        // Verify that regular releases work as before
        assertTrue(localDist.getDistributionDir().toString().contains("apache-maven-3.9.9"));
        assertTrue(localDist.getZipFile().toString().contains("apache-maven-3.9.9-bin.zip"));
    }
}
