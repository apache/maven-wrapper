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
import java.net.URI;
import java.nio.file.Path;

/**
 * Interface for downloading and extracting binary distributions.
 * Supports various archive formats including zip and tar.gz.
 */
interface BinaryDownloader {

    /**
     * Downloads and extracts a binary distribution.
     *
     * @param downloadUrl URL to download the binary from
     * @param extractDir directory to extract the binary to
     * @param sha256Sum optional SHA-256 checksum for verification
     * @return path to the extracted binary directory
     * @throws IOException if download or extraction fails
     */
    Path downloadAndExtract(URI downloadUrl, Path extractDir, String sha256Sum) throws IOException;

    /**
     * Checks if the given file extension is supported for extraction.
     *
     * @param fileName the file name to check
     * @return true if supported, false otherwise
     */
    boolean isSupported(String fileName);
}

/**
 * Default implementation of BinaryDownloader that supports zip and tar.gz archives.
 */
class DefaultBinaryDownloader implements BinaryDownloader {

    private final Downloader downloader;
    private final Verifier verifier;

    DefaultBinaryDownloader(Downloader downloader, Verifier verifier) {
        this.downloader = downloader;
        this.verifier = verifier;
    }

    @Override
    public Path downloadAndExtract(URI downloadUrl, Path extractDir, String sha256Sum) throws IOException {
        if (downloadUrl == null) {
            throw new IllegalArgumentException("Download URL cannot be null");
        }
        if (extractDir == null) {
            throw new IllegalArgumentException("Extract directory cannot be null");
        }

        String fileName = getFileNameFromUrl(downloadUrl);
        if (!isSupported(fileName)) {
            throw new IOException(
                    "Unsupported archive format: " + fileName + ". Supported formats: .zip, .tar.gz, .tgz");
        }

        try {
            // Create extract directory
            java.nio.file.Files.createDirectories(extractDir);

            // Download the archive
            Path archiveFile = extractDir.resolve(fileName);
            Logger.info("Downloading " + downloadUrl + " to " + archiveFile);
            downloader.download(downloadUrl, archiveFile);

            // Verify checksum if provided
            if (sha256Sum != null && !sha256Sum.trim().isEmpty()) {
                Logger.info("Verifying SHA-256 checksum");
                verifier.verify(archiveFile, "sha256Sum", Verifier.SHA_256_ALGORITHM, sha256Sum);
            }

            // Extract the archive
            Path extractedDir = extractArchive(archiveFile, extractDir);

            // Clean up the archive file
            java.nio.file.Files.deleteIfExists(archiveFile);

            return extractedDir;

        } catch (Exception e) {
            throw new IOException("Failed to download and extract binary from " + downloadUrl, e);
        }
    }

    @Override
    public boolean isSupported(String fileName) {
        if (fileName == null) {
            return false;
        }

        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".zip") || lowerFileName.endsWith(".tar.gz") || lowerFileName.endsWith(".tgz");
    }

    /**
     * Extracts the archive file to the specified directory.
     *
     * @param archiveFile the archive file to extract
     * @param extractDir the directory to extract to
     * @return path to the extracted content directory
     * @throws IOException if extraction fails
     */
    private Path extractArchive(Path archiveFile, Path extractDir) throws IOException {
        String fileName = archiveFile.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".zip")) {
            return extractZip(archiveFile, extractDir);
        } else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
            return extractTarGz(archiveFile, extractDir);
        } else {
            throw new IOException("Unsupported archive format: " + fileName);
        }
    }

    /**
     * Extracts a ZIP archive.
     */
    private Path extractZip(Path zipFile, Path extractDir) throws IOException {
        Logger.info("Extracting ZIP archive " + zipFile + " to " + extractDir);

        // Use the existing unzip method from Installer
        // Create a temporary installer instance for extraction
        Installer installer = new Installer(downloader, verifier, new PathAssembler(extractDir.getParent()));
        installer.unzip(zipFile, extractDir);

        return findExtractedDirectory(extractDir);
    }

    /**
     * Extracts a TAR.GZ archive.
     */
    private Path extractTarGz(Path tarGzFile, Path extractDir) throws IOException {
        Logger.info("Extracting TAR.GZ archive " + tarGzFile + " to " + extractDir);

        // For TAR.GZ extraction, we would need to implement tar extraction
        // For now, throw an exception indicating it's not yet implemented
        throw new IOException("TAR.GZ extraction not yet implemented. Please use ZIP archives for now.");
    }

    /**
     * Finds the main directory after extraction.
     * Many archives contain a single top-level directory.
     */
    private Path findExtractedDirectory(Path extractDir) throws IOException {
        try (java.nio.file.DirectoryStream<Path> stream = java.nio.file.Files.newDirectoryStream(extractDir)) {
            Path firstDir = null;
            int dirCount = 0;

            for (Path entry : stream) {
                if (java.nio.file.Files.isDirectory(entry)) {
                    firstDir = entry;
                    dirCount++;
                }
            }

            // If there's exactly one directory, return it
            if (dirCount == 1 && firstDir != null) {
                return firstDir;
            }

            // Otherwise, return the extract directory itself
            return extractDir;
        }
    }

    /**
     * Extracts the file name from a URL.
     */
    private String getFileNameFromUrl(URI url) {
        String path = url.getPath();
        if (path == null || path.isEmpty()) {
            return "download";
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }

        return path;
    }
}
