<# : batch portion
@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version @@project.version@@
@REM
@REM Optional ENV vars
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM   MVNW_SKIP_JDK - true: skip JDK installation and management (use system JDK)
@REM
@REM JDK Management ENV vars (override maven-wrapper.properties)
@REM   MVNW_JDK_VERSION - JDK version to download and use (e.g., 17, 21, 17.0.14)
@REM   MVNW_JDK_DISTRIBUTION - JDK distribution name (e.g., temurin, corretto, zulu)
@REM   MVNW_JDK_DISTRIBUTION_URL - Direct URL to JDK archive (overrides version/distribution)
@REM   MVNW_JDK_SHA256_SUM - SHA-256 checksum for JDK archive verification
@REM   MVNW_JDK_UPDATE_POLICY - Update policy (never, daily, weekly, monthly, always, interval:X)
@REM   MVNW_ALWAYS_DOWNLOAD_JDK - Force re-download of JDK (true/false)
@REM   MVNW_TOOLCHAIN_JDK_VERSION - Toolchain JDK version (e.g., 11, 8)
@REM   MVNW_TOOLCHAIN_JDK_DISTRIBUTION - Toolchain JDK distribution name
@REM   MVNW_TOOLCHAIN_JDK_DISTRIBUTION_URL - Direct URL to toolchain JDK archive
@REM   MVNW_TOOLCHAIN_JDK_SHA256_SUM - SHA-256 checksum for toolchain JDK archive
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $script='%__MVNW_ARG0_NAME__%'; icm -ScriptBlock ([Scriptblock]::Create((Get-Content -Raw '%~f0'))) -NoNewScope}"`) DO @(
  IF "%%A"=="MVN_CMD" (set __MVNW_CMD__=%%B) ELSE IF "%%B"=="" (echo %%A) ELSE (echo %%A=%%B)
)
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@SET MVNW_USERNAME=
@SET MVNW_PASSWORD=
@IF NOT "%__MVNW_CMD__%"=="" ("%__MVNW_CMD__%" %*)
@echo Cannot start maven from wrapper >&2 && exit /b 1
@GOTO :EOF
: end batch / begin powershell #>

$ErrorActionPreference = "Stop"
if ($env:MVNW_VERBOSE -eq "true") {
  $VerbosePreference = "Continue"
}

# calculate distributionUrl, requires .mvn/wrapper/maven-wrapper.properties
$wrapperProperties = Get-Content -Raw "$scriptDir/.mvn/wrapper/maven-wrapper.properties" | ConvertFrom-StringData
$distributionUrl = $wrapperProperties.distributionUrl
if (!$distributionUrl) {
  Write-Error "cannot read distributionUrl property in $scriptDir/.mvn/wrapper/maven-wrapper.properties"
}

# Read JDK-related properties
$jdkVersion = $wrapperProperties.jdkVersion
$jdkDistribution = $wrapperProperties.jdkDistribution
$jdkDistributionUrl = $wrapperProperties.jdkDistributionUrl
$jdkSha256Sum = $wrapperProperties.jdkSha256Sum
$jdkUpdatePolicy = $wrapperProperties.jdkUpdatePolicy
$alwaysDownloadJdk = $wrapperProperties.alwaysDownloadJdk
$toolchainJdkVersion = $wrapperProperties.toolchainJdkVersion
$toolchainJdkDistribution = $wrapperProperties.toolchainJdkDistribution
$toolchainJdkDistributionUrl = $wrapperProperties.toolchainJdkDistributionUrl
$toolchainJdkSha256Sum = $wrapperProperties.toolchainJdkSha256Sum

# Disco API constants
$DISCO_API_BASE_URL = "https://api.foojay.io/disco/v3.0"

# Override JDK properties with environment variables if set
if ($env:MVNW_JDK_VERSION) { $jdkVersion = $env:MVNW_JDK_VERSION }
if ($env:MVNW_JDK_DISTRIBUTION) { $jdkDistribution = $env:MVNW_JDK_DISTRIBUTION }
if ($env:MVNW_JDK_DISTRIBUTION_URL) { $jdkDistributionUrl = $env:MVNW_JDK_DISTRIBUTION_URL }
if ($env:MVNW_JDK_SHA256_SUM) { $jdkSha256Sum = $env:MVNW_JDK_SHA256_SUM }
if ($env:MVNW_JDK_UPDATE_POLICY) { $jdkUpdatePolicy = $env:MVNW_JDK_UPDATE_POLICY }
if ($env:MVNW_ALWAYS_DOWNLOAD_JDK) { $alwaysDownloadJdk = $env:MVNW_ALWAYS_DOWNLOAD_JDK }
if ($env:MVNW_TOOLCHAIN_JDK_VERSION) { $toolchainJdkVersion = $env:MVNW_TOOLCHAIN_JDK_VERSION }
if ($env:MVNW_TOOLCHAIN_JDK_DISTRIBUTION) { $toolchainJdkDistribution = $env:MVNW_TOOLCHAIN_JDK_DISTRIBUTION }
if ($env:MVNW_TOOLCHAIN_JDK_DISTRIBUTION_URL) { $toolchainJdkDistributionUrl = $env:MVNW_TOOLCHAIN_JDK_DISTRIBUTION_URL }
if ($env:MVNW_TOOLCHAIN_JDK_SHA256_SUM) { $toolchainJdkSha256Sum = $env:MVNW_TOOLCHAIN_JDK_SHA256_SUM }

# Set default distribution if not specified
if (-not $jdkDistribution) { $jdkDistribution = "temurin" }
if (-not $toolchainJdkDistribution) { $toolchainJdkDistribution = "temurin" }

switch -wildcard -casesensitive ( $($distributionUrl -replace '^.*/','') ) {
  "maven-mvnd-*" {
    $USE_MVND = $true
    $distributionUrl = $distributionUrl -replace '-bin\.[^.]*$',"-windows-amd64.zip"
    $MVN_CMD = "mvnd.cmd"
    break
  }
  default {
    $USE_MVND = $false
    $MVN_CMD = $script -replace '^mvnw','mvn'
    break
  }
}

# apply MVNW_REPOURL and calculate MAVEN_HOME
# maven home pattern: ~/.m2/wrapper/dists/{apache-maven-<version>,maven-mvnd-<version>-<platform>}/<hash>
if ($env:MVNW_REPOURL) {
  $MVNW_REPO_PATTERN = if ($USE_MVND -eq $False) { "/org/apache/maven/" } else { "/maven/mvnd/" }
  $distributionUrl = "$env:MVNW_REPOURL$MVNW_REPO_PATTERN$($distributionUrl -replace "^.*$MVNW_REPO_PATTERN",'')"
}
$distributionUrlName = $distributionUrl -replace '^.*/',''
$distributionUrlNameMain = $distributionUrlName -replace '\.[^.]*$','' -replace '-bin$',''

$MAVEN_M2_PATH = "$HOME/.m2"
if ($env:MAVEN_USER_HOME) {
  $MAVEN_M2_PATH = "$env:MAVEN_USER_HOME"
}

if (-not (Test-Path -Path $MAVEN_M2_PATH)) {
    New-Item -Path $MAVEN_M2_PATH -ItemType Directory | Out-Null
}

$MAVEN_WRAPPER_DISTS = $null
if ((Get-Item -Path $MAVEN_M2_PATH -Force).Target[0] -eq $null) {
  $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
} else {
  $MAVEN_WRAPPER_DISTS = (Get-Item -Path $MAVEN_M2_PATH -Force).Target[0] + "/wrapper/dists"
}

$MAVEN_HOME_PARENT = "$MAVEN_WRAPPER_DISTS/$distributionUrlNameMain"
$MAVEN_HOME_NAME = ([System.Security.Cryptography.SHA256]::Create().ComputeHash([byte[]][char[]]$distributionUrl) | ForEach-Object {$_.ToString("x2")}) -join ''
$MAVEN_HOME = "$MAVEN_HOME_PARENT/$MAVEN_HOME_NAME"

# JDK management functions
function Get-CacheMaxAgeSeconds {
  param([string]$UpdatePolicy = "daily")

  switch ($UpdatePolicy.ToLower()) {
    "never" { return 31536000 }    # 1 year (effectively never)
    "daily" { return 86400 }       # 24 hours
    "always" { return 0 }          # Always expired
    "weekly" { return 604800 }     # 7 days
    "monthly" { return 2592000 }   # 30 days
    default {
      if ($UpdatePolicy -match '^interval:(\d+)$') {
        $minutes = [int]$matches[1]
        return $minutes * 60
      } else {
        Write-Verbose "Unknown update policy: $UpdatePolicy, using daily"
        return 86400
      }
    }
  }
}



function Install-JDK {
  param(
    [string]$Version,
    [string]$Distribution = "temurin",
    [string]$Url,
    [string]$Checksum,
    [string]$AlwaysDownload = "false"
  )

  if (!$Version) {
    return  # No JDK version specified
  }

  # Check if JDK selection should be bypassed
  if ($env:MVNW_SKIP_JDK) {
    Write-Verbose "Skipping JDK installation due to MVNW_SKIP_JDK environment variable"
    return
  }

  # Determine JDK installation directory
  $jdkDirName = "jdk-$Version-$Distribution"
  $mavenUserHome = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { "$HOME/.m2" }
  $jdkHome = "$mavenUserHome/wrapper/jdks/$jdkDirName"

  # Check if JDK already exists and we're not forcing re-download
  if ((Test-Path -Path $jdkHome -PathType Container) -and ($AlwaysDownload -ne "true")) {
    Write-Verbose "JDK $Version already installed at $jdkHome"
    $env:JAVA_HOME = $jdkHome
    return
  }

  # Resolve JDK URL if not provided using Disco API
  if (!$Url) {
    # Validate distribution name
    $validDistributions = @("aoj", "aoj_openj9", "bisheng", "corretto", "debian", "dragonwell", "gluon_graalvm", "graalvm", "graalvm_ce11", "graalvm_ce16", "graalvm_ce17", "graalvm_ce19", "graalvm_ce20", "graalvm_ce8", "graalvm_community", "jetbrains", "kona", "liberica", "liberica_native", "mandrel", "microsoft", "ojdk_build", "openlogic", "oracle", "oracle_open_jdk", "redhat", "sap_machine", "semeru", "semeru_certified", "temurin", "trava", "zulu", "zulu_prime")

    if ($validDistributions -notcontains $Distribution.ToLower()) {
      Write-Error "ERROR: Unknown JDK distribution '$Distribution'."
      Write-Error ""
      Write-Error "Available JDK distributions:"
      Write-Error "  - temurin (Eclipse Adoptium - default)"
      Write-Error "  - corretto (Amazon)"
      Write-Error "  - zulu (Azul)"
      Write-Error "  - liberica (BellSoft)"
      Write-Error "  - oracle_open_jdk (Oracle OpenJDK)"
      Write-Error "  - microsoft (Microsoft)"
      Write-Error "  - semeru (IBM)"
      Write-Error "  - graalvm_ce11 (GraalVM CE 11)"
      Write-Error "  - graalvm_ce17 (GraalVM CE 17)"
      Write-Error "  - sap_machine (SAP)"
      Write-Error "  - dragonwell (Alibaba)"
      Write-Error "  - jetbrains (JetBrains Runtime)"
      Write-Error "  - bisheng (Huawei)"
      Write-Error "  - kona (Tencent)"
      Write-Error "  - mandrel (Red Hat)"
      Write-Error "  - openlogic (OpenLogic)"
      Write-Error ""
      Write-Error "For a complete list, see: $DISCO_API_BASE_URL/distributions"
      Write-Error ""
      Write-Error "To use a different distribution, set jdkDistribution in maven-wrapper.properties:"
      Write-Error "  jdkDistribution=temurin"
      Write-Error "  jdkDistribution=corretto"
      Write-Error "  jdkDistribution=zulu"
      Write-Error ""
      Write-Error "Alternatively, specify an exact JDK URL with jdkDistributionUrl."
      return
    }

    # Handle major version resolution by querying Disco API
    $normalizedVersion = $Version
    if ($Version -match '^\d+$') {
      # This is a major version, try to get the latest from Disco API

      # Get update policy and check cache
      $updatePolicy = if ($env:jdkUpdatePolicy) { $env:jdkUpdatePolicy } else { "daily" }
      $cacheMaxAgeSeconds = Get-CacheMaxAgeSeconds -UpdatePolicy $updatePolicy
      $cacheFile = "$mavenUserHome/wrapper/cache/jdk-$Version-$Distribution.cache"
      $cacheDir = Split-Path -Path $cacheFile -Parent

      # Check cache based on update policy
      $useCachedVersion = $false
      if ((Test-Path -Path $cacheFile) -and ($cacheMaxAgeSeconds -gt 0)) {
        $cacheAge = (Get-Date) - (Get-Item $cacheFile).LastWriteTime
        if ($cacheAge.TotalSeconds -lt $cacheMaxAgeSeconds) {
          $cachedVersion = Get-Content $cacheFile -ErrorAction SilentlyContinue
          if ($cachedVersion) {
            Write-Verbose "Using cached JDK version (policy: $updatePolicy): $Version -> $cachedVersion"
            $normalizedVersion = $cachedVersion
            $useCachedVersion = $true
          }
        } else {
          Write-Verbose "Cache expired (policy: $updatePolicy, age: $($cacheAge.TotalSeconds)s, max: ${cacheMaxAgeSeconds}s)"
        }
      } elseif ($updatePolicy -eq "always") {
        Write-Verbose "Update policy 'always': skipping cache check"
      }

      if (-not $useCachedVersion) {
        try {
          # Query Disco API for the latest version
          # Determine the correct version parameter name based on version format
          if ($Version -match '^\d+$') {
            # Major version only (e.g., "17") - use jdk_version parameter
            $versionParam = "jdk_version=$Version"
          } else {
            # Specific version (e.g., "17.0.7+7") - use java_version parameter
            $encodedVersionForQuery = $Version -replace '\+', '%2B'
            $versionParam = "java_version=$encodedVersionForQuery"
          }
          $discoApiUrl = "$DISCO_API_BASE_URL/packages?distro=$Distribution&package_type=jdk&$versionParam&operating_system=windows&architecture=x64&archive_type=zip&latest=available&release_status=ga"

          Write-Verbose "Querying Disco API for JDK versions: $discoApiUrl"

          $webclientVersions = New-Object System.Net.WebClient
          [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
          $apiResponse = $webclientVersions.DownloadString($discoApiUrl)

          # Extract the java_version from the JSON response (simple parsing)
          if ($apiResponse -match '"java_version":"([^"]*)"') {
            $normalizedVersion = $matches[1]

            # Cache the result (unless policy is 'always')
            if ($updatePolicy -ne "always") {
              if (-not (Test-Path -Path $cacheDir)) {
                New-Item -ItemType Directory -Path $cacheDir -Force | Out-Null
              }
              Set-Content -Path $cacheFile -Value $normalizedVersion -ErrorAction SilentlyContinue
            }

            Write-Verbose "Resolved JDK version from Disco API (policy: $updatePolicy): $Version -> $normalizedVersion"
        } else {
          # Show helpful error message with available alternatives
          Write-Error "ERROR: JDK $Version is not available from distribution '$Distribution'."
          Write-Error ""
          Write-Error "Available JDK distributions:"
          Write-Error "  - temurin (Eclipse Adoptium - default)"
          Write-Error "  - corretto (Amazon)"
          Write-Error "  - zulu (Azul)"
          Write-Error "  - liberica (BellSoft)"
          Write-Error "  - oracle_open_jdk (Oracle OpenJDK)"
          Write-Error "  - microsoft (Microsoft)"
          Write-Error "  - semeru (IBM)"
          Write-Error "  - graalvm_ce11 (GraalVM CE 11)"
          Write-Error "  - graalvm_ce17 (GraalVM CE 17)"
          Write-Error "  - sap_machine (SAP)"
          Write-Error "  - dragonwell (Alibaba)"
          Write-Error "  - jetbrains (JetBrains Runtime)"
          Write-Error ""
          Write-Error "For a complete list, see: $DISCO_API_BASE_URL/distributions"
          Write-Error ""
          Write-Error "To use a different distribution, set jdkDistribution in maven-wrapper.properties:"
          Write-Error "  jdkDistribution=temurin"
          Write-Error "  jdkDistribution=corretto"
          Write-Error "  jdkDistribution=zulu"
          Write-Error ""
          Write-Error "Alternatively, specify an exact JDK URL with jdkDistributionUrl."
          Write-Error "Or set MVNW_SKIP_JDK=true to use system JDK."
          return
          }
        } catch {
          # Network or API error - extract HTTP status if available
          $httpStatus = ""
          if ($_.Exception -is [System.Net.WebException] -and $_.Exception.Response) {
            $httpStatus = [int]$_.Exception.Response.StatusCode
            Write-Error "Failed to resolve JDK version $Version from Disco API: HTTP $httpStatus"
            if ($httpStatus -eq 503) {
              Write-Error "The Disco API is temporarily unavailable (Service Unavailable)"
              Write-Error "This is likely a temporary issue with the Foojay Disco API service"
            } elseif ($httpStatus -eq 429) {
              Write-Error "Rate limited by Disco API (Too Many Requests)"
            } elseif ($httpStatus -eq 404) {
              Write-Error "JDK version not found (Not Found)"
            }
          } else {
            Write-Error "Failed to resolve JDK version $Version from Disco API: $($_.Exception.Message)"
          }
          Write-Error "API URL: $discoApiUrl"
          Write-Error ""
          Write-Error "This could be due to:"
          Write-Error "1. Network connectivity issues"
          Write-Error "2. Disco API being temporarily unavailable (HTTP 503)"
          Write-Error "3. Rate limiting (HTTP 429)"
          Write-Error "4. Invalid JDK version or distribution combination (HTTP 404)"
          Write-Error ""
          Write-Error "To fix this issue:"
          Write-Error "1. Check your internet connection"
          Write-Error "2. Wait a few minutes and try again (if HTTP 503/429)"
          Write-Error "3. Use a direct JDK URL with jdkDistributionUrl in maven-wrapper.properties"
          Write-Error "4. Set MVNW_SKIP_JDK=true to use system JDK"
          Write-Error "5. Try a different JDK distribution (temurin, corretto, zulu, etc.)"
          return
        }
      }
    }

    # URL encode the version (replace + with %2B)
    $encodedVersion = $normalizedVersion -replace '\+', '%2B'

    # For URL resolution, always use the original major version if available
    # This avoids issues with the Foojay API not properly filtering by specific java_version
    if ($Version -match '^\d+$') {
      # Original input was a major version, use it directly
      $versionParam = "jdk_version=$Version"
    } else {
      # Original input was a specific version, extract major version
      $majorVersion = $Version -replace '^(\d+).*', '$1'
      $versionParam = "jdk_version=$majorVersion"
    }

    # Call Disco API to get package information (more reliable than direct URI)
    # Only get directly downloadable packages to avoid redirect-only entries
    $discoPackageUrl = "$DISCO_API_BASE_URL/packages?distro=$Distribution&javafx_bundled=false&archive_type=zip&operating_system=windows&package_type=jdk&$versionParam&architecture=x64&latest=available&release_status=ga&directly_downloadable=true"

    try {
      Write-Verbose "Resolving JDK download URL from Disco API: $discoPackageUrl"

      $webclientUrl = New-Object System.Net.WebClient
      [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
      $apiResponse = $webclientUrl.DownloadString($discoPackageUrl)

      # Extract the download redirect URL from the JSON response
      $redirectUrl = ""
      if ($apiResponse -match '"pkg_download_redirect":"([^"]*)"') {
        $redirectUrl = $matches[1]
      }

      if (!$redirectUrl -or $redirectUrl.Trim() -eq "") {
        Write-Error "Failed to extract JDK download URL for version $normalizedVersion, distribution $Distribution"
        return
      }

      # Follow the redirect to get the actual download URL
      try {
        $redirectRequest = [System.Net.WebRequest]::Create($redirectUrl)
        $redirectRequest.Method = "HEAD"
        $redirectRequest.AllowAutoRedirect = $false
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

        $redirectResponse = $redirectRequest.GetResponse()
        if ($redirectResponse.StatusCode -eq [System.Net.HttpStatusCode]::Found -or
            $redirectResponse.StatusCode -eq [System.Net.HttpStatusCode]::MovedPermanently -or
            $redirectResponse.StatusCode -eq [System.Net.HttpStatusCode]::SeeOther) {
          $Url = $redirectResponse.Headers["Location"]
        }
        $redirectResponse.Close()

        # Fallback to redirect URL if no location header
        if (!$Url -or $Url.Trim() -eq "") {
          $Url = $redirectUrl
        }
      } catch {
        # Fallback to redirect URL if redirect fails
        $Url = $redirectUrl
      }
    } catch {
      # Extract HTTP status if available
      $httpStatus = ""
      if ($_.Exception -is [System.Net.WebException] -and $_.Exception.Response) {
        $httpStatus = [int]$_.Exception.Response.StatusCode
        Write-Error "Failed to resolve JDK URL from Disco API: HTTP $httpStatus"
        if ($httpStatus -eq 503) {
          Write-Error "The Disco API is temporarily unavailable (Service Unavailable)"
        } elseif ($httpStatus -eq 429) {
          Write-Error "Rate limited by Disco API (Too Many Requests)"
        }
      } else {
        Write-Error "Failed to resolve JDK URL from Disco API: $($_.Exception.Message)"
      }
      Write-Error "API URL: $discoPackageUrl"
      return
    }


  }

  Write-Verbose "Installing JDK $Version from $Url"

  # Create JDK directory
  New-Item -ItemType Directory -Path (Split-Path $jdkHome -Parent) -Force | Out-Null

  # Download JDK
  $jdkFileName = $Url -replace '^.*/',''
  $tempDir = New-TemporaryFile
  $tempDirPath = New-Item -ItemType Directory -Path "$tempDir.dir"
  $tempDir.Delete() | Out-Null
  $jdkFile = "$tempDirPath/$jdkFileName"

  try {
    Write-Verbose "Downloading JDK to: $jdkFile"
    $webclient = New-Object System.Net.WebClient
    if ($env:MVNW_USERNAME -and $env:MVNW_PASSWORD) {
      $webclient.Credentials = New-Object System.Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD)
    }
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $webclient.DownloadFile($Url, $jdkFile) | Out-Null

    # Verify checksum if provided
    if ($Checksum) {
      Write-Verbose "Verifying JDK checksum"
      Import-Module $PSHOME\Modules\Microsoft.PowerShell.Utility -Function Get-FileHash
      if ((Get-FileHash $jdkFile -Algorithm SHA256).Hash.ToLower() -ne $Checksum.ToLower()) {
        Write-Error "Error: Failed to validate JDK SHA-256 checksum"
      }
    }

    # Extract JDK
    Write-Verbose "Extracting JDK to: $jdkHome"
    New-Item -ItemType Directory -Path $jdkHome -Force | Out-Null
    Expand-Archive $jdkFile -DestinationPath $tempDirPath | Out-Null

    # Find the JDK directory and move its contents
    $extractedJdkDir = Get-ChildItem -Path $tempDirPath -Directory | Where-Object { $_.Name -like "*jdk*" } | Select-Object -First 1
    if ($extractedJdkDir) {
      Get-ChildItem -Path $extractedJdkDir.FullName | Move-Item -Destination $jdkHome
    } else {
      Write-Error "Could not find JDK directory in extracted archive"
    }

    # Verify JDK installation
    if (!(Test-Path -Path "$jdkHome/bin/java.exe")) {
      Write-Error "JDK installation failed: java.exe not found at $jdkHome/bin/java.exe"
    }

    Write-Verbose "JDK $Version installed successfully at $jdkHome"
    $env:JAVA_HOME = $jdkHome

  } finally {
    if (Test-Path $tempDirPath) {
      Remove-Item $tempDirPath -Recurse -Force | Out-Null
    }
  }
}

# Install JDK if configured
Install-JDK -Version $jdkVersion -Distribution $jdkDistribution -Url $jdkDistributionUrl -Checksum $jdkSha256Sum -AlwaysDownload $alwaysDownloadJdk

# Install toolchain JDK if configured
if ($toolchainJdkVersion) {
  Write-Verbose "Installing toolchain JDK $toolchainJdkVersion"
  Install-JDK -Version $toolchainJdkVersion -Distribution $toolchainJdkDistribution -Url $toolchainJdkDistributionUrl -Checksum $toolchainJdkSha256Sum -AlwaysDownload $alwaysDownloadJdk
}

if (Test-Path -Path "$MAVEN_HOME" -PathType Container) {
  Write-Verbose "found existing MAVEN_HOME at $MAVEN_HOME"
  Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
  exit $?
}

if (! $distributionUrlNameMain -or ($distributionUrlName -eq $distributionUrlNameMain)) {
  Write-Error "distributionUrl is not valid, must end with *-bin.zip, but found $distributionUrl"
}

# prepare tmp dir
$TMP_DOWNLOAD_DIR_HOLDER = New-TemporaryFile
$TMP_DOWNLOAD_DIR = New-Item -Itemtype Directory -Path "$TMP_DOWNLOAD_DIR_HOLDER.dir"
$TMP_DOWNLOAD_DIR_HOLDER.Delete() | Out-Null
trap {
  if ($TMP_DOWNLOAD_DIR.Exists) {
    try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null }
    catch { Write-Warning "Cannot remove $TMP_DOWNLOAD_DIR" }
  }
}

New-Item -Itemtype Directory -Path "$MAVEN_HOME_PARENT" -Force | Out-Null

# Download and Install Apache Maven
Write-Verbose "Couldn't find MAVEN_HOME, downloading and installing it ..."
Write-Verbose "Downloading from: $distributionUrl"
Write-Verbose "Downloading to: $TMP_DOWNLOAD_DIR/$distributionUrlName"

$webclient = New-Object System.Net.WebClient
if ($env:MVNW_USERNAME -and $env:MVNW_PASSWORD) {
  $webclient.Credentials = New-Object System.Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD)
}
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$webclient.DownloadFile($distributionUrl, "$TMP_DOWNLOAD_DIR/$distributionUrlName") | Out-Null

# If specified, validate the SHA-256 sum of the Maven distribution zip file
$distributionSha256Sum = $wrapperProperties.distributionSha256Sum
if ($distributionSha256Sum) {
  if ($USE_MVND) {
    Write-Error "Checksum validation is not supported for maven-mvnd. `nPlease disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties."
  }
  Import-Module $PSHOME\Modules\Microsoft.PowerShell.Utility -Function Get-FileHash
  if ((Get-FileHash "$TMP_DOWNLOAD_DIR/$distributionUrlName" -Algorithm SHA256).Hash.ToLower() -ne $distributionSha256Sum) {
    Write-Error "Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised. If you updated your Maven version, you need to update the specified distributionSha256Sum property."
  }
}

# unzip and move
Expand-Archive "$TMP_DOWNLOAD_DIR/$distributionUrlName" -DestinationPath "$TMP_DOWNLOAD_DIR" | Out-Null

# Find the actual extracted directory name (handles snapshots where filename != directory name)
$actualDistributionDir = ""

# First try the expected directory name (for regular distributions)
$expectedPath = Join-Path "$TMP_DOWNLOAD_DIR" "$distributionUrlNameMain"
$expectedMvnPath = Join-Path "$expectedPath" "bin/$MVN_CMD"
if ((Test-Path -Path $expectedPath -PathType Container) -and (Test-Path -Path $expectedMvnPath -PathType Leaf)) {
  $actualDistributionDir = $distributionUrlNameMain
}

# If not found, search for any directory with the Maven executable (for snapshots)
if (!$actualDistributionDir) {
  Get-ChildItem -Path "$TMP_DOWNLOAD_DIR" -Directory | ForEach-Object {
    $testPath = Join-Path $_.FullName "bin/$MVN_CMD"
    if (Test-Path -Path $testPath -PathType Leaf) {
      $actualDistributionDir = $_.Name
    }
  }
}

if (!$actualDistributionDir) {
  Write-Error "Could not find Maven distribution directory in extracted archive"
}

Write-Verbose "Found extracted Maven distribution directory: $actualDistributionDir"
Rename-Item -Path "$TMP_DOWNLOAD_DIR/$actualDistributionDir" -NewName $MAVEN_HOME_NAME | Out-Null
try {
  Move-Item -Path "$TMP_DOWNLOAD_DIR/$MAVEN_HOME_NAME" -Destination $MAVEN_HOME_PARENT | Out-Null
} catch {
  if (! (Test-Path -Path "$MAVEN_HOME" -PathType Container)) {
    Write-Error "fail to move MAVEN_HOME"
  }
} finally {
  try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null }
  catch { Write-Warning "Cannot remove $TMP_DOWNLOAD_DIR" }
}

Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
