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
import java.io.BufferedWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloaderTest {

    private DefaultDownloader download;

    private Path testDir;

    private Path downloadFile;

    private Path rootDir;

    private URI sourceRoot;

    private Path remoteFile;

    @BeforeEach
    void setUp() throws Exception {
        download = new DefaultDownloader("mvnw", "aVersion");
        testDir = Paths.get("target/test-files/DownloadTest");
        Files.createDirectories(testDir);
        rootDir = testDir.resolve("root");
        downloadFile = rootDir.resolve("file");
        Files.deleteIfExists(downloadFile);
        remoteFile = testDir.resolve("remoteFile");
        try (BufferedWriter writer = Files.newBufferedWriter(remoteFile, StandardCharsets.UTF_8)) {
            writer.write("sometext");
        }
        sourceRoot = remoteFile.toUri();
    }

    @Test
    void testDownload() throws Exception {
        assertTrue(Files.notExists(downloadFile));
        download.download(sourceRoot, downloadFile);
        assertTrue(Files.exists(downloadFile));
        try (BufferedReader reader = Files.newBufferedReader(downloadFile, StandardCharsets.UTF_8)) {
            assertEquals("sometext", reader.readLine());
        }
    }
}
