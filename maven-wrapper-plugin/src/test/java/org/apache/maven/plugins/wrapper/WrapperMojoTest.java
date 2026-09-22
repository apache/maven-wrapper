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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WrapperMojoTest {
    @Mock(strictness = Mock.Strictness.LENIENT)
    private RepositorySystem repositorySystem;

    @Mock
    private RepositorySystemSession repositorySystemSession;

    @Mock
    private MavenProject project;

    @Mock
    private Version version1;

    @Mock
    private Version version2;

    @Mock
    private MavenSession session;

    @Captor
    private ArgumentCaptor<VersionRangeRequest> requestCaptor;

    @InjectMocks
    private WrapperMojo wrapperMojo;

    @BeforeEach
    void setupMocks() {
        when(repositorySystem.newResolutionRepositories(same(repositorySystemSession), anyList()))
                .then(i -> i.getArguments()[1]);
    }

    @Test
    void resolveMavenVersion() throws VersionRangeResolutionException {
        String versionRange = "versionRange";
        String resolvedVersion1 = "resolvedVersion1-SNAPSHOT";
        String resolvedVersion2 = "resolvedVersion2";
        List<RemoteRepository> remoteRepositories = Collections.emptyList();
        VersionRangeResult result =
                new VersionRangeResult(new VersionRangeRequest()).setVersions(Arrays.asList(version1, version2));

        when(repositorySystem.resolveVersionRange(eq(repositorySystemSession), requestCaptor.capture()))
                .thenReturn(result);
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getRemotePluginRepositories()).thenReturn(remoteRepositories);
        when(version1.toString()).thenReturn(resolvedVersion1);
        when(version2.toString()).thenReturn(resolvedVersion2);

        assertEquals(resolvedVersion2, wrapperMojo.resolveMavenVersion(versionRange));

        VersionRangeRequest capturedRequest = requestCaptor.getValue();
        assertEquals(remoteRepositories, capturedRequest.getRepositories());
        assertEquals("wrapper", capturedRequest.getRequestContext());

        Artifact artifact = capturedRequest.getArtifact();
        assertEquals("org.apache.maven", artifact.getGroupId());
        assertEquals("apache-maven", artifact.getArtifactId());
        assertEquals(versionRange, artifact.getVersion());
    }

    @Test
    void resolveMavenVersionException() throws VersionRangeResolutionException {
        String versionRange = "versionRange";
        List<RemoteRepository> remoteRepositories = Collections.emptyList();

        when(repositorySystem.resolveVersionRange(eq(repositorySystemSession), requestCaptor.capture()))
                .thenThrow(VersionRangeResolutionException.class);
        when(session.getCurrentProject()).thenReturn(project);
        when(project.getRemotePluginRepositories()).thenReturn(remoteRepositories);

        assertEquals(versionRange, wrapperMojo.resolveMavenVersion(versionRange));

        VersionRangeRequest capturedRequest = requestCaptor.getValue();
        assertEquals(remoteRepositories, capturedRequest.getRepositories());
        assertEquals("wrapper", capturedRequest.getRequestContext());

        Artifact artifact = capturedRequest.getArtifact();
        assertEquals("org.apache.maven", artifact.getGroupId());
        assertEquals("apache-maven", artifact.getArtifactId());
        assertEquals(versionRange, artifact.getVersion());
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
