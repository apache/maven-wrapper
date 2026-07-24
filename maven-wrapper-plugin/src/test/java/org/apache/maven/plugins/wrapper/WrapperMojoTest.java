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
package org.apache.maven.plugins.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WrapperMojoTest {

    private final String repoUrl = "repoUrl";
    private final String metadataXmlUrl = "/org/apache/maven/apache-maven/maven-metadata.xml";
    private final String version = "version";

    @Mock(strictness = Mock.Strictness.LENIENT)
    private RepositorySystem repositorySystem;

    @Mock
    private RepositorySystemSession repositorySystemSession;

    @Mock
    private VersionRange versionRange;

    @Mock
    private ArtifactVersion artifactVersion;

    @Mock
    private URI uri;

    @Mock
    private URL url;

    @Mock
    private InputStream inputStream;

    @Mock
    private Metadata metadata;

    @Mock
    private Versioning versioning;

    @InjectMocks
    private WrapperMojo wrapperMojo;

    @BeforeEach
    void setupMocks() {
        when(repositorySystem.newResolutionRepositories(same(repositorySystemSession), anyList()))
                .then(i -> i.getArguments()[1]);
    }

    @Test
    void resolveVersionRangeRecommendedVersion() throws InvalidVersionSpecificationException, MojoExecutionException {
        try (MockedStatic<VersionRange> ignored = Mockito.mockStatic()) {
            when(VersionRange.createFromVersionSpec(version)).thenReturn(versionRange);
            when(versionRange.getRecommendedVersion()).thenReturn(artifactVersion);

            assertEquals(version, wrapperMojo.resolveVersionRange(version, repoUrl));
        }
    }

    @ParameterizedTest
    @MethodSource("resolveVersionRangeArguments")
    void resolveVersionRange(String version, List<String> metadataVersions, String expectedVersion)
            throws IOException, MojoExecutionException {
        try (MockedStatic<URI> ignored2 = Mockito.mockStatic()) {
            when(URI.create(repoUrl + metadataXmlUrl)).thenReturn(uri);
            when(uri.toURL()).thenReturn(url);
            when(url.openStream()).thenReturn(inputStream);

            try (MockedConstruction<MetadataXpp3Reader> ignored3 = mockConstruction(
                    (mock, context) -> when(mock.read(inputStream)).thenReturn(metadata))) {
                when(metadata.getVersioning()).thenReturn(versioning);
                when(versioning.getVersions()).thenReturn(metadataVersions);

                assertEquals(expectedVersion, wrapperMojo.resolveVersionRange(version, repoUrl));
            }
        }
    }

    private static Stream<Arguments> resolveVersionRangeArguments() {
        return Stream.of(
                arguments("[1.0,2.0]", Arrays.asList("1.0.0", "1.2.3"), "1.2.3"),
                arguments("[1.0,2.0]", Arrays.asList("1.0.0", "2.0.0"), "2.0.0"),
                arguments("[1.0,2.0]", Arrays.asList("1.0.0", "3.0.0"), "1.0.0"),
                arguments("[1.0,2.0-rc)", Arrays.asList("1.0.0", "2.0.0-rc-1"), "1.0.0"),
                arguments("[1.0,2.0)", Arrays.asList("1.0.0", "2.0.0-rc-1"), "2.0.0-rc-1"),
                arguments("[1.0,2.0]", Arrays.asList("1.0.0", "1.2.3"), "1.2.3"),
                arguments("[1.0,2.0]", Arrays.asList("1.0.0", "1.2.3-rc-1", "1.2.3-rc-2"), "1.2.3-rc-2"),
                arguments("[1.0,2.0]", Arrays.asList("1.0.0", "1.2.3-alpha-1", "1.2.3-rc-1"), "1.2.3-rc-1"),
                arguments("[1.0,2.0]", Arrays.asList("1.0.0", "1.2.3-rc-1", "2.0.0"), "2.0.0"),
                arguments("[1.0,2.0]", Arrays.asList("1.2.3-rc-1", "2.0.0-alpha-1"), "2.0.0-alpha-1"),
                arguments("[1.0,2.0]", Arrays.asList("1.0.0", "1.2.3-rc-1", "3.0.0"), "1.2.3-rc-1"));
    }

    @Test
    void resolveVersionRangeInvalidVersion() throws InvalidVersionSpecificationException {
        try (MockedStatic<VersionRange> ignored = Mockito.mockStatic()) {
            when(VersionRange.createFromVersionSpec(version)).thenThrow(InvalidVersionSpecificationException.class);

            Exception e =
                    assertThrows(MojoExecutionException.class, () -> wrapperMojo.resolveVersionRange(version, repoUrl));
            assertEquals("Invalid version specification: " + version, e.getMessage());
        }
    }

    @Test
    void resolveVersionRangeIOException()
            throws InvalidVersionSpecificationException, IOException, MojoExecutionException {
        try (MockedStatic<VersionRange> ignored = Mockito.mockStatic();
                MockedStatic<URI> ignored2 = Mockito.mockStatic()) {
            when(VersionRange.createFromVersionSpec(version)).thenReturn(versionRange);
            when(versionRange.getRecommendedVersion()).thenReturn(null);

            when(URI.create(repoUrl + metadataXmlUrl)).thenReturn(uri);
            when(uri.toURL()).thenReturn(url);
            when(url.openStream()).thenThrow(IOException.class);

            assertEquals(version, wrapperMojo.resolveVersionRange(version, repoUrl));
        }
    }

    @Test
    void resolveVersionRangeXmlPullParserException()
            throws InvalidVersionSpecificationException, IOException, MojoExecutionException {
        try (MockedStatic<VersionRange> ignored = Mockito.mockStatic();
                MockedStatic<URI> ignored2 = Mockito.mockStatic()) {
            when(VersionRange.createFromVersionSpec(version)).thenReturn(versionRange);
            when(versionRange.getRecommendedVersion()).thenReturn(null);

            when(URI.create(repoUrl + metadataXmlUrl)).thenReturn(uri);
            when(uri.toURL()).thenReturn(url);
            when(url.openStream()).thenReturn(inputStream);

            try (MockedConstruction<MetadataXpp3Reader> ignored3 = mockConstruction(
                    (mock, context) -> when(mock.read(inputStream)).thenThrow(XmlPullParserException.class))) {
                assertEquals(version, wrapperMojo.resolveVersionRange(version, repoUrl));
            }
        }
    }

    @Test
    void userSuppliedRepoUrlGetsTrailingSlashTrimmed() {
        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl(WrapperMojo.DEFAULT_REPO_URL + "/");

        // then
        assertEquals(WrapperMojo.DEFAULT_REPO_URL, determinedRepoUrl);
    }

    @Test
    void nullRepoUrlNotUsed() {
        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl(null);

        // then
        assertEquals(WrapperMojo.DEFAULT_REPO_URL, determinedRepoUrl);
    }

    @Test
    void emptyRepoUrlNotUsed() {
        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl("");

        // then
        assertEquals(WrapperMojo.DEFAULT_REPO_URL, determinedRepoUrl);
    }

    @Test
    void slashRepoUrlNotUsed() {
        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl("/");

        // then
        assertEquals(WrapperMojo.DEFAULT_REPO_URL, determinedRepoUrl);
    }
}
