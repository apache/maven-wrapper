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

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WrapperMojoTest {
    @Mock(strictness = Mock.Strictness.LENIENT)
    private RepositorySystem repositorySystem;

    @Mock
    private RepositorySystemSession repositorySystemSession;

    @InjectMocks
    private WrapperMojo wrapperMojo;

    @BeforeEach
    void setupMocks() {
        when(repositorySystem.newResolutionRepositories(same(repositorySystemSession), anyList()))
                .then(i -> i.getArguments()[1]);
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
