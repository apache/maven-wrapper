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

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class HashAlgorithmVerifierTest {

    @TempDir
    private File temporaryFolder;

    private Verifier verifier = new HashAlgorithmVerifier();

    private Path file;

    @BeforeEach
    void setUp() throws Exception {
        file = File.createTempFile("junit", null, temporaryFolder).toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Sample file with content");
        }
    }

    @Test
    void sha256SumsMatch() throws Exception {
        verifier.verify(
                file,
                "property",
                Verifier.SHA_256_ALGORITHM,
                "7e0c63c6a99639e57cc64375d6717d72e301d8ab829fef2e145ee860317bc3cb");
    }

    @Test
    void sha256SumsDoNotMatch() throws Exception {
        try {
            verifier.verify(
                    file,
                    "prop",
                    Verifier.SHA_256_ALGORITHM,
                    "d3b572c45972921782287d8edafa5b19533212f2ebbc61c13c375a67c8f2c48f");
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals(
                    "Failed to validate Maven distribution SHA-256, your Maven "
                            + "distribution might be compromised. If you updated your Maven version, "
                            + "you need to update the specified prop property.",
                    e.getMessage());
        }
    }
}
