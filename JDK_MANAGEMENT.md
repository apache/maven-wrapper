# JDK Management in Maven Wrapper

This document describes the enhanced JDK management capabilities added to Maven Wrapper, allowing projects to automatically download and manage JDK installations.

## Overview

The Maven Wrapper now supports:

- 🚀 **Automatic JDK download and installation**
- 📦 **JDK version management via maven-wrapper.properties**
- 🔧 **Toolchain JDK support for multi-JDK builds**
- 🔒 **SHA-256 checksum verification for security**
- 🌍 **Cross-platform support (Windows, macOS, Linux)**
- ⚙️ **Environment variable configuration**
- ↩️ **Backward compatibility with existing configurations**

## Configuration

### Basic JDK Configuration

Add JDK configuration to your `.mvn/wrapper/maven-wrapper.properties` file:

```properties
# JDK Management
jdkVersion=17          # Resolves to latest 17.x (e.g., 17.0.14)
jdkVendor=temurin

# Optional: Update policy (Maven-style)
jdkUpdatePolicy=daily  # never, daily, always, interval:X

# Optional: Direct URL (overrides version/vendor resolution)
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
toolchainJdkVendor=temurin
toolchainJdkDistributionUrl=https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.19%2B7/OpenJDK11U-jdk_x64_linux_hotspot_11.0.19_7.tar.gz
toolchainJdkSha256Sum=...
```

## Supported JDK Vendors

The following JDK vendors are supported:

| Vendor | Aliases | Description |
|--------|---------|-------------|
| `temurin` | `adoptium`, `adoptopenjdk`, `eclipse` | Eclipse Temurin (default) |
| `corretto` | `amazon`, `aws` | Amazon Corretto |
| `zulu` | `azul` | Azul Zulu |
| `liberica` | `bellsoft` | BellSoft Liberica |
| `oracle` | | Oracle JDK |
| `microsoft` | `ms` | Microsoft Build of OpenJDK |
| `semeru` | `ibm` | IBM Semeru |
| `graalvm` | `graal` | GraalVM |

## JDK Version Formats

You can specify JDK versions in several formats:

- **Major version**: `17`, `21`, `11` (resolves to latest patch version via SDKMAN API)
- **Specific version**: `17.0.7`, `21.0.1`, `11.0.19` (used exactly as specified)
- **Full version**: `17.0.7+7`, `21.0.1+12` (includes build number)

## Update Policies (Maven-Style)

Control how often major versions are resolved to latest patch versions:

- **`never`**: Resolve once, cache forever (good for CI/production)
- **`daily`**: Check for updates once per day (default, like Maven)
- **`always`**: Check on every run (for development/testing)
- **`interval:X`**: Check every X minutes (e.g., `interval:60` for hourly)

```properties
# Examples
jdkUpdatePolicy=never     # Resolve 17 -> 17.0.14 once, never update
jdkUpdatePolicy=daily     # Check daily for newer 17.x versions
jdkUpdatePolicy=always    # Always get latest 17.x (slower)
jdkUpdatePolicy=interval:60  # Check every hour
```

## Environment Variables

Configure JDK settings via environment variables:

```bash
export MAVEN_WRAPPER_JDK_VERSION=17
export MAVEN_WRAPPER_JDK_VENDOR=temurin
export MAVEN_WRAPPER_JDK_DOWNLOAD=true
export MAVEN_WRAPPER_TOOLCHAIN_JDK=11
```

## Maven Plugin Usage

Use the Maven Wrapper plugin to configure JDK settings:

```bash
# Set JDK version via plugin
mvn wrapper:wrapper -Djdk=17

# Set JDK vendor
mvn wrapper:wrapper -Djdk=17 -DjdkVendor=corretto

# Set toolchain JDK
mvn wrapper:wrapper -Djdk=17 -DtoolchainJdk=11

# Use direct URL
mvn wrapper:wrapper -DjdkUrl=https://example.com/jdk-17.tar.gz
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
# Configure main JDK and toolchain JDK
cat >> .mvn/wrapper/maven-wrapper.properties << EOF
jdkVersion=21
toolchainJdkVersion=17
toolchainJdkVendor=temurin
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

### Vendor-Specific JDK

```bash
# Use Amazon Corretto JDK 17
mvn wrapper:wrapper -Djdk=17 -DjdkVendor=corretto

# Use Azul Zulu JDK 21
mvn wrapper:wrapper -Djdk=21 -DjdkVendor=zulu
```

## Security

- All JDK downloads support SHA-256 checksum verification
- Checksums are automatically resolved when using version/vendor specification
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
2. Verify JDK version and vendor are supported
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

## Migration from Existing Setups

The JDK management feature is fully backward compatible. Existing Maven Wrapper configurations will continue to work without changes.

To migrate to automatic JDK management:

1. Add `jdkVersion` to your `maven-wrapper.properties`
2. Optionally specify `jdkVendor` (defaults to Temurin)
3. Remove manual JDK installation steps from your build process
4. Update documentation to reference the new automatic JDK management

## Implementation Details

The JDK management feature consists of several components:

- **JdkResolver**: Resolves JDK versions to download URLs
- **JdkDownloader**: Downloads and installs JDKs
- **ToolchainManager**: Manages toolchains.xml integration
- **BinaryDownloader**: Generic binary download and extraction

These components work together to provide a seamless JDK management experience while maintaining the simplicity and reliability of Maven Wrapper.
