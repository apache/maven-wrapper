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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WrapperMojoTest {
    private final RepositorySystem mockRepositorySystem = mock(RepositorySystem.class);
    private final RepositorySystemSession mockSession = mock(RepositorySystemSession.class);

    WrapperMojoTest() {
        when(mockRepositorySystem.newResolutionRepositories(any(RepositorySystemSession.class), anyList()))
                .then(i -> i.getArguments()[1]);
    }

    @Test
    void userSuppliedRepoUrlGetsTrailingSlashTrimmed() {
        // given
        WrapperMojo wrapperMojo = new WrapperMojo(mockRepositorySystem, mockSession);

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl(WrapperMojo.DEFAULT_REPO_URL + "/");

        // then
        assertEquals(WrapperMojo.DEFAULT_REPO_URL, determinedRepoUrl);
    }

    @Test
    void nullRepoUrlNotUsed() {
        // given
        WrapperMojo wrapperMojo = new WrapperMojo(mockRepositorySystem, mockSession);

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl(null);

        // then
        assertEquals(WrapperMojo.DEFAULT_REPO_URL, determinedRepoUrl);
    }

    @Test
    void emptyRepoUrlNotUsed() {
        // given
        WrapperMojo wrapperMojo = new WrapperMojo(mockRepositorySystem, mockSession);

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl("");

        // then
        assertEquals(WrapperMojo.DEFAULT_REPO_URL, determinedRepoUrl);
    }

    @Test
    void slashRepoUrlNotUsed() {
        // given
        WrapperMojo wrapperMojo = new WrapperMojo(mockRepositorySystem, mockSession);

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl("/");

        // then
        assertEquals(WrapperMojo.DEFAULT_REPO_URL, determinedRepoUrl);
    }
}
