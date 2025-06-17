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
$jdkVendor = $wrapperProperties.jdkVendor
$jdkDistributionUrl = $wrapperProperties.jdkDistributionUrl
$jdkSha256Sum = $wrapperProperties.jdkSha256Sum
$alwaysDownloadJdk = $wrapperProperties.alwaysDownloadJdk
$toolchainJdkVersion = $wrapperProperties.toolchainJdkVersion
$toolchainJdkVendor = $wrapperProperties.toolchainJdkVendor
$toolchainJdkDistributionUrl = $wrapperProperties.toolchainJdkDistributionUrl
$toolchainJdkSha256Sum = $wrapperProperties.toolchainJdkSha256Sum

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
$MAVEN_HOME_PARENT = "$HOME/.m2/wrapper/dists/$distributionUrlNameMain"
if ($env:MAVEN_USER_HOME) {
  $MAVEN_HOME_PARENT = "$env:MAVEN_USER_HOME/wrapper/dists/$distributionUrlNameMain"
}
$MAVEN_HOME_NAME = ([System.Security.Cryptography.SHA256]::Create().ComputeHash([byte[]][char[]]$distributionUrl) | ForEach-Object {$_.ToString("x2")}) -join ''
$MAVEN_HOME = "$MAVEN_HOME_PARENT/$MAVEN_HOME_NAME"

# JDK management functions
function Install-JDK {
  param(
    [string]$Version,
    [string]$Vendor = "temurin",
    [string]$Url,
    [string]$Checksum,
    [string]$AlwaysDownload = "false"
  )

  if (!$Version) {
    return  # No JDK version specified
  }

  # Determine JDK installation directory
  $jdkDirName = "jdk-$Version-$Vendor"
  $mavenUserHome = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { "$HOME/.m2" }
  $jdkHome = "$mavenUserHome/wrapper/jdks/$jdkDirName"

  # Check if JDK already exists and we're not forcing re-download
  if ((Test-Path -Path $jdkHome -PathType Container) -and ($AlwaysDownload -ne "true")) {
    Write-Verbose "JDK $Version already installed at $jdkHome"
    $env:JAVA_HOME = $jdkHome
    return
  }

  # Resolve JDK URL if not provided using SDKMAN API
  if (!$Url) {
    # Get SDKMAN version identifier
    $sdkmanSuffix = switch ($Vendor.ToLower()) {
      "temurin" { "-tem" }
      "adoptium" { "-tem" }
      "adoptopenjdk" { "-tem" }
      "eclipse" { "-tem" }
      "corretto" { "-amzn" }
      "amazon" { "-amzn" }
      "aws" { "-amzn" }
      "zulu" { "-zulu" }
      "azul" { "-zulu" }
      "liberica" { "-librca" }
      "bellsoft" { "-librca" }
      "oracle" { "-oracle" }
      "microsoft" { "-ms" }
      "ms" { "-ms" }
      "semeru" { "-sem" }
      "ibm" { "-sem" }
      "graalvm" { "-grl" }
      "graal" { "-grl" }
      default { "-tem" }
    }

    # Normalize version for major versions
    $normalizedVersion = switch ($Version) {
      "8" { "8.0.452" }
      "11" { "11.0.27" }
      "17" { "17.0.15" }
      "21" { "21.0.7" }
      "22" { "22.0.2" }
      default { $Version }
    }

    $sdkmanVersion = "$normalizedVersion$sdkmanSuffix"
    $platform = "windowsx64"  # Windows x64 platform identifier for SDKMAN

    # Call SDKMAN API to get download URL (handle 302 redirect)
    $sdkmanApiUrl = "https://api.sdkman.io/2/broker/download/java/$sdkmanVersion/$platform"

    try {
      Write-Verbose "Resolving JDK download URL from SDKMAN API: $sdkmanApiUrl"

      # Create HTTP request to handle redirects manually
      $request = [System.Net.WebRequest]::Create($sdkmanApiUrl)
      $request.Method = "GET"
      $request.UserAgent = "Maven-Wrapper/3.3.0"
      $request.AllowAutoRedirect = $false
      [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

      $response = $request.GetResponse()

      if ($response.StatusCode -eq [System.Net.HttpStatusCode]::Found -or
          $response.StatusCode -eq [System.Net.HttpStatusCode]::MovedPermanently -or
          $response.StatusCode -eq [System.Net.HttpStatusCode]::SeeOther) {
        $Url = $response.Headers["Location"]
        if (!$Url) {
          Write-Error "SDKMAN API returned redirect without location header"
        }
      } else {
        Write-Error "Unexpected response from SDKMAN API: $($response.StatusCode)"
      }

      $response.Close()

      if (!$Url) {
        Write-Error "Failed to resolve JDK download URL for $sdkmanVersion on $platform"
      }
    } catch {
      Write-Error "Failed to resolve JDK URL from SDKMAN API: $($_.Exception.Message)"
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
Install-JDK -Version $jdkVersion -Vendor $jdkVendor -Url $jdkDistributionUrl -Checksum $jdkSha256Sum -AlwaysDownload $alwaysDownloadJdk

# Install toolchain JDK if configured
if ($toolchainJdkVersion) {
  Write-Verbose "Installing toolchain JDK $toolchainJdkVersion"
  Install-JDK -Version $toolchainJdkVersion -Vendor $toolchainJdkVendor -Url $toolchainJdkDistributionUrl -Checksum $toolchainJdkSha256Sum -AlwaysDownload $alwaysDownloadJdk
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
Rename-Item -Path "$TMP_DOWNLOAD_DIR/$distributionUrlNameMain" -NewName $MAVEN_HOME_NAME | Out-Null
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
