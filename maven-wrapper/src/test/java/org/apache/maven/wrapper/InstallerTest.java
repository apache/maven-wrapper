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
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Hans Dockter
 */
public class InstallerTest {
    @TempDir
    private File tempFolder;

    private Path testDir;

    private Installer install;

    private Path distributionDir;

    private Path zipStore;

    private Path mavenHomeDir;

    private Path zipDestination;

    private WrapperConfiguration configuration = new WrapperConfiguration();

    private Downloader download;

    private Verifier verifier;

    private PathAssembler pathAssembler;

    private PathAssembler.LocalDistribution localDistribution;

    @BeforeEach
    void setup() throws Exception {
        testDir = tempFolder.toPath();

        configuration.setZipBase(PathAssembler.PROJECT_STRING);
        configuration.setZipPath(Paths.get("someZipPath"));
        configuration.setDistributionBase(PathAssembler.MAVEN_USER_HOME_STRING);
        configuration.setDistributionPath(Paths.get("someDistPath"));
        configuration.setDistribution(new URI("http://server/maven-0.9.zip"));
        configuration.setAlwaysDownload(false);
        configuration.setAlwaysUnpack(false);
        configuration.setDistributionSha256Sum("");
        distributionDir = testDir.resolve("someDistPath");
        mavenHomeDir = distributionDir.resolve("maven-0.9");
        zipStore = testDir.resolve("zips");
        zipDestination = zipStore.resolve("maven-0.9.zip");

        download = mock(Downloader.class);
        verifier = mock(Verifier.class);
        pathAssembler = mock(PathAssembler.class);
        localDistribution = mock(PathAssembler.LocalDistribution.class);

        when(localDistribution.getZipFile()).thenReturn(zipDestination);
        when(localDistribution.getDistributionDir()).thenReturn(distributionDir);
        when(pathAssembler.getDistribution(configuration)).thenReturn(localDistribution);

        install = new Installer(download, verifier, pathAssembler);
    }

    private void createTestZip(Path zipDestination) throws Exception {
        Path explodedZipDir = testDir.resolve("explodedZip");
        Files.createDirectories(explodedZipDir);
        Files.createDirectories(zipDestination.getParent());
        Path mavenScript = explodedZipDir.resolve("maven-0.9/bin/mvn");
        Path mavenLib = explodedZipDir.resolve("maven-0.9/lib/maven-core-0.9.jar");
        Files.createDirectories(mavenScript.getParent());
        Files.createDirectories(mavenLib.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(mavenScript, StandardCharsets.UTF_8)) {
            writer.write("something");
        }
        try (OutputStream os = Files.newOutputStream(mavenLib);
                JarOutputStream jar = new JarOutputStream(os, new Manifest())) {
            jar.putNextEntry(new ZipEntry("test"));
            jar.closeEntry();
        }

        zipTo(explodedZipDir, zipDestination);
    }

    public void testCreateDist() throws Exception {
        Path homeDir = install.createDist(configuration);

        assertEquals(mavenHomeDir, homeDir);
        assertTrue(Files.isDirectory(homeDir));
        assertTrue(Files.exists(homeDir.resolve("bin/mvn")));
        assertTrue(Files.exists(zipDestination));

        assertEquals(localDistribution, pathAssembler.getDistribution(configuration));
        assertEquals(distributionDir, localDistribution.getDistributionDir());
        assertEquals(zipDestination, localDistribution.getZipFile());

        // download.download(new URI("http://some/test"), distributionDir);
        // verify(download).download(new URI("http://some/test"), distributionDir);
    }

    @Test
    void createDistWithExistingDistribution() throws Exception {

        createTestZip(zipDestination);
        Files.createDirectories(mavenHomeDir);
        Path someFile = mavenHomeDir.resolve("some-file");
        touchFile(someFile);

        Path homeDir = install.createDist(configuration);

        assertEquals(mavenHomeDir, homeDir);
        assertTrue(Files.isDirectory(mavenHomeDir));
        assertTrue(Files.exists(homeDir.resolve("some-file")));
        assertTrue(Files.exists(zipDestination));

        assertEquals(localDistribution, pathAssembler.getDistribution(configuration));
        assertEquals(distributionDir, localDistribution.getDistributionDir());
        assertEquals(zipDestination, localDistribution.getZipFile());
    }

    @Test
    void createDistWithExistingDistAndZipAndAlwaysUnpackTrue() throws Exception {

        createTestZip(zipDestination);
        Files.createDirectories(mavenHomeDir);
        Path garbage = mavenHomeDir.resolve("garbage");
        touchFile(garbage);
        configuration.setAlwaysUnpack(true);

        Path homeDir = install.createDist(configuration);

        assertEquals(mavenHomeDir, homeDir);
        assertTrue(Files.isDirectory(mavenHomeDir));
        assertFalse(Files.exists(homeDir.resolve("garbage")));
        assertTrue(Files.exists(zipDestination));

        assertEquals(localDistribution, pathAssembler.getDistribution(configuration));
        assertEquals(distributionDir, localDistribution.getDistributionDir());
        assertEquals(zipDestination, localDistribution.getZipFile());
    }

    @Test
    void createDistWithExistingZipAndDistAndAlwaysDownloadTrue() throws Exception {

        createTestZip(zipDestination);
        Path garbage = mavenHomeDir.resolve("garbage");
        touchFile(garbage);
        configuration.setAlwaysUnpack(true);

        Path homeDir = install.createDist(configuration);

        assertEquals(mavenHomeDir, homeDir);
        assertTrue(Files.isDirectory(mavenHomeDir));
        assertTrue(Files.exists(homeDir.resolve("bin/mvn")));
        assertFalse(Files.exists(homeDir.resolve("garbage")));
        assertTrue(Files.exists(zipDestination));

        assertEquals(localDistribution, pathAssembler.getDistribution(configuration));
        assertEquals(distributionDir, localDistribution.getDistributionDir());
        assertEquals(zipDestination, localDistribution.getZipFile());

        // download.download(new URI("http://some/test"), distributionDir);
        // verify(download).download(new URI("http://some/test"), distributionDir);
    }

    @Test
    void zipSlip() throws Exception {
        URL resource = getClass().getClassLoader().getResource("zip-slip.zip");
        Path zipSlip = Paths.get(resource.toURI());
        when(localDistribution.getZipFile()).thenReturn(zipSlip);
        configuration.setAlwaysUnpack(true);

        try {
            install.createDist(configuration);
            fail("Should fail as it contains a zip slip.");
        } catch (Exception ex) {
            assertInstanceOf(ZipException.class, ex);
        }
    }

    public void zipTo(final Path directoryToZip, final Path zipFile) throws IOException {
        // Creating a ZipOutputStream by wrapping a OutputStream
        try (OutputStream fos = Files.newOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)) {
            // Walk the tree structure using WalkFileTree method
            Files.walkFileTree(directoryToZip, new SimpleFileVisitor<Path>() {
                @Override
                // Before visiting the directory create the directory in zip archive
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                        throws IOException {
                    // Don't create dir for root folder as it is already created with .zip name
                    if (!directoryToZip.equals(dir)) {
                        zos.putNextEntry(
                                new ZipEntry(directoryToZip.relativize(dir).toString() + "/"));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                // For each visited file add it to zip entry
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(
                            new ZipEntry(directoryToZip.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void touchFile(Path file) throws IOException {
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        }
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
    }
}
