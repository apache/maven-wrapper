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

/**
 * @author Hans Dockter
 */
public class PathAssembler {
    public static final String MAVEN_USER_HOME_STRING = "MAVEN_USER_HOME";

    public static final String PROJECT_STRING = "PROJECT";

    private Path mavenUserHome;

    public PathAssembler() {}

    public PathAssembler(Path mavenUserHome) {
        this.mavenUserHome = mavenUserHome;
    }

    /**
     * Determines the local locations for the distribution to use given the supplied configuration.
     *
     * @param configuration a wrapper configuration
     * @return the local distribution
     */
    public LocalDistribution getDistribution(WrapperConfiguration configuration) {
        String baseName = getBaseName(configuration.getDistribution());
        String distName = removeExtension(baseName);
        Path rootDirName = rootDirName(distName, configuration);
        Path distDir = getBaseDir(configuration.getDistributionBase())
                .resolve(configuration.getDistributionPath())
                .resolve(rootDirName);
        Path distZip = getBaseDir(configuration.getZipBase())
                .resolve(configuration.getZipPath())
                .resolve(rootDirName)
                .resolve(baseName);
        return new LocalDistribution(distDir, distZip);
    }

    private Path rootDirName(String distName, WrapperConfiguration configuration) {
        String urlHash = getHash(configuration.getDistribution());
        return Paths.get(distName, urlHash);
    }

    private String getHash(URI path) {
        return Integer.toHexString(path.hashCode());
    }

    private String removeExtension(String name) {
        int dot = name.lastIndexOf(".");
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private String getBaseName(URI distUrl) {
        return Paths.get(distUrl.getPath()).getFileName().toString();
    }

    private Path getBaseDir(String base) {
        if (MAVEN_USER_HOME_STRING.equals(base)) {
            return mavenUserHome;
        } else if (PROJECT_STRING.equals(base)) {
            return Paths.get(System.getProperty("user.dir"));
        } else {
            throw new RuntimeException("Base: " + base + " is unknown");
        }
    }

    /**
     * Returns the base directory for this path assembler.
     *
     * @return the base directory
     */
    public Path getBaseDir() {
        return mavenUserHome;
    }

    /**
     * Local distribution
     */
    public static class LocalDistribution {
        private final Path distZip;

        private final Path distDir;

        public LocalDistribution(Path distDir, Path distZip) {
            this.distDir = distDir;
            this.distZip = distZip;
        }

        /**
         * Returns the location to install the distribution into.
         *
         * @return the location to install the distribution into
         */
        public Path getDistributionDir() {
            return distDir;
        }

        /**
         * Returns the location to install the distribution ZIP file to.
         *
         * @return the location to install the distribution ZIP file to
         */
        public Path getZipFile() {
            return distZip;
        }
    }
}
