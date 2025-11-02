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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Hans Dockter
 */
public class PathAssemblerTest {
    public static final String TEST_MAVEN_USER_HOME = "someUserHome";

    private PathAssembler pathAssembler = new PathAssembler(Paths.get(TEST_MAVEN_USER_HOME));

    final WrapperConfiguration configuration = new WrapperConfiguration();

    @BeforeEach
    void setup() {
        configuration.setDistributionBase(PathAssembler.MAVEN_USER_HOME_STRING);
        configuration.setDistributionPath(Paths.get("somePath"));
        configuration.setZipBase(PathAssembler.MAVEN_USER_HOME_STRING);
        configuration.setZipPath(Paths.get("somePath"));
    }

    @Test
    void distributionDirWithMavenUserHomeBase() throws Exception {
        configuration.setDistribution(new URI("http://server/dist/maven-0.9-bin.zip"));

        Path distributionDir = pathAssembler.getDistribution(configuration).getDistributionDir();
        assertThat(distributionDir.getFileName().toString(), matchesRegexp("[a-z0-9]+"));
        assertEquals(distributionDir.getParent(), file(TEST_MAVEN_USER_HOME + "/somePath/maven-0.9-bin"));
    }

    @Test
    void distributionDirWithProjectBase() throws Exception {
        configuration.setDistributionBase(PathAssembler.PROJECT_STRING);
        configuration.setDistribution(new URI("http://server/dist/maven-0.9-bin.zip"));

        Path distributionDir = pathAssembler.getDistribution(configuration).getDistributionDir();
        assertThat(distributionDir.getFileName().toString(), matchesRegexp("[a-z0-9]+"));
        assertEquals(distributionDir.getParent(), file(currentDirPath() + "/somePath/maven-0.9-bin"));
    }

    @Test
    void distributionDirWithUnknownBase() throws Exception {
        configuration.setDistribution(new URI("http://server/dist/maven-1.0.zip"));
        configuration.setDistributionBase("unknownBase");

        try {
            pathAssembler.getDistribution(configuration);
            fail();
        } catch (RuntimeException e) {
            assertEquals("Base: unknownBase is unknown", e.getMessage());
        }
    }

    @Test
    void distZipWithMavenUserHomeBase() throws Exception {
        configuration.setDistribution(new URI("http://server/dist/maven-1.0.zip"));

        Path dist = pathAssembler.getDistribution(configuration).getZipFile();
        assertEquals("maven-1.0.zip", dist.getFileName().toString());
        assertThat(dist.getParent().getFileName().toString(), matchesRegexp("[a-z0-9]+"));
        assertEquals(dist.getParent().getParent(), file(TEST_MAVEN_USER_HOME + "/somePath/maven-1.0"));
    }

    @Test
    void distZipWithProjectBase() throws Exception {
        configuration.setZipBase(PathAssembler.PROJECT_STRING);
        configuration.setDistribution(new URI("http://server/dist/maven-1.0.zip"));

        Path dist = pathAssembler.getDistribution(configuration).getZipFile();
        assertEquals("maven-1.0.zip", dist.getFileName().toString());
        assertThat(dist.getParent().getFileName().toString(), matchesRegexp("[a-z0-9]+"));
        assertEquals(dist.getParent().getParent(), file(currentDirPath() + "/somePath/maven-1.0"));
    }

    @Test
    void distZipWithLocalWindowsPath() throws Exception {
        configuration.setZipBase(PathAssembler.PROJECT_STRING);
        configuration.setDistribution(new URI("file:///C:/maven-1.0.zip"));

        Path dist = pathAssembler.getDistribution(configuration).getZipFile();
        assertEquals("maven-1.0.zip", dist.getFileName().toString());
        assertThat(dist.getParent().getFileName().toString(), matchesRegexp("[a-z0-9]+"));
        assertEquals(dist.getParent().getParent(), file(currentDirPath() + "/somePath/maven-1.0"));
    }

    private Path file(String path) {
        return Paths.get(path);
    }

    private String currentDirPath() {
        return System.getProperty("user.dir");
    }

    public static <T extends CharSequence> Matcher<T> matchesRegexp(final String pattern) {
        return new BaseMatcher<T>() {
            @Override
            public boolean matches(Object o) {
                return Pattern.compile(pattern).matcher((CharSequence) o).matches();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a CharSequence that matches regexp ").appendValue(pattern);
            }
        };
    }
}
