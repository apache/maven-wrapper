<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# JDK Management in Maven Wrapper

This document describes the enhanced JDK management capabilities added to Maven Wrapper for the `only-script` distribution type, allowing projects to automatically download and manage JDK installations using the [Foojay Disco API](https://api.foojay.io/disco/v3.0).

**Important**: JDK management is **only available for the `only-script` distribution type**. This design choice avoids the chicken-and-egg problem where Java is needed to download Java.

## Overview

The Maven Wrapper `only-script` distribution now supports:

- 🚀 **Automatic JDK download and installation** via Foojay Disco API
- 📦 **JDK version management via maven-wrapper.properties**
- 🔧 **Toolchain JDK support for multi-JDK builds**
- 🔒 **SHA-256 checksum verification for security**
- 🌍 **Cross-platform support (Windows, macOS, Linux)**
- ⚙️ **Environment variable configuration**
- ↩️ **Backward compatibility with existing configurations**

## Getting Started

```bash
# Generate wrapper with JDK management support
mvn wrapper:wrapper -Dtype=only-script -Djdk=17 -DjdkDistribution=temurin
```

The `only-script` distribution uses shell scripts (Unix) and PowerShell (Windows) to handle JDK download and installation directly, without requiring Java to be pre-installed.

## Configuration

JDK settings can be configured in two ways:
1. **Properties file**: Add settings to `.mvn/wrapper/maven-wrapper.properties`
2. **Environment variables**: Override properties with `MVNW_JDK_*` prefixed environment variables

### Environment Variables

Environment variables take precedence over properties file settings:

```bash
# Basic JDK configuration
export MVNW_JDK_VERSION=17                    # Override jdkVersion
export MVNW_JDK_DISTRIBUTION=corretto         # Override jdkDistribution
export MVNW_JDK_DISTRIBUTION_URL=https://...  # Override jdkDistributionUrl
export MVNW_JDK_SHA256_SUM=abc123...          # Override jdkSha256Sum
export MVNW_JDK_UPDATE_POLICY=weekly          # Override jdkUpdatePolicy
export MVNW_ALWAYS_DOWNLOAD_JDK=true          # Override alwaysDownloadJdk

# Toolchain JDK configuration
export MVNW_TOOLCHAIN_JDK_VERSION=11          # Override toolchainJdkVersion
export MVNW_TOOLCHAIN_JDK_DISTRIBUTION=zulu   # Override toolchainJdkDistribution
export MVNW_TOOLCHAIN_JDK_DISTRIBUTION_URL=https://...  # Override toolchainJdkDistributionUrl
export MVNW_TOOLCHAIN_JDK_SHA256_SUM=def456...          # Override toolchainJdkSha256Sum

# Skip JDK management entirely
export MVNW_SKIP_JDK=true                     # Use system JDK instead
```

**Windows (PowerShell):**
```powershell
$env:MVNW_JDK_VERSION = "17"
$env:MVNW_JDK_DISTRIBUTION = "corretto"
$env:MVNW_SKIP_JDK = "true"
```

### Basic JDK Configuration

Add JDK configuration to your `.mvn/wrapper/maven-wrapper.properties` file:

```properties
# JDK Management
jdkVersion=17                # Resolves to latest 17.x (e.g., 17.0.14)
jdkDistribution=temurin      # Distribution name from Disco API (default: temurin)

# Optional: Update policy (Maven-style)
jdkUpdatePolicy=daily        # never, daily, always, interval:X

# Optional: Direct URL (overrides version/distribution resolution)
jdkDistributionUrl=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.7%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.7_7.tar.gz

# Optional: SHA-256 checksum for security
jdkSha256Sum=aee68e7a34c7c6239d65b3bfbf0ca8f0b5b5b6e8e8e8e8e8e8e8e8e8e8e8e8e8

# Optional: Force re-download
alwaysDownloadJdk=false
```

### Toolchain JDK Configuration

For multi-JDK builds using Maven toolchains:

```properties
# Toolchain JDK (automatically added to toolchains.xml)
toolchainJdkVersion=11
toolchainJdkDistribution=temurin      # Distribution name from Disco API (default: temurin)

# Optional: Direct URL (overrides version/distribution resolution)
toolchainJdkDistributionUrl=https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.19%2B7/OpenJDK11U-jdk_x64_linux_hotspot_11.0.19_7.tar.gz

# Optional: SHA-256 checksum for security
toolchainJdkSha256Sum=aee68e7a34c7c6239d65b3bfbf0ca8f0b5b5b6e8e8e8e8e8e8e8e8e8e8e8e8e8
```

## JDK Version Syntax

The `jdkVersion` and `toolchainJdkVersion` properties support flexible version formats via Foojay Disco API integration:

### Major Version (Recommended)
```properties
# Resolves to latest patch version automatically
jdkVersion=17          # → 17.0.14+7 (latest 17.x) - LTS
jdkVersion=21          # → 21.0.7+7 (latest 21.x) - LTS
jdkVersion=22          # → 22.0.2+7 (latest 22.x) - Non-LTS (warning shown)
```

**LTS vs Non-LTS Versions:**
- **LTS (Long Term Support)**: 8, 11, 17, 21, 25 (Sept 2025), 29, 33...
- **Non-LTS**: 9, 10, 12-16, 18-20, 22-24, 26-28...
- **Recommendation**: Use LTS versions for production, or direct URLs for non-LTS

### Full Version (Specific)
```properties
# Use exact version if needed
jdkVersion=17.0.14     # → Exact version 17.0.14
jdkVersion=21.0.6      # → Exact version 21.0.6
jdkVersion=22.0.1      # → Exact version 22.0.1
```

### Version Examples by Distribution
```properties
# Temurin (Eclipse Adoptium) - default distribution
jdkVersion=17          # → 17.0.14+7-tem
jdkVersion=21          # → 21.0.7+7-tem

# Oracle OpenJDK
jdkVersion=22          # → 22.0.2-oracle
jdkDistribution=oracle_open_jdk

# Amazon Corretto
jdkVersion=17          # → 17.0.15-amzn
jdkDistribution=corretto
```

**Note**: Major version resolution (e.g., `17` → `17.0.14+7`) is recommended as it automatically provides the latest security patches and bug fixes.

## Supported JDK Distributions

The Maven Wrapper supports multiple JDK distributions through the [Foojay Disco API](https://api.foojay.io/disco/v3.0/distributions) using native distribution names:

### Popular Distributions

| Distribution | Description | Use Case |
|-------------|-------------|----------|
| `temurin` | Eclipse Adoptium (default) | General purpose, excellent support |
| `corretto` | Amazon Corretto | AWS environments, enterprise |
| `zulu` | Azul Zulu | Commercial support available |
| `liberica` | BellSoft Liberica | Lightweight, embedded systems |
| `oracle_open_jdk` | Oracle OpenJDK | Oracle environments |
| `microsoft` | Microsoft OpenJDK | Azure, Windows environments |
| `semeru` | IBM Semeru | IBM environments, OpenJ9 JVM |

### Specialized Distributions

| Distribution | Description | Use Case |
|-------------|-------------|----------|
| `graalvm_ce11` | GraalVM CE 11 | Native compilation, polyglot |
| `graalvm_ce17` | GraalVM CE 17 | Native compilation, polyglot |
| `sap_machine` | SAP Machine | SAP environments |
| `dragonwell` | Alibaba Dragonwell | Cloud-native, Chinese market |
| `jetbrains` | JetBrains Runtime | IDE development |
| `bisheng` | Huawei BiSheng | ARM64 optimization |
| `kona` | Tencent Kona | Tencent cloud environments |
| `mandrel` | Red Hat Mandrel | Native compilation |

**Complete List**: For all 34+ supported distributions, see: [Foojay Disco API](https://api.foojay.io/disco/v3.0/distributions)

## JDK Version Formats

You can specify JDK versions in several formats:

- **Major version**: `17`, `21`, `11` (resolves to latest patch version via Foojay Disco API)
- **Specific version**: `17.0.7`, `21.0.1`, `11.0.19` (used exactly as specified)
- **Full version**: `17.0.7+7`, `21.0.1+12` (includes build number)

## Update Policies (Maven-Style)

Control how often major versions are resolved to latest patch versions:

- **`never`**: Resolve once, cache forever (good for CI/production)
- **`daily`**: Check for updates once per day (default, like Maven)
- **`always`**: Check on every run (for development/testing)
- **`weekly`**: Check for updates once per week
- **`monthly`**: Check for updates once per month
- **`interval:X`**: Check every X minutes (e.g., `interval:60` for hourly)

```properties
# Examples
jdkUpdatePolicy=never        # Resolve 17 -> 17.0.14 once, never update
jdkUpdatePolicy=daily        # Check daily for newer 17.x versions (default)
jdkUpdatePolicy=weekly       # Check weekly for newer 17.x versions
jdkUpdatePolicy=always       # Always get latest 17.x (slower, no caching)
jdkUpdatePolicy=interval:60  # Check every hour
jdkUpdatePolicy=interval:10  # Check every 10 minutes
```

### Update Policy Use Cases

#### Production/CI Environments
```properties
# Pin to exact version for reproducible builds
jdkVersion=17.0.14
jdkDistribution=temurin
jdkUpdatePolicy=never

# OR use major version with no updates
jdkVersion=17
jdkDistribution=temurin
jdkUpdatePolicy=never
```

#### Development Environments
```properties
# Get latest patches automatically (default)
jdkVersion=17
jdkDistribution=temurin
jdkUpdatePolicy=daily

# Conservative approach
jdkVersion=17
jdkDistribution=temurin
jdkUpdatePolicy=weekly
```

#### Testing/Validation Environments
```properties
# Always get the latest for testing
jdkVersion=17
jdkDistribution=temurin
jdkUpdatePolicy=always

# Custom validation schedule
jdkVersion=17
jdkDistribution=temurin
jdkUpdatePolicy=interval:30  # Check every 30 minutes
```

### Update Timeline Examples

**Daily Policy (Default):**
- Day 1: Downloads JDK 17.0.14+7, caches for 24h
- Day 2: Uses cached 17.0.14+7 (no API call)
- Day 3: Cache expired → Queries Disco API → Gets 17.0.15+7 (if available)

**Never Policy (Production):**
- Week 1: Downloads JDK 17.0.14+7, caches permanently
- Week 2+: Always uses cached 17.0.14+7 (reproducible builds)

**Always Policy (Testing):**
- Every run: Queries Disco API for latest 17.x version
- No caching: Always gets the newest available patch

## JDK Version Stability Warnings

When configuring JDK versions using the Maven plugin, warnings are displayed at **installation time** for potentially unstable configurations:

### Non-LTS Version Warning
```
WARNING: Main JDK 22 is not an LTS (Long Term Support) version.
Non-LTS versions may be removed from distribution repositories when newer versions are released,
which could break your builds in the future.

For better long-term stability, consider:
1. Switch to an LTS version: 8, 11, 17, 21, or 25 (Sept 2025)
2. Use a direct URL with -DjdkDistributionUrl=https://...
3. Pin to exact version and resolve URL explicitly

To suppress these warnings, use -DskipJdkWarnings=true
```

### Specific Version Warning
```
WARNING: Main JDK 17.0.14 uses a specific minor/micro version.
Distribution repositories may drop specific versions over time, especially older ones.

For better long-term stability, consider:
1. Use major version only (e.g., '17') for latest patches
2. Resolve the exact URL and use -DjdkDistributionUrl=https://...
3. Use an LTS version for production builds

To suppress these warnings, use -DskipJdkWarnings=true
```

### LTS Version Timeline
- **JDK 8**: LTS (March 2014) - Extended support
- **JDK 11**: LTS (September 2018) - Extended support
- **JDK 17**: LTS (September 2021) - Current LTS
- **JDK 21**: LTS (September 2023) - Current LTS
- **JDK 25**: LTS (September 2025) - Next LTS
- **JDK 29**: LTS (September 2027) - Future LTS

### Recommendations
- **Production**: Use LTS versions (8, 11, 17, 21) or direct URLs
- **Development**: LTS versions recommended, non-LTS acceptable with warnings
- **Long-term projects**: Always use direct URLs for non-LTS versions

## Runtime Control Variables

Additional environment variables for runtime control:

```bash
# Runtime Control
export MVNW_SKIP_JDK=true          # Skip JDK installation, use system JDK
export MVNW_VERBOSE=true           # Enable verbose output
export MAVEN_USER_HOME=~/.m2       # Override Maven user directory
```

### Bypassing JDK Selection

Set `MVNW_SKIP_JDK` to bypass automatic JDK installation and use the system JDK instead:

```bash
# Use system JDK instead of wrapper-managed JDK
export MVNW_SKIP_JDK=true
./mvnw clean compile

# Useful for CI matrix testing with different JDK versions
MVNW_SKIP_JDK=true ./mvnw test

# Windows PowerShell
$env:MVNW_SKIP_JDK = "true"
.\mvnw.cmd test
```

**Use Cases for MVNW_SKIP_JDK:**
- **CI Matrix Testing**: Test with multiple JDK versions using build matrix
- **System JDK Override**: Use pre-installed JDK instead of wrapper-managed
- **Debugging**: Temporarily bypass JDK management for troubleshooting
- **Performance**: Skip JDK download/setup in environments where it's not needed

## Maven Plugin Usage

Use the Maven Wrapper plugin to configure JDK settings (requires `only-script` distribution type):

```bash
# Set JDK version via plugin (must specify only-script type)
mvn wrapper:wrapper -Dtype=only-script -Djdk=17

# Set JDK distribution
mvn wrapper:wrapper -Dtype=only-script -Djdk=17 -DjdkDistribution=corretto

# Set toolchain JDK with distribution
mvn wrapper:wrapper -Dtype=only-script -Djdk=17 -DtoolchainJdk=11 -DtoolchainJdkDistribution=corretto

# Use direct URLs for both main and toolchain JDK
mvn wrapper:wrapper -Dtype=only-script -DjdkUrl=https://example.com/jdk-17.tar.gz -DtoolchainJdkUrl=https://example.com/jdk-11.tar.gz

# Set checksums for security
mvn wrapper:wrapper -Dtype=only-script -Djdk=17 -DjdkSha256Sum=abc123... -DtoolchainJdk=11 -DtoolchainJdkSha256Sum=def456...

# Configure update policy
mvn wrapper:wrapper -Dtype=only-script -Djdk=17 -DjdkUpdatePolicy=weekly

# Production setup with no updates
mvn wrapper:wrapper -Dtype=only-script -Djdk=17.0.14 -DjdkUpdatePolicy=never

# Suppress JDK version stability warnings
mvn wrapper:wrapper -Dtype=only-script -Djdk=22 -DskipJdkWarnings=true

# Use non-LTS version without warnings
mvn wrapper:wrapper -Dtype=only-script -Djdk=22.0.2 -DskipJdkWarnings=true
```

## Toolchains Integration

When JDKs are downloaded, they are automatically added to `~/.m2/toolchains.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>temurin</vendor>
    </provides>
    <configuration>
      <jdkHome>/path/to/downloaded/jdk-17</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

## Usage Examples

### Simple JDK Download

```bash
# Configure wrapper to use JDK 17
echo "jdkVersion=17" >> .mvn/wrapper/maven-wrapper.properties

# Run Maven - JDK will be downloaded automatically
./mvnw clean compile
```

### Multi-JDK Project

```bash
# Configure main JDK and toolchain JDK with different distributions
cat >> .mvn/wrapper/maven-wrapper.properties << EOF
jdkVersion=21
jdkDistribution=oracle_open_jdk
jdkUpdatePolicy=weekly
toolchainJdkVersion=17
toolchainJdkDistribution=temurin
EOF

# Configure Maven Compiler Plugin to use toolchain
cat >> pom.xml << EOF
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-toolchains-plugin</artifactId>
  <version>3.1.0</version>
  <executions>
    <execution>
      <goals>
        <goal>toolchain</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <toolchains>
      <jdk>
        <version>17</version>
        <vendor>temurin</vendor>
      </jdk>
    </toolchains>
  </configuration>
</plugin>
EOF

# Run Maven - both JDKs will be downloaded and configured
./mvnw clean compile
```

### Distribution-Specific JDK

```bash
# Use Amazon Corretto JDK 17
mvn wrapper:wrapper -Dtype=only-script -Djdk=17 -DjdkDistribution=corretto

# Use Azul Zulu JDK 21
mvn wrapper:wrapper -Dtype=only-script -Djdk=21 -DjdkDistribution=zulu
```

### Update Policy Scenarios

#### Production Environment
```bash
# Pin to exact version, never update
mvn wrapper:wrapper -Dtype=only-script -Djdk=17.0.14 -DjdkUpdatePolicy=never

# OR use major version with no updates
cat >> .mvn/wrapper/maven-wrapper.properties << EOF
jdkVersion=17
jdkDistribution=temurin
jdkUpdatePolicy=never
EOF
```

#### Development Environment
```bash
# Get latest patches daily (default behavior)
mvn wrapper:wrapper -Dtype=only-script -Djdk=17

# Conservative weekly updates
cat >> .mvn/wrapper/maven-wrapper.properties << EOF
jdkVersion=17
jdkDistribution=temurin
jdkUpdatePolicy=weekly
EOF
```

#### CI/CD Environment
```bash
# Monthly updates for stability
cat >> .mvn/wrapper/maven-wrapper.properties << EOF
jdkVersion=17
jdkDistribution=temurin
jdkUpdatePolicy=monthly
EOF

# Custom interval for specific workflows
cat >> .mvn/wrapper/maven-wrapper.properties << EOF
jdkVersion=17
jdkDistribution=temurin
jdkUpdatePolicy=interval:120  # Check every 2 hours
EOF
```

#### Testing Environment
```bash
# Always get latest for validation
cat >> .mvn/wrapper/maven-wrapper.properties << EOF
jdkVersion=17
jdkDistribution=temurin
jdkUpdatePolicy=always
EOF
```

## Reliability & Resilience

The Maven Wrapper includes robust error handling and retry mechanisms for reliable JDK downloads:

### HTTP Retry Strategy
- **Exponential backoff**: 2s, 4s, 8s delays with 0-20% random jitter
- **Intelligent retry**: Only retries on 5xx server errors (503, 502, etc.)
- **No retry on client errors**: 4xx errors (404, 401) fail immediately
- **Consistent across HTTP clients**: Same retry logic for curl, wget, and PowerShell
- **Prevents thundering herd**: Random jitter prevents synchronized retry attempts

### Error Handling
- **Detailed error messages**: HTTP status codes and specific guidance
- **Graceful degradation**: Falls back to system JDK when downloads fail
- **API resilience**: Handles temporary Disco API unavailability
- **Network tolerance**: Robust handling of network timeouts and interruptions

### Fallback Mechanisms
- **Multiple HTTP clients**: Supports curl, wget, and busybox wget
- **System JDK bypass**: `MVNW_SKIP_JDK=true` for emergency situations
- **Direct URL support**: Bypass API resolution when needed
- **Offline mode**: Works with pre-downloaded JDK installations

## Security

- All JDK downloads support SHA-256 checksum verification
- Checksums are automatically resolved when using version/distribution specification
- Manual checksum specification is supported for direct URLs
- Downloads use HTTPS by default

## Platform Support

The JDK management feature supports:

- **Linux**: x64, aarch64
- **Windows**: x64
- **macOS**: x64, aarch64 (Apple Silicon)

Archive formats supported:
- ZIP files (`.zip`)
- TAR.GZ files (`.tar.gz`, `.tgz`)

## Troubleshooting

### JDK Download Fails

1. Check internet connectivity
2. Verify JDK version and distribution are supported
3. Check firewall/proxy settings
4. Verify SHA-256 checksum if specified

### Toolchain Not Found

1. Ensure toolchain JDK is configured and downloaded
2. Check `~/.m2/toolchains.xml` exists and contains the JDK entry
3. Verify Maven Toolchains Plugin configuration

### Permission Issues

1. Ensure write permissions to Maven user home directory
2. Check JDK installation directory permissions
3. Verify executable permissions on JDK binaries

### Bypassing JDK Management

If you encounter issues with automatic JDK management, you can bypass it entirely:

```bash
# Skip JDK management and use system JDK
export MVNW_SKIP_JDK=true
./mvnw clean compile

# Windows
set MVNW_SKIP_JDK=true
mvnw.cmd clean compile
```

**When to use MVNW_SKIP_JDK:**
- JDK download fails due to network issues
- Need to use a specific system-installed JDK
- CI environments with pre-installed JDKs
- Debugging JDK-related issues
- Performance optimization (skip JDK setup time)

## Migration from Existing Setups

The JDK management feature is fully backward compatible. Existing Maven Wrapper configurations will continue to work without changes.

To migrate to automatic JDK management:

1. **Switch to `only-script` distribution type** (required for JDK management)
2. Add `jdkVersion` to your `maven-wrapper.properties`
3. Optionally specify `jdkDistribution` (defaults to Temurin)
4. Remove manual JDK installation steps from your build process
5. Update documentation to reference the new automatic JDK management

```bash
# Migration command
mvn wrapper:wrapper -Dtype=only-script -Djdk=17 -DjdkDistribution=temurin
```

## Implementation Details

The JDK management feature is implemented entirely in shell scripts for the `only-script` distribution type:

- **Shell Scripts**: `only-mvnw` (Unix/Linux/macOS) and `only-mvnw.cmd` (Windows)
- **Foojay Disco API Integration**: Direct HTTP calls using `curl`/`wget` and PowerShell
- **Version Resolution**: Major version (17) → Latest patch version (17.0.14)
- **Archive Extraction**: Native `tar`/`unzip` commands with cross-platform support
- **Toolchain Integration**: Direct XML manipulation for `~/.m2/toolchains.xml`

This shell-based approach avoids the chicken-and-egg problem of needing Java to download Java, providing a self-contained solution that works without any Java runtime pre-installed.

### Foojay Disco API Benefits

The Maven Wrapper uses the [Foojay Disco API](https://api.foojay.io/disco/docs) for JDK resolution, which provides several advantages:

- **🏢 Professional Backing**: Eclipse Foundation governance ensures long-term stability
- **📊 Comprehensive Coverage**: 34+ JDK distributions including enterprise distributions
- **🔗 Direct Downloads**: Single API call returns direct download URLs
- **🏗️ Structured Responses**: JSON-based API with rich metadata
- **🔍 Advanced Filtering**: Precise control over JDK selection criteria
- **📋 Enhanced Metadata**: TCK compliance, checksums, and security information
- **⚡ Better Performance**: Optimized for fewer network round trips
