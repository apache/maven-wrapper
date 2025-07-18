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

import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WrapperMojoTest {

    @Test
    void userSuppliedRepoUrlGetsTrailingSlashTrimmed() {
        // given
        Settings settings = new Settings();
        WrapperMojo wrapperMojo = new WrapperMojo();

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl(WrapperMojo.DEFAULT_REPOURL + "/", settings);

        // then
        assertEquals(WrapperMojo.DEFAULT_REPOURL, determinedRepoUrl);
    }

    @Test
    void nullRepoUrlNotUsed() {
        // given
        Settings settings = new Settings();
        WrapperMojo wrapperMojo = new WrapperMojo();

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl(null, settings);

        // then
        assertEquals(WrapperMojo.DEFAULT_REPOURL, determinedRepoUrl);
    }

    @Test
    void emptyRepoUrlNotUsed() {
        // given
        Settings settings = new Settings();
        WrapperMojo wrapperMojo = new WrapperMojo();

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl("", settings);

        // then
        assertEquals(WrapperMojo.DEFAULT_REPOURL, determinedRepoUrl);
    }

    @Test
    void slashRepoUrlNotUsed() {
        // given
        Settings settings = new Settings();
        WrapperMojo wrapperMojo = new WrapperMojo();

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl("/", settings);

        // then
        assertEquals(WrapperMojo.DEFAULT_REPOURL, determinedRepoUrl);
    }
}
