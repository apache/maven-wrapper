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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class SystemPropertiesHandlerTest {

    private Path tmpDir = Paths.get("target/test-files/SystemPropertiesHandlerTest");

    @BeforeEach
    void setupTempDir() throws IOException {
        Files.createDirectories(tmpDir);
    }

    @Test
    void testParsePropertiesFile() throws Exception {
        Path propFile = tmpDir.resolve("props");
        Properties props = new Properties();
        props.put("a", "b");
        props.put("systemProp.c", "d");
        props.put("systemProp.", "e");

        try (OutputStream fos = Files.newOutputStream(propFile)) {
            props.store(fos, "");
        }

        Map<String, String> expected = new HashMap<>();
        expected.put("c", "d");

        assertThat(SystemPropertiesHandler.getSystemProperties(propFile), equalTo(expected));
    }

    @Test
    void ifNoPropertyFileExistShouldReturnEmptyMap() {
        Map<String, String> expected = new HashMap<>();
        assertThat(SystemPropertiesHandler.getSystemProperties(tmpDir.resolve("unknown")), equalTo(expected));
    }
}
