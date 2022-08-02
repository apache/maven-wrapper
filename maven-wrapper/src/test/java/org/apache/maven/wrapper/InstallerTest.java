package org.apache.maven.wrapper;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Hans Dockter
 */
public class InstallerTest
{
  @Rule
  public TemporaryFolder tempFolder = TemporaryFolder.builder().assureDeletion().build();

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

  @Before
  public void setup()
    throws Exception
  {
    testDir = tempFolder.getRoot().toPath();

    configuration.setZipBase( PathAssembler.PROJECT_STRING );
    configuration.setZipPath( Paths.get( "someZipPath" ) );
    configuration.setDistributionBase( PathAssembler.MAVEN_USER_HOME_STRING );
    configuration.setDistributionPath( Paths.get( "someDistPath" ) );
    configuration.setDistribution( new URI( "http://server/maven-0.9.zip" ) );
    configuration.setAlwaysDownload( false );
    configuration.setAlwaysUnpack( false );
    configuration.setDistributionSha256Sum( "" );
    distributionDir = testDir.resolve( "someDistPath" );
    mavenHomeDir = distributionDir.resolve( "maven-0.9" );
    zipStore = testDir.resolve( "zips" );
    zipDestination = zipStore.resolve( "maven-0.9.zip" );

    download = mock( Downloader.class );
    verifier = mock ( Verifier.class );
    pathAssembler = mock( PathAssembler.class );
    localDistribution = mock( PathAssembler.LocalDistribution.class );

    when( localDistribution.getZipFile() ).thenReturn( zipDestination );
    when( localDistribution.getDistributionDir() ).thenReturn( distributionDir );
    when( pathAssembler.getDistribution( configuration ) ).thenReturn( localDistribution );

    install = new Installer( download, verifier, pathAssembler );
  }

  private void createTestZip( Path zipDestination )
    throws Exception
  {
    Path explodedZipDir = testDir.resolve( "explodedZip" );
    Files.createDirectories( explodedZipDir );
    Files.createDirectories( zipDestination.getParent() );
    Path mavenScript = explodedZipDir.resolve( "maven-0.9/bin/mvn" );
    Path mavenLib = explodedZipDir.resolve( "maven-0.9/lib/maven-core-0.9.jar" );
    Files.createDirectories( mavenScript.getParent() );
    Files.createDirectories( mavenLib.getParent() );
    try ( BufferedWriter writer = Files.newBufferedWriter( mavenScript, StandardCharsets.UTF_8 ) )
    {
      writer.write( "something" );
    }
    try ( OutputStream os = Files.newOutputStream( mavenLib );
            JarOutputStream jar = new JarOutputStream( os, new Manifest() ) )
    {
      jar.putNextEntry( new ZipEntry( "test" ) );
      jar.closeEntry();
    }

    zipTo( explodedZipDir, zipDestination );
  }

  public void testCreateDist()
    throws Exception
  {
    Path homeDir = install.createDist( configuration );

    Assert.assertEquals( mavenHomeDir, homeDir );
    Assert.assertTrue( Files.isDirectory( homeDir ) );
    Assert.assertTrue( Files.exists( homeDir.resolve( "bin/mvn" ) ) );
    Assert.assertTrue( Files.exists( zipDestination ) );

    Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
    Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
    Assert.assertEquals( zipDestination, localDistribution.getZipFile() );

    // download.download(new URI("http://some/test"), distributionDir);
    // verify(download).download(new URI("http://some/test"), distributionDir);
  }

  @Test
  public void testCreateDistWithExistingDistribution()
    throws Exception
  {

    createTestZip( zipDestination );
    Files.createDirectories( mavenHomeDir );
    Path someFile = mavenHomeDir.resolve( "some-file" );
    touchFile( someFile );

    Path homeDir = install.createDist( configuration );

    Assert.assertEquals( mavenHomeDir, homeDir );
    Assert.assertTrue( Files.isDirectory( mavenHomeDir ) );
    Assert.assertTrue( Files.exists( homeDir.resolve( "some-file" ) ) );
    Assert.assertTrue( Files.exists( zipDestination ) );

    Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
    Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
    Assert.assertEquals( zipDestination, localDistribution.getZipFile() );
  }

  @Test
  public void testCreateDistWithExistingDistAndZipAndAlwaysUnpackTrue()
    throws Exception
  {

    createTestZip( zipDestination );
    Files.createDirectories( mavenHomeDir );
    Path garbage = mavenHomeDir.resolve( "garbage" );
    touchFile( garbage );
    configuration.setAlwaysUnpack( true );

    Path homeDir = install.createDist( configuration );

    Assert.assertEquals( mavenHomeDir, homeDir );
    Assert.assertTrue( Files.isDirectory( mavenHomeDir ) );
    Assert.assertFalse( Files.exists( homeDir.resolve( "garbage" ) ) );
    Assert.assertTrue( Files.exists( zipDestination ) );

    Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
    Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
    Assert.assertEquals( zipDestination, localDistribution.getZipFile() );
  }

  @Test
  public void testCreateDistWithExistingZipAndDistAndAlwaysDownloadTrue()
    throws Exception
  {

    createTestZip( zipDestination );
    Path garbage = mavenHomeDir.resolve( "garbage" );
    touchFile( garbage );
    configuration.setAlwaysUnpack( true );

    Path homeDir = install.createDist( configuration );

    Assert.assertEquals( mavenHomeDir, homeDir );
    Assert.assertTrue( Files.isDirectory( mavenHomeDir ) );
    Assert.assertTrue( Files.exists( homeDir.resolve( "bin/mvn" ) ) );
    Assert.assertFalse( Files.exists( homeDir.resolve( "garbage" ) ) );
    Assert.assertTrue( Files.exists( zipDestination ) );

    Assert.assertEquals( localDistribution, pathAssembler.getDistribution( configuration ) );
    Assert.assertEquals( distributionDir, localDistribution.getDistributionDir() );
    Assert.assertEquals( zipDestination, localDistribution.getZipFile() );

    // download.download(new URI("http://some/test"), distributionDir);
    // verify(download).download(new URI("http://some/test"), distributionDir);
  }

  @Test
  public void testZipSlip()
    throws URISyntaxException
  {
    URL resource = getClass().getClassLoader().getResource( "zip-slip.zip" );
    Path zipSlip = Paths.get( resource.toURI() );
    when( localDistribution.getZipFile() ).thenReturn( zipSlip );
    configuration.setAlwaysUnpack( true );

    try
    {
      install.createDist( configuration );
      fail( "Should fail as it contains a zip slip." );
    }
    catch ( Exception ex )
    {
      assertTrue( ex instanceof ZipException );
    }
  }

  public void zipTo( final Path directoryToZip, final Path zipFile )
    throws IOException
  {
    // Creating a ZipOutputStream by wrapping a OutputStream
    try ( OutputStream fos = Files.newOutputStream( zipFile ); ZipOutputStream zos = new ZipOutputStream( fos ) )
    {
      // Walk the tree structure using WalkFileTree method
      Files.walkFileTree( directoryToZip, new SimpleFileVisitor<Path>()
      {
        @Override
        // Before visiting the directory create the directory in zip archive
        public FileVisitResult preVisitDirectory( final Path dir, final BasicFileAttributes attrs )
          throws IOException
        {
          // Don't create dir for root folder as it is already created with .zip name
          if ( !directoryToZip.equals( dir ) )
          {
            zos.putNextEntry( new ZipEntry( directoryToZip.relativize( dir ).toString() + "/" ) );
            zos.closeEntry();
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        // For each visited file add it to zip entry
        public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs )
          throws IOException
        {
          zos.putNextEntry( new ZipEntry( directoryToZip.relativize( file ).toString() ) );
          Files.copy( file, zos );
          zos.closeEntry();
          return FileVisitResult.CONTINUE;
        }
      } );
    }
  }

  private void touchFile( Path file )
    throws IOException
  {
    if ( Files.notExists( file ) )
    {
      Files.createDirectories( file.getParent() );
      Files.createFile( file );
    }
    Files.setLastModifiedTime( file, FileTime.fromMillis( System.currentTimeMillis() ) );
  }

}
