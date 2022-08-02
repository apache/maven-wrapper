package org.apache.maven.wrapper;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Maven distribution installer, eventually using a {@link Downloader} first.
 *
 * @author Hans Dockter
 */
public class Installer
{
    public static final Path DEFAULT_DISTRIBUTION_PATH = Paths.get( "wrapper", "dists" );

    private final Downloader download;

    private final Verifier verifier;

    private final PathAssembler pathAssembler;

    public Installer( Downloader download, Verifier verifier, PathAssembler pathAssembler )
    {
        this.download = download;
        this.verifier = verifier;
        this.pathAssembler = pathAssembler;
    }

    public Path createDist( WrapperConfiguration configuration )
        throws Exception
    {
        URI distributionUrl = configuration.getDistribution();

        boolean alwaysDownload = configuration.isAlwaysDownload();
        boolean alwaysUnpack = configuration.isAlwaysUnpack();
        boolean verifyDistributionSha256Sum = !configuration.getDistributionSha256Sum().isEmpty();

        PathAssembler.LocalDistribution localDistribution = pathAssembler.getDistribution( configuration );
        Path localZipFile = localDistribution.getZipFile();

        if ( alwaysDownload || alwaysUnpack || Files.notExists( localZipFile ) )
        {
            Logger.info( "Installing Maven distribution " + localDistribution.getDistributionDir().toAbsolutePath() );
        }

        boolean downloaded = false;
        if ( alwaysDownload || Files.notExists( localZipFile ) )
        {
            Logger.info( "Downloading " + distributionUrl );
            Path tmpZipFile = localZipFile.resolveSibling( localZipFile.getFileName() + ".part" );
            Files.deleteIfExists( tmpZipFile );
            download.download( distributionUrl, tmpZipFile );
            Files.move( tmpZipFile, localZipFile, StandardCopyOption.REPLACE_EXISTING );
            downloaded = Files.exists( localZipFile );
        }

        Path distDir = localDistribution.getDistributionDir();
        List<Path> dirs = listDirs( distDir );

        if ( downloaded || alwaysUnpack || dirs.isEmpty() )
        {
            if ( verifyDistributionSha256Sum )
            {
                verifier.verify( localZipFile,
                        "distributionSha256Sum",
                        Verifier.SHA_256_ALGORITHM,
                        configuration.getDistributionSha256Sum() );
            }
            for ( Path dir : dirs )
            {
                Logger.info( "Deleting directory " + dir.toAbsolutePath() );
                deleteDir( dir );
            }
            Logger.info( "Unzipping " + localZipFile.toAbsolutePath() + " to " + distDir.toAbsolutePath() );
            unzip( localZipFile, distDir );
            dirs = listDirs( distDir );
            if ( dirs.isEmpty() )
            {
                throw new RuntimeException( String.format( Locale.ROOT,
                                                           "Maven distribution '%s' does not contain any directory."
                                                               + " Expected to find exactly 1 directory.",
                                                           distDir ) );
            }
            setExecutablePermissions( dirs.get( 0 ) );
        }
        if ( dirs.size() != 1 )
        {
            throw new RuntimeException( String.format( Locale.ROOT,
                                                       "Maven distribution '%s' contains too many directories."
                                                           + " Expected to find exactly 1 directory.",
                                                       distDir ) );
        }
        return dirs.get( 0 );
    }

    private List<Path> listDirs( Path distDir )
        throws IOException
    {
        List<Path> dirs = new ArrayList<>();
        if ( Files.exists( distDir ) )
        {
            try ( DirectoryStream<Path> dirStream = Files.newDirectoryStream( distDir ) )
            {
                for ( Path file : dirStream )
                {
                    if ( Files.isDirectory( file ) )
                    {
                        dirs.add( file );
                    }
                }
            }
        }
        return dirs;
    }

    private void setExecutablePermissions( Path mavenHome )
    {
        if ( isWindows() )
        {
            return;
        }
        Path mavenCommand = mavenHome.resolve( "bin/mvn" );
        try
        {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString( "rwxr-xr-x" );
            Files.setPosixFilePermissions( mavenCommand, perms );
        }
        catch ( IOException e )
        {
            Logger.warn( "Could not set executable permissions for: " + mavenCommand.toAbsolutePath()
                + ". Please do this manually if you want to use Maven." );
        }
    }

    private boolean isWindows()
    {
        String osName = System.getProperty( "os.name" ).toLowerCase( Locale.US );
        return osName.contains( "windows" );
    }

    private void deleteDir( Path dirPath )
        throws IOException
    {
        Files.walkFileTree( dirPath, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult postVisitDirectory( Path dir, IOException exc )
                throws IOException
            {
                Files.delete( dir );
                if ( exc != null )
                {
                    throw exc;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
                throws IOException
            {
                Files.delete( file );
                return FileVisitResult.CONTINUE;
            }
        } );
    }

    public void unzip( Path zip, Path dest )
        throws IOException
    {
        final Path destDir = dest.normalize();
        try ( final ZipFile zipFile = new ZipFile( zip.toFile() ) )
        {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while ( entries.hasMoreElements() )
            {
                final ZipEntry entry = entries.nextElement();

                Path fileEntry = destDir.resolve( entry.getName() ).normalize();
                if ( !fileEntry.startsWith( destDir ) )
                {
                    throw new ZipException( "Zip includes an invalid entry: " + entry.getName() );
                }

                if ( entry.isDirectory() )
                {
                    continue;
                }

                Files.createDirectories( fileEntry.getParent() );

                try ( InputStream inStream = zipFile.getInputStream( entry ) )
                {
                    Files.copy( inStream, fileEntry );
                }
            }
        }
    }
}
