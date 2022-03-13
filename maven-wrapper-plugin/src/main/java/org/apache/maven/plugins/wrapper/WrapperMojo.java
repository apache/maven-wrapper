package org.apache.maven.plugins.wrapper;

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

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.Maven;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

/**
 * Unpacks the maven-wrapper distribution files to the current project source tree.
 *
 * @author Robert Scholte
 * @since 3.0.0
 */
@Mojo( name = "wrapper", aggregator = true, requiresDirectInvocation = true )
public class WrapperMojo
    extends AbstractMojo
{
    private static final String MVNW_REPOURL = "MVNW_REPOURL";

    private static final String DEFAULT_REPOURL = "https://repo.maven.apache.org/maven2";

    // CONFIGURATION PARAMETERS

    /**
     * The version of Maven to require, default value is the Runtime version of Maven.
     * Can be any valid release above 2.0.9
     */
    @Parameter( property = "maven" )
    private String mavenVersion;

    /**
     * Options are:
     * <dl>
     *   <dt>script</dt>
     *   <dd>only mvnw scripts</dd>
     *   <dt>bin (default)</dt>
     *   <dd>precompiled and packaged code</dd>
     *   <dt>source</dt>
     *   <dd>Java source code, will be compiled on the fly</dd>
     * </dl>
     *
     * Value will be used as classifier of the downloaded file
     */
    @Parameter( defaultValue = "bin", property = "type" )
    private String distributionType;

    /**
     * Include <code>mvnwDebug*</code> scripts?
     */
    @Parameter( defaultValue = "false", property = "includeDebug" )
    private boolean includeDebugScript;

    // READONLY PARAMETERS

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    @Parameter( defaultValue = "${settings}", readonly = true, required = true )
    private Settings settings;

    // Waiting for https://github.com/eclipse/sisu.inject/pull/39 PathTypeConverter
    @Parameter( defaultValue = "${project.basedir}", readonly = true, required = true )
    private File basedir;

    // CONSTANTS

    private static final String WRAPPER_DISTRIBUTION_GROUP_ID = "org.apache.maven.wrapper";

    private static final String WRAPPER_DISTRIBUTION_ARTIFACT_ID = "maven-wrapper-distribution";

    private static final String WRAPPER_DISTRIBUTION_EXTENSION = "zip";

    // COMPONENTS

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private Map<String, UnArchiver> unarchivers;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        mavenVersion = getVersion( mavenVersion, Maven.class, "org.apache.maven/maven-core" );
        String wrapperVersion = getVersion( null, this.getClass(), "org.apache.maven.plugins/maven-wrapper-plugin" );

        final Artifact artifact = downloadWrapperDistribution( wrapperVersion );
        final Path wrapperDir = createDirectories( basedir.toPath().resolve( ".mvn/wrapper" ) );

        cleanup( wrapperDir );
        unpack( artifact, basedir.toPath() );
        replaceProperties( wrapperVersion, wrapperDir );
    }

    private Path createDirectories( Path dir )
        throws MojoExecutionException
    {
        try
        {
            return Files.createDirectories( dir );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( ioe.getMessage(), ioe );
        }
    }

    private void cleanup( Path wrapperDir )
        throws MojoExecutionException
    {
        try ( DirectoryStream<Path> dsClass = Files.newDirectoryStream( wrapperDir, "*.class" ) )
        {
            for ( Path file : dsClass )
            {
                // Cleanup old compiled *.class
                Files.deleteIfExists( file );
            }
            Files.deleteIfExists( wrapperDir.resolve( "maven-wrapper.jar" ) );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( ioe.getMessage(), ioe );
        }
    }

    private Artifact downloadWrapperDistribution( String wrapperVersion )
        throws MojoExecutionException
    {
        Artifact artifact =
            repositorySystem.createArtifactWithClassifier( WRAPPER_DISTRIBUTION_GROUP_ID,
                                                           WRAPPER_DISTRIBUTION_ARTIFACT_ID, wrapperVersion,
                                                           WRAPPER_DISTRIBUTION_EXTENSION, distributionType );

        MavenExecutionRequest executionRequest = session.getRequest();

        ArtifactResolutionRequest resolutionRequest = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setLocalRepository( session.getLocalRepository() )
            .setRemoteRepositories( session.getCurrentProject().getPluginArtifactRepositories() )
            .setOffline( executionRequest.isOffline() )
            .setForceUpdate( executionRequest.isUpdateSnapshots() );

        ArtifactResolutionResult resolveResult = repositorySystem.resolve( resolutionRequest );

        if ( !resolveResult.isSuccess() )
        {
            if ( executionRequest.isShowErrors() )
            {
                for ( Exception e : resolveResult.getExceptions() )
                {
                    getLog().error( e.getMessage(), e );
                }
            }
            throw new MojoExecutionException( "artifact: " + artifact + " not resolved." );
        }

        return artifact;
    }

    private void unpack( Artifact artifact, Path targetFolder )
    {
        UnArchiver unarchiver = unarchivers.get( WRAPPER_DISTRIBUTION_EXTENSION );
        unarchiver.setDestDirectory( targetFolder.toFile() );
        unarchiver.setSourceFile( artifact.getFile() );
        if ( !includeDebugScript )
        {
            unarchiver.setFileSelectors( new FileSelector[] { new FileSelector()
            {
                @Override
                public boolean isSelected( @Nonnull FileInfo fileInfo )
                    throws IOException
                {
                    return !fileInfo.getName().contains( "Debug" );
                }
            } } );
        }
        unarchiver.extract();
        getLog().info( "Unpacked " + buffer().strong( distributionType ) + " type wrapper distribution " + artifact );
    }

    /**
     * As long as the content only contains the license and the distributionUrl, we can simply replace it.
     * No need to look for other properties, restore them, respecting comments, etc.
     *
     * @param wrapperVersion the wrapper version
     * @param targetFolder the folder containing the wrapper.properties
     * @throws MojoExecutionException if writing fails
     */
    private void replaceProperties( String wrapperVersion, Path targetFolder )
        throws MojoExecutionException
    {
        String repoUrl = getRepoUrl();

        String distributionUrl =
            repoUrl + "/org/apache/maven/apache-maven/" + mavenVersion + "/apache-maven-" + mavenVersion + "-bin.zip";
        String wrapperUrl = repoUrl + "/org/apache/maven/wrapper/maven-wrapper/" + wrapperVersion + "/maven-wrapper-"
            + wrapperVersion + ".jar";

        Path wrapperPropertiesFile = targetFolder.resolve( "maven-wrapper.properties" );

        getLog().info( "Configuring .mvn/wrapper/maven-wrapper.properties to use "
            + buffer().strong( "Maven " + mavenVersion ) + " and download from " + repoUrl );

        final String license = "# Licensed to the Apache Software Foundation (ASF) under one%n"
            + "# or more contributor license agreements.  See the NOTICE file%n"
            + "# distributed with this work for additional information%n"
            + "# regarding copyright ownership.  The ASF licenses this file%n"
            + "# to you under the Apache License, Version 2.0 (the%n"
            + "# \"License\"); you may not use this file except in compliance%n"
            + "# with the License.  You may obtain a copy of the License at%n"
            + "#%n"
            + "#   https://www.apache.org/licenses/LICENSE-2.0%n"
            + "#%n"
            + "# Unless required by applicable law or agreed to in writing,%n"
            + "# software distributed under the License is distributed on an%n"
            + "# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY%n"
            + "# KIND, either express or implied.  See the License for the%n"
            + "# specific language governing permissions and limitations%n"
            + "# under the License.%n";

        try ( BufferedWriter out = Files.newBufferedWriter( wrapperPropertiesFile, StandardCharsets.UTF_8 ) )
        {
            out.append( String.format( Locale.ROOT, license ) );
            out.append( "distributionUrl=" + distributionUrl + System.lineSeparator() );
            out.append( "wrapperUrl=" + wrapperUrl + System.lineSeparator() );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Can't create maven-wrapper.properties", ioe );
        }
    }

    private String getVersion( String defaultVersion, Class<?> clazz, String path )
    {
        String version = defaultVersion;
        if ( version == null )
        {
            Properties props = new Properties();
            try ( InputStream is = clazz.getResourceAsStream( "/META-INF/maven/" + path + "/pom.properties" ) )
            {
                if ( is != null )
                {
                    props.load( is );
                    version = props.getProperty( "version" );
                }
            }
            catch ( IOException e )
            {
                // noop
            }
        }
        return version;
    }

    /**
     * Determine the repository URL to download Wrapper and Maven from.
     */
    private String getRepoUrl()
    {
        // default
        String repoUrl = DEFAULT_REPOURL;

        // adapt to also support MVNW_REPOURL as supported by mvnw scripts from maven-wrapper
        String mvnwRepoUrl = System.getenv( MVNW_REPOURL );
        if ( mvnwRepoUrl != null && !mvnwRepoUrl.isEmpty() )
        {
            repoUrl = mvnwRepoUrl;
            getLog().debug( "Using repo URL from " + MVNW_REPOURL + " environment variable." );
        }
        // otherwise mirror from settings
        else if ( settings.getMirrors() != null && !settings.getMirrors().isEmpty() )
        {
            for ( Mirror current : settings.getMirrors() )
            {
                if ( "*".equals( current.getMirrorOf() ) )
                {
                    repoUrl = current.getUrl();
                    break;
                }
            }
            getLog().debug( "Using repo URL from * mirror in settings file." );
        }

        getLog().debug( "Determined repo URL to use as " + repoUrl );

        return repoUrl;
    }
}
