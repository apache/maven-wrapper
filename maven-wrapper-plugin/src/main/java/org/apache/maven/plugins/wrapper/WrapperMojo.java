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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.Maven;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
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

    protected static final String DEFAULT_REPOURL = "https://repo.maven.apache.org/maven2";

    // Disco API constants
    private static final String DISCO_API_BASE_URL = "https://api.foojay.io/disco/v3.0";

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
     * The JDK version to use for Maven execution.
     * Can be any valid JDK release, such as "17", "21", or "17.0.14".
     */
    @Parameter(property = "jdk")
    private String jdkVersion;

    /**
     * The distribution of JDK to download using native Disco API names.
     * Supported distributions: temurin, corretto, zulu, liberica, oracle_open_jdk, microsoft, semeru, graalvm_ce11, etc.
     * Default is temurin.
     */
    @Parameter(property = "jdkDistribution", defaultValue = "temurin")
    private String jdkDistribution;

    /**
     * Direct URL for main JDK distribution download.
     * If specified, overrides jdkVersion and jdkDistribution.
     */
    @Parameter(property = "jdkUrl")
    private String jdkDistributionUrl;

    /**
     * The JDK version to use for toolchains.
     * Can be any valid JDK release.
     */
    @Parameter(property = "toolchainJdk")
    private String toolchainJdkVersion;

    /**
     * The distribution of JDK to download for toolchains using native Disco API names.
     * Supported distributions: temurin, corretto, zulu, liberica, oracle_open_jdk, microsoft, semeru, graalvm_ce11, etc.
     * Default is temurin.
     */
    @Parameter(property = "toolchainJdkDistribution", defaultValue = "temurin")
    private String toolchainJdkDistribution;

    /**
     * Direct URL for toolchain JDK distribution download.
     * If specified, overrides toolchainJdkVersion and toolchainJdkDistribution.
     */
    @Parameter(property = "toolchainJdkUrl")
    private String toolchainJdkDistributionUrl;

    /**
     * SHA-256 checksum for the main JDK distribution
     */
    @Parameter(property = "jdkSha256Sum")
    private String jdkSha256Sum;

    /**
     * SHA-256 checksum for the toolchain JDK distribution
     */
    @Parameter(property = "toolchainJdkSha256Sum")
    private String toolchainJdkSha256Sum;

    /**
     * JDK update policy for major version resolution. Controls how often the latest patch version is checked.
     * Supported values: never, daily, always, interval:X (where X is minutes).
     * Default is daily.
     */
    @Parameter(property = "jdkUpdatePolicy", defaultValue = "daily")
    private String jdkUpdatePolicy;

    /**
     * Skip JDK version stability warnings for non-LTS versions or specific version pinning.
     * Default is false (warnings are shown).
     */
    @Parameter(property = "skipJdkWarnings", defaultValue = "false")
    private boolean skipJdkWarnings;

    // READONLY PARAMETERS

    @Component
    private MavenSession session;

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

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private Map<String, UnArchiver> unarchivers;

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

        String distributionUrl = repoUrl + "/org/apache/maven/apache-maven/" + mavenVersion + "/apache-maven-"
                + mavenVersion + "-bin.zip";
        String wrapperUrl = repoUrl + "/org/apache/maven/wrapper/maven-wrapper/" + wrapperVersion + "/maven-wrapper-"
                + wrapperVersion + ".jar";

        if (mvndVersion != null && mvndVersion.length() > 0) {
            // now maven-mvnd is not published to the central repo.
            distributionUrl = "https://archive.apache.org/dist/maven/mvnd/" + mvndVersion + "/maven-mvnd-" + mvndVersion
                    + "-bin.zip";
        }

        Path wrapperPropertiesFile = targetFolder.resolve("maven-wrapper.properties");

        getLog().info("Configuring .mvn/wrapper/maven-wrapper.properties to use "
                + buffer().strong("Maven " + mavenVersion) + " and download from " + repoUrl);

        final String license = "# Licensed to the Apache Software Foundation (ASF) under one%n"
                + "# or more contributor license agreements.  See the NOTICE file%n"
                + "# distributed with this work for additional information%n"
                + "# regarding copyright ownership.  The ASF licenses this file%n"
                + "# to you under the Apache License, Version 2.0 (the%n"
                + "# \"License\"); you may not use this file except in compliance%n"
                + "# with the License.  You may obtain a copy of the License at%n"
                + "#%n"
                + "#   http://www.apache.org/licenses/LICENSE-2.0%n"
                + "#%n"
                + "# Unless required by applicable law or agreed to in writing,%n"
                + "# software distributed under the License is distributed on an%n"
                + "# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY%n"
                + "# KIND, either express or implied.  See the License for the%n"
                + "# specific language governing permissions and limitations%n"
                + "# under the License.%n";

        try (BufferedWriter out = Files.newBufferedWriter(wrapperPropertiesFile, StandardCharsets.UTF_8)) {
            out.append(String.format(Locale.ROOT, license));
            out.append("wrapperVersion=" + wrapperVersion + System.lineSeparator());
            out.append(DISTRIBUTION_TYPE_PROPERTY_NAME + "=" + distributionType + System.lineSeparator());
            out.append("distributionUrl=" + distributionUrl + System.lineSeparator());
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
                out.append("alwaysDownload=true" + System.lineSeparator());
            }
            if (alwaysUnpack) {
                out.append("alwaysUnpack=true" + System.lineSeparator());
            }
            if (jdkVersion != null) {
                out.append("jdkVersion=" + jdkVersion + System.lineSeparator());
            }
            if (jdkDistribution != null) {
                out.append("jdkDistribution=" + jdkDistribution + System.lineSeparator());
            }
            if (jdkDistributionUrl != null) {
                out.append("jdkDistributionUrl=" + jdkDistributionUrl + System.lineSeparator());
            }
            if (toolchainJdkVersion != null) {
                out.append("toolchainJdkVersion=" + toolchainJdkVersion + System.lineSeparator());
            }
            if (toolchainJdkDistribution != null) {
                out.append("toolchainJdkDistribution=" + toolchainJdkDistribution + System.lineSeparator());
            }
            if (toolchainJdkDistributionUrl != null) {
                out.append("toolchainJdkDistributionUrl=" + toolchainJdkDistributionUrl + System.lineSeparator());
            }
            if (jdkSha256Sum != null) {
                out.append("jdkSha256Sum=" + jdkSha256Sum + System.lineSeparator());
            }
            if (toolchainJdkSha256Sum != null) {
                out.append("toolchainJdkSha256Sum=" + toolchainJdkSha256Sum + System.lineSeparator());
            }
            if (jdkUpdatePolicy != null) {
                out.append("jdkUpdatePolicy=" + jdkUpdatePolicy + System.lineSeparator());
            }

            // Show JDK version stability warnings at installation time
            if (!skipJdkWarnings) {
                showJdkVersionWarnings();
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
        final String repoUrl = determineRepoUrl(envRepoUrl, session.getSettings());

        getLog().debug("Determined repo URL to use as " + repoUrl);

        return repoUrl;
    }

    protected String determineRepoUrl(String envRepoUrl, Settings settings) {

        if (envRepoUrl != null && !envRepoUrl.trim().isEmpty() && envRepoUrl.length() > 4) {
            String repoUrl = envRepoUrl.trim();

            if (repoUrl.endsWith("/")) {
                repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
            }

            getLog().debug("Using repo URL from " + MVNW_REPOURL + " environment variable.");

            return repoUrl;
        }

        // otherwise mirror from settings
        if (settings.getMirrors() != null && !settings.getMirrors().isEmpty()) {
            for (Mirror current : settings.getMirrors()) {
                if ("*".equals(current.getMirrorOf())) {
                    getLog().debug("Using repo URL from * mirror in settings file.");
                    return current.getUrl();
                }
            }
        }

        return DEFAULT_REPOURL;
    }

    private void showJdkVersionWarnings() {
        boolean warningsShown = false;

        // Check main JDK version
        if (jdkVersion != null && jdkDistributionUrl == null) {
            if (checkJdkVersionStability("Main JDK", jdkVersion, jdkDistribution)) {
                warningsShown = true;
            }
        }

        // Check toolchain JDK version
        if (toolchainJdkVersion != null && toolchainJdkDistributionUrl == null) {
            if (checkJdkVersionStability("Toolchain JDK", toolchainJdkVersion, toolchainJdkDistribution)) {
                warningsShown = true;
            }
        }

        // Only show suppression message if warnings were actually displayed
        if (warningsShown) {
            getLog().warn("To suppress these warnings, use -DskipJdkWarnings=true");
            getLog().warn("");
        }
    }

    private boolean checkJdkVersionStability(String jdkType, String version, String distribution) {
        if (version == null) {
            return false;
        }

        boolean warningShown = false;

        // Check for non-LTS versions
        if (isNonLtsVersion(version)) {
            getLog().warn("");
            getLog().warn("WARNING: " + jdkType + " " + version + " is not an LTS (Long Term Support) version.");
            getLog().warn("Non-LTS versions may have shorter support lifecycles and could become");
            getLog().warn("unavailable from distribution providers when newer versions are released.");
            getLog().warn("");
            getLog().warn("For better long-term stability, consider:");
            Set<Integer> ltsVersions = getLtsVersionsFromDiscoApi();
            getLog().warn("1. Switch to an LTS version: " + formatLtsVersions(ltsVersions));
            getLog().warn("2. Use a direct URL with -DjdkDistributionUrl=https://...");
            getLog().warn("3. Pin to exact version and resolve URL explicitly");
            getLog().warn("");
            warningShown = true;
        }

        // Check for specific minor/micro versions
        if (isSpecificVersion(version)) {
            getLog().warn("");
            getLog().warn("WARNING: " + jdkType + " " + version + " uses a specific minor/micro version.");
            getLog().warn("SDKMAN may drop specific versions over time, especially older ones.");
            getLog().warn("");
            getLog().warn("For better long-term stability, consider:");
            getLog().warn("1. Use major version only (e.g., '" + getMajorVersion(version) + "') for latest patches");
            getLog().warn("2. Resolve the exact URL and use -DjdkDistributionUrl=https://...");
            getLog().warn("3. Use an LTS version for production builds");
            getLog().warn("");
            warningShown = true;
        }

        return warningShown;
    }

    /**
     * Check if a JDK version is non-LTS by querying the Disco API.
     * Uses caching to avoid repeated API calls.
     */
    private boolean isNonLtsVersion(String version) {
        String majorVersion = getMajorVersion(version);
        try {
            int major = Integer.parseInt(majorVersion);
            Set<Integer> ltsVersions = getLtsVersionsFromDiscoApi();
            return !ltsVersions.contains(major);
        } catch (NumberFormatException e) {
            return false; // If we can't parse, don't warn
        }
    }

    /**
     * Cache for LTS versions to avoid repeated API calls
     */
    private static Set<Integer> ltsVersionsCache = null;

    /**
     * Get LTS versions from Disco API with caching.
     * Falls back to hardcoded list if API is unavailable.
     */
    private Set<Integer> getLtsVersionsFromDiscoApi() {
        if (ltsVersionsCache != null) {
            return ltsVersionsCache;
        }

        Set<Integer> ltsVersions = new HashSet<>();

        try {
            URL url = new URL(DISCO_API_BASE_URL + "/major_versions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 second timeout
            connection.setReadTimeout(10000); // 10 second timeout

            if (connection.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Parse JSON response to extract LTS versions
                    // Look for "major_version": X, "term_of_support": "LTS"
                    Pattern pattern =
                            Pattern.compile("\"major_version\":\\s*(\\d+)[^}]*\"term_of_support\":\\s*\"LTS\"");
                    Matcher matcher = pattern.matcher(response.toString());

                    while (matcher.find()) {
                        int majorVersion = Integer.parseInt(matcher.group(1));
                        ltsVersions.add(majorVersion);
                    }

                    getLog().debug("Retrieved LTS versions from Disco API: " + ltsVersions);
                }
            } else {
                getLog().warn("Failed to fetch LTS versions from Disco API (HTTP " + connection.getResponseCode()
                        + "), using fallback");
                ltsVersions = getFallbackLtsVersions();
            }
        } catch (IOException e) {
            getLog().warn("Failed to fetch LTS versions from Disco API: " + e.getMessage() + ", using fallback");
            ltsVersions = getFallbackLtsVersions();
        }

        // Cache the result
        ltsVersionsCache = ltsVersions;
        return ltsVersions;
    }

    /**
     * Fallback LTS versions if Disco API is unavailable.
     * Based on Oracle's LTS schedule: 8, 11, 17, 21, 25, 29, 33...
     */
    private Set<Integer> getFallbackLtsVersions() {
        Set<Integer> fallback = new HashSet<>();
        fallback.add(8);
        fallback.add(11);
        fallback.add(17);
        fallback.add(21);
        return fallback;
    }

    /**
     * Format LTS versions for display in warning messages.
     */
    private String formatLtsVersions(Set<Integer> ltsVersions) {
        return ltsVersions.stream()
                .sorted()
                .map(String::valueOf)
                .reduce((a, b) -> a + ", " + b)
                .orElse("8, 11, 17, 21");
    }

    private boolean isSpecificVersion(String version) {
        // Check if version contains dots (e.g., "17.0.14" vs "17")
        return version != null && version.contains(".");
    }

    private String getMajorVersion(String version) {
        if (version == null) {
            return "";
        }
        int dotIndex = version.indexOf('.');
        return dotIndex > 0 ? version.substring(0, dotIndex) : version;
    }
}
