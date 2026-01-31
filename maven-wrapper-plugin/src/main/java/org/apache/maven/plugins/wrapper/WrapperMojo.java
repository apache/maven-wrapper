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

import javax.inject.Inject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.Maven;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * Unpacks the maven-wrapper distribution files to the current project source tree.
 *
 * @since 3.0.0
 */
@Mojo(name = "wrapper", aggregator = true, requiresProject = false)
public class WrapperMojo extends AbstractMojo {
    private static final String MVNW_REPOURL = "MVNW_REPOURL";

    protected static final String DEFAULT_REPO_ID = "central";
    protected static final String DEFAULT_REPO_URL = "https://repo.maven.apache.org/maven2";

    // CONFIGURATION PARAMETERS

    /**
     * The version of Maven to require, default value is the Runtime version of Maven.
     * Can be any valid release above 2.0.9
     *
     * @since 3.0.0
     */
    @Parameter(property = "maven")
    private String mavenVersion;

    /**
     * The version of Maven Daemon to require.
     *
     * @since 3.2.0
     */
    @Parameter(property = "mvnd")
    private String mvndVersion;

    /**
     * The Maven Wrapper distribution type.
     * <p>
     * Options are:
     *
     * <dl>
     *   <dt>script</dt>
     *   <dd>only mvnw scripts</dd>
     *   <dt>bin</dt>
     *   <dd>precompiled and packaged code</dd>
     *   <dt>source</dt>
     *   <dd>Java source code, will be compiled on the fly</dd>
     *   <dt>only-script (default)</dt>
     *   <dd>the new lite implementation of mvnw/mvnw.cmd scripts downloads the maven directly and skips maven-wrapper.jar - since 3.2.0</dd>
     * </dl>
     *
     * If {@code -Dtype={type}} is not explicitly provided, then {@code distributionType} from
     * {@code .mvn/wrapper/maven-wrapper.properties} is used, if it exists.
     * Otherwise, {@code only-script} is used as the default distribution type.
     * <p>
     * This value will be used as the classifier of the downloaded file.
     *
     * @since 3.0.0
     */
    @Parameter(property = "type")
    private String distributionType;

    /**
     * Include <code>mvnwDebug*</code> scripts?
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "false", property = "includeDebug")
    private boolean includeDebugScript;

    /**
     * The expected SHA-256 checksum of the <i>maven-wrapper.jar</i> that is
     * used to load the configured Maven distribution.
     *
     * @since 3.2.0
     */
    @Parameter(property = "wrapperSha256Sum")
    private String wrapperSha256Sum;

    /**
     * The expected SHA-256 checksum of the Maven distribution that is
     * executed by the installed wrapper.
     *
     * @since 3.2.0
     */
    @Parameter(property = "distributionSha256Sum")
    private String distributionSha256Sum;

    /**
     * Determines if the Maven distribution should be downloaded
     * on every execution of the Maven wrapper.
     *
     * @since 3.2.0
     */
    @Parameter(defaultValue = "false", property = "alwaysDownload")
    private boolean alwaysDownload;

    /**
     * Determines if the Maven distribution should be unpacked
     * on every execution of the Maven wrapper.
     *
     * @since 3.2.0
     */
    @Parameter(defaultValue = "false", property = "alwaysUnpack")
    private boolean alwaysUnpack;

    /**
     * The URL to download the Maven distribution from.
     * If not specified, the URL will be constructed based on the Maven version
     * and repository URL.
     *
     * @since 3.3.0
     */
    @Parameter(property = "distributionUrl")
    private String distributionUrl;

    // CONSTANTS

    private static final String WRAPPER_DISTRIBUTION_GROUP_ID = "org.apache.maven.wrapper";

    private static final String WRAPPER_DISTRIBUTION_ARTIFACT_ID = "maven-wrapper-distribution";

    private static final String WRAPPER_DISTRIBUTION_EXTENSION = "zip";

    private static final String WRAPPER_DIR = ".mvn/wrapper";

    private static final String WRAPPER_PROPERTIES_FILENAME = "maven-wrapper.properties";

    private static final String DISTRIBUTION_TYPE_PROPERTY_NAME = "distributionType";

    private static final String TYPE_ONLY_SCRIPT = "only-script";

    private static final String DEFAULT_DISTRIBUTION_TYPE = TYPE_ONLY_SCRIPT;

    // COMPONENTS

    @Parameter(readonly = true, defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repositorySystemSession;

    @Inject
    private MavenSession session;

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private Map<String, UnArchiver> unarchivers;

    public WrapperMojo() {}

    /**
     * Ctor for UT.
     */
    public WrapperMojo(RepositorySystem system, RepositorySystemSession session) {
        this.repositorySystem = system;
        this.repositorySystemSession = session;
    }

    @Override
    public void execute() throws MojoExecutionException {
        final Path baseDir = Paths.get(session.getRequest().getBaseDirectory());
        final Path wrapperDir = baseDir.resolve(WRAPPER_DIR);

        if (distributionType == null) {
            distributionType = determineDistributionType(wrapperDir);
        }

        if (mvndVersion != null && mvndVersion.length() > 0 && !TYPE_ONLY_SCRIPT.equals(distributionType)) {
            throw new MojoExecutionException("maven-wrapper with type=" + distributionType
                    + " cannot work with mvnd, please set type to '" + TYPE_ONLY_SCRIPT + "'.");
        }

        mavenVersion = getVersion(mavenVersion, Maven.class, "org.apache.maven/maven-core");
        String wrapperVersion = getVersion(null, this.getClass(), "org.apache.maven.plugins/maven-wrapper-plugin");

        final Artifact artifact = downloadWrapperDistribution(wrapperVersion);

        createDirectories(wrapperDir);
        cleanup(wrapperDir);
        unpack(artifact, baseDir);
        replaceProperties(wrapperVersion, wrapperDir);
    }

    private String determineDistributionType(final Path wrapperDir) {
        final String typeFromMavenWrapperProperties = distributionTypeFromExistingMavenWrapperProperties(wrapperDir);
        if (typeFromMavenWrapperProperties != null) {
            return typeFromMavenWrapperProperties;
        }

        return DEFAULT_DISTRIBUTION_TYPE;
    }

    private String distributionTypeFromExistingMavenWrapperProperties(final Path wrapperDir) {
        final Path mavenWrapperProperties = wrapperDir.resolve(WRAPPER_PROPERTIES_FILENAME);
        try (InputStream inputStream = Files.newInputStream(mavenWrapperProperties)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty(DISTRIBUTION_TYPE_PROPERTY_NAME);
        } catch (IOException e) {
            return null;
        }
    }

    private void createDirectories(Path dir) throws MojoExecutionException {
        try {
            Files.createDirectories(dir);
        } catch (IOException ioe) {
            throw new MojoExecutionException(ioe.getMessage(), ioe);
        }
    }

    private void cleanup(Path wrapperDir) throws MojoExecutionException {
        try (DirectoryStream<Path> dsClass = Files.newDirectoryStream(wrapperDir, "*.class")) {
            for (Path file : dsClass) {
                // Cleanup old compiled *.class
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(wrapperDir.resolve("MavenWrapperDownloader.java"));
            Files.deleteIfExists(wrapperDir.resolve("maven-wrapper.jar"));
        } catch (IOException ioe) {
            throw new MojoExecutionException(ioe.getMessage(), ioe);
        }
    }

    private Artifact downloadWrapperDistribution(String wrapperVersion) throws MojoExecutionException {

        Artifact artifact = new DefaultArtifact(
                WRAPPER_DISTRIBUTION_GROUP_ID,
                WRAPPER_DISTRIBUTION_ARTIFACT_ID,
                distributionType,
                WRAPPER_DISTRIBUTION_EXTENSION,
                wrapperVersion);

        ArtifactRequest request = new ArtifactRequest();
        request.setRepositories(session.getCurrentProject().getRemotePluginRepositories());
        request.setArtifact(artifact);

        try {
            ArtifactResult artifactResult = repositorySystem.resolveArtifact(session.getRepositorySession(), request);
            return artifactResult.getArtifact();

        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("artifact: " + artifact + " not resolved.", e);
        }
    }

    private void unpack(Artifact artifact, Path targetFolder) {
        UnArchiver unarchiver = unarchivers.get(WRAPPER_DISTRIBUTION_EXTENSION);
        unarchiver.setDestDirectory(targetFolder.toFile());
        unarchiver.setSourceFile(artifact.getFile());
        if (!includeDebugScript) {
            unarchiver.setFileSelectors(
                    new FileSelector[] {fileInfo -> !fileInfo.getName().contains("Debug")});
        }
        unarchiver.extract();
        getLog().info("Unpacked " + buffer().strong(distributionType) + " type wrapper distribution " + artifact);
    }

    /**
     * As long as the content only contains the license and the distributionUrl, we can simply replace it.
     * No need to look for other properties, restore them, respecting comments, etc.
     *
     * @param wrapperVersion the wrapper version
     * @param targetFolder   the folder containing the wrapper.properties
     * @throws MojoExecutionException if writing fails
     */
    private void replaceProperties(String wrapperVersion, Path targetFolder) throws MojoExecutionException {
        String repoUrl = getRepoUrl();

        String finalDistributionUrl;
        if (distributionUrl != null && !distributionUrl.trim().isEmpty()) {
            // Use custom distribution URL if provided
            finalDistributionUrl = distributionUrl.trim();
        } else if (mvndVersion != null && mvndVersion.length() > 0) {
            // Use Maven Daemon distribution URL
            finalDistributionUrl = "https://archive.apache.org/dist/maven/mvnd/" + mvndVersion + "/maven-mvnd-"
                    + mvndVersion + "-bin.zip";
        } else {
            // Use standard Maven distribution URL
            finalDistributionUrl = repoUrl + "/org/apache/maven/apache-maven/" + mavenVersion + "/apache-maven-"
                    + mavenVersion + "-bin.zip";
        }

        String wrapperUrl = repoUrl + "/org/apache/maven/wrapper/maven-wrapper/" + wrapperVersion + "/maven-wrapper-"
                + wrapperVersion + ".jar";

        Path wrapperPropertiesFile = targetFolder.resolve("maven-wrapper.properties");

        getLog().info("Configuring .mvn/wrapper/maven-wrapper.properties to use "
                + buffer().strong("Maven " + mavenVersion) + " and download from " + repoUrl);

        try (BufferedWriter out = Files.newBufferedWriter(wrapperPropertiesFile, StandardCharsets.UTF_8)) {
            out.append("wrapperVersion=" + wrapperVersion + System.lineSeparator());
            out.append(DISTRIBUTION_TYPE_PROPERTY_NAME + "=" + distributionType + System.lineSeparator());
            out.append("distributionUrl=" + finalDistributionUrl + System.lineSeparator());
            if (distributionSha256Sum != null) {
                out.append("distributionSha256Sum=" + distributionSha256Sum + System.lineSeparator());
            }
            if (!distributionType.equals(TYPE_ONLY_SCRIPT)) {
                out.append("wrapperUrl=" + wrapperUrl + System.lineSeparator());
            }
            if (wrapperSha256Sum != null) {
                out.append("wrapperSha256Sum=" + wrapperSha256Sum + System.lineSeparator());
            }
            if (alwaysDownload) {
                out.append("alwaysDownload=" + Boolean.TRUE + System.lineSeparator());
            }
            if (alwaysUnpack) {
                out.append("alwaysUnpack=" + Boolean.TRUE + System.lineSeparator());
            }
        } catch (IOException ioe) {
            throw new MojoExecutionException("Can't create maven-wrapper.properties", ioe);
        }
    }

    private String getVersion(String defaultVersion, Class<?> clazz, String path) {
        String version = defaultVersion;
        if (version == null || version.trim().length() == 0 || "true".equals(version)) {
            Properties props = new Properties();
            try (InputStream is = clazz.getResourceAsStream("/META-INF/maven/" + path + "/pom.properties")) {
                if (is != null) {
                    props.load(is);
                    version = props.getProperty("version");
                }
            } catch (IOException e) {
                // noop
            }
        }
        return version;
    }

    /**
     * Determine the repository URL to download Wrapper and Maven from.
     */
    private String getRepoUrl() {
        // adapt to also support MVNW_REPOURL as supported by mvnw scripts from maven-wrapper
        String envRepoUrl = System.getenv(MVNW_REPOURL);
        final String repoUrl = determineRepoUrl(envRepoUrl);
        getLog().debug("Determined repo URL to use as " + repoUrl);
        return repoUrl;
    }

    protected String determineRepoUrl(String envRepoUrl) {
        if (envRepoUrl != null && !envRepoUrl.trim().isEmpty() && envRepoUrl.length() > 4) {
            String repoUrl = envRepoUrl.trim();
            if (repoUrl.endsWith("/")) {
                repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
            }
            getLog().debug("Using repo URL from " + MVNW_REPOURL + " environment variable.");
            return repoUrl;
        }

        return repositorySystem
                .newResolutionRepositories(
                        repositorySystemSession,
                        Collections.singletonList(
                                new RemoteRepository.Builder(DEFAULT_REPO_ID, "default", DEFAULT_REPO_URL).build()))
                .get(0)
                .getUrl();
    }
}
