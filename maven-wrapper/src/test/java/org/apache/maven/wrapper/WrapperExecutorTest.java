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

import static org.apache.maven.wrapper.MavenWrapperMain.MVNW_REPOURL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class WrapperExecutorTest
{
  private final Installer install;

  private final BootstrapMainStarter start;

  private Path propertiesFile;

  private Properties properties = new Properties();

  private Path testDir = Paths.get( "target/test-files/SystemPropertiesHandlerTest-" + System.currentTimeMillis() );

  private Path mockInstallDir = testDir.resolve( "mock-dir" );

  public WrapperExecutorTest()
    throws Exception
  {
    install = mock( Installer.class );
    when( install.createDist( Mockito.any( WrapperConfiguration.class ) ) ).thenReturn( mockInstallDir );
    start = mock( BootstrapMainStarter.class );

    Files.createDirectories( testDir );
    propertiesFile = testDir.resolve( "maven/wrapper/maven-wrapper.properties" );

    properties.put( "distributionUrl", "http://server/test/maven.zip" );
    properties.put( "distributionBase", "testDistBase" );
    properties.put( "distributionPath", "testDistPath" );
    properties.put( "zipStoreBase", "testZipBase" );
    properties.put( "zipStorePath", "testZipPath" );

    writePropertiesFile( properties, propertiesFile, "header" );

  }

  @Test
  public void loadWrapperMetadataFromFile()
    throws Exception
  {
    WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile( propertiesFile );

    Assert.assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getDistribution() );
    Assert.assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getConfiguration().getDistribution() );
    Assert.assertEquals( "testDistBase", wrapper.getConfiguration().getDistributionBase() );
    Assert.assertEquals( "testDistPath", wrapper.getConfiguration().getDistributionPath().toString() );
    Assert.assertEquals( "testZipBase", wrapper.getConfiguration().getZipBase() );
    Assert.assertEquals( "testZipPath", wrapper.getConfiguration().getZipPath().toString() );
  }

  @Test
  public void loadWrapperMetadataFromDirectory()
    throws Exception
  {
    WrapperExecutor wrapper = WrapperExecutor.forProjectDirectory( testDir );

    Assert.assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getDistribution() );
    Assert.assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getConfiguration().getDistribution() );
    Assert.assertEquals( "testDistBase", wrapper.getConfiguration().getDistributionBase() );
    Assert.assertEquals( "testDistPath", wrapper.getConfiguration().getDistributionPath().toString() );
    Assert.assertEquals( "testZipBase", wrapper.getConfiguration().getZipBase() );
    Assert.assertEquals( "testZipPath", wrapper.getConfiguration().getZipPath().toString() );
  }

  @Test
  public void useDefaultMetadataNoProeprtiesFile()
    throws Exception
  {
    WrapperExecutor wrapper = WrapperExecutor.forProjectDirectory( testDir.resolve( "unknown" ) );

    Assert.assertNull( wrapper.getDistribution() );
    Assert.assertNull( wrapper.getConfiguration().getDistribution() );
    Assert.assertEquals( PathAssembler.MAVEN_USER_HOME_STRING, wrapper.getConfiguration().getDistributionBase() );
    Assert.assertEquals( Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getDistributionPath() );
    Assert.assertEquals( PathAssembler.MAVEN_USER_HOME_STRING, wrapper.getConfiguration().getZipBase() );
    Assert.assertEquals( Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getZipPath() );
  }

  @Test
  public void propertiesFileOnlyContainsDistURL()
    throws Exception
  {

    properties = new Properties();
    properties.put( "distributionUrl", "http://server/test/maven.zip" );
    writePropertiesFile( properties, propertiesFile, "header" );

    WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile( propertiesFile );

    Assert.assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getDistribution() );
    Assert.assertEquals( new URI( "http://server/test/maven.zip" ), wrapper.getConfiguration().getDistribution() );
    Assert.assertEquals( PathAssembler.MAVEN_USER_HOME_STRING, wrapper.getConfiguration().getDistributionBase() );
    Assert.assertEquals( Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getDistributionPath() );
    Assert.assertEquals( PathAssembler.MAVEN_USER_HOME_STRING, wrapper.getConfiguration().getZipBase() );
    Assert.assertEquals( Installer.DEFAULT_DISTRIBUTION_PATH, wrapper.getConfiguration().getZipPath() );
  }

  @Test
  public void executeInstallAndLaunch()
    throws Exception
  {
    WrapperExecutor wrapper = WrapperExecutor.forProjectDirectory( propertiesFile );

    final String[] args = { "arg" };
    wrapper.execute( args, install, start );
    verify( install ).createDist( Mockito.any( WrapperConfiguration.class ) );
    verify( start ).start(
            Mockito.eq( args ),
            Mockito.eq( mockInstallDir ),
            Mockito.any( Properties.class ) );
  }

  @Test( )
  public void failWhenDistNotSetInProperties()
    throws Exception
  {
    properties = new Properties();
    writePropertiesFile( properties, propertiesFile, "header" );

    try
    {
      WrapperExecutor.forWrapperPropertiesFile( propertiesFile );
      Assert.fail( "Expected RuntimeException" );
    }
    catch ( RuntimeException e )
    {
      Assert.assertEquals( "Could not load wrapper properties from '" + propertiesFile + "'.", e.getMessage() );
      Assert.assertEquals( "No value with key 'distributionUrl' specified in wrapper properties file '" + propertiesFile
        + "'.", e.getCause().getMessage() );
    }

  }

  @Test
  public void failWhenPropertiesFileDoesNotExist()
  {
    propertiesFile = testDir.resolve( "unknown.properties" );

    try
    {
      WrapperExecutor.forWrapperPropertiesFile( propertiesFile );
      Assert.fail( "Expected RuntimeException" );
    }
    catch ( RuntimeException e )
    {
      Assert.assertEquals( "Wrapper properties file '" + propertiesFile + "' does not exist.", e.getMessage() );
    }
  }

  @Test
  public void testRelativeDistUrl()
    throws Exception
  {

    properties = new Properties();
    properties.put( "distributionUrl", "some/relative/url/to/bin.zip" );
    writePropertiesFile( properties, propertiesFile, "header" );

    WrapperExecutor wrapper = WrapperExecutor.forWrapperPropertiesFile( propertiesFile );
    Assert.assertNotEquals( "some/relative/url/to/bin.zip", wrapper.getDistribution().getSchemeSpecificPart() );
    Assert.assertTrue( wrapper.getDistribution().getSchemeSpecificPart().endsWith( "some/relative/url/to/bin.zip" ) );
  }

  @Test
  public void testEnvironmentVariableOverwrite_simpleCase()
    throws Exception
  {
    final Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put( MVNW_REPOURL, "https://repo/test" );

    properties = new Properties();
    properties.put( "distributionUrl", "https://server/path/to/bin.zip" );
    writePropertiesFile( properties, propertiesFile, "header" );

    WrapperExecutor wrapper = prepareWrapperExecutorWithEnvironmentVariables(environmentVariables);

    Assert.assertEquals( "https://repo/test/path/to/bin.zip", wrapper.getDistribution().toString() );
  }

  @Test
  public void testEnvironmentVariableOverwrite_mvnwRepoUrl_trailingSlash()
          throws Exception
  {
    final Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put( MVNW_REPOURL, "https://repo/test/" );
    properties = new Properties();
    properties.put( "distributionUrl", "https://server/path/to/bin.zip" );
    writePropertiesFile( properties, propertiesFile, "header" );

    WrapperExecutor wrapper = prepareWrapperExecutorWithEnvironmentVariables(environmentVariables);

    Assert.assertEquals( "https://repo/test/path/to/bin.zip", wrapper.getDistribution().toString() );
  }

  @Test
  public void testEnvironmentVariableOverwrite_packageName()
          throws Exception
  {
    final Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put( MVNW_REPOURL, "https://repo/test" );
    properties = new Properties();
    properties.put( "distributionUrl", "https://server/org/apache/maven/to/bin.zip" );
    writePropertiesFile( properties, propertiesFile, "header" );

    WrapperExecutor wrapper = prepareWrapperExecutorWithEnvironmentVariables(environmentVariables);

    Assert.assertEquals( "https://repo/test/org/apache/maven/to/bin.zip", wrapper.getDistribution().toString() );
  }

  @Test
  public void testEnvironmentVariableOverwrite_packageName_trailingSpace()
          throws Exception
  {
    final Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put( MVNW_REPOURL, "https://repo/test/" );
    properties = new Properties();
    properties.put( "distributionUrl", "https://server/whatever/org/apache/maven/to/bin.zip" );
    writePropertiesFile( properties, propertiesFile, "header" );

    WrapperExecutor wrapper = prepareWrapperExecutorWithEnvironmentVariables(environmentVariables);

    Assert.assertEquals( "https://repo/test/org/apache/maven/to/bin.zip", wrapper.getDistribution().toString() );
  }

  private WrapperExecutor prepareWrapperExecutorWithEnvironmentVariables(final Map<String, String> environmentVariables )
  {
    return new WrapperExecutor( propertiesFile, new Properties() ) {
      @Override
      protected String getEnv( String key )
      {
        return environmentVariables.get( key );
      }
    };
  }

  private void writePropertiesFile( Properties properties, Path propertiesFile, String message )
    throws Exception
  {
    Files.createDirectories( propertiesFile.getParent() );
    try ( OutputStream outStream = Files.newOutputStream( propertiesFile ) )
    {
      properties.store( outStream, message );
    }
  }
}
