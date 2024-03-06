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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WrapperMojoTest {

    @Test
    void user_supplied_repo_url_gets_trailing_slash_trimmed() {
        // given
        Settings settings = new Settings();
        WrapperMojo wrapperMojo = new WrapperMojo();

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl(WrapperMojo.DEFAULT_REPOURL + "/", settings);

        // then
        Assertions.assertEquals(WrapperMojo.DEFAULT_REPOURL, determinedRepoUrl);
    }

    @Test
    void null_repo_url_not_used() {
        // given
        Settings settings = new Settings();
        WrapperMojo wrapperMojo = new WrapperMojo();

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl(null, settings);

        // then
        Assertions.assertEquals(WrapperMojo.DEFAULT_REPOURL, determinedRepoUrl);
    }

    @Test
    void empty_repo_url_not_used() {
        // given
        Settings settings = new Settings();
        WrapperMojo wrapperMojo = new WrapperMojo();

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl("", settings);

        // then
        Assertions.assertEquals(WrapperMojo.DEFAULT_REPOURL, determinedRepoUrl);
    }

    @Test
    void slash_repo_url_not_used() {
        // given
        Settings settings = new Settings();
        WrapperMojo wrapperMojo = new WrapperMojo();

        // when
        String determinedRepoUrl = wrapperMojo.determineRepoUrl("/", settings);

        // then
        Assertions.assertEquals(WrapperMojo.DEFAULT_REPOURL, determinedRepoUrl);
    }
}
