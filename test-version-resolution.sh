#!/bin/bash

# Test script for version resolution

detect_sdkman_platform() {
  local kernel="$(uname -s)"
  local machine="$(uname -m)"
  
  case "$kernel" in
  Linux)
    case "$machine" in
    i686) echo "linuxx32" ;;
    x86_64) echo "linuxx64" ;;
    armv6l|armv7l|armv8l) echo "linuxarm32hf" ;;
    aarch64) echo "linuxarm64" ;;
    *) echo "exotic" ;;
    esac
    ;;
  Darwin)
    case "$machine" in
    x86_64) echo "darwinx64" ;;
    arm64) echo "darwinarm64" ;;
    *) echo "darwinx64" ;;
    esac
    ;;
  CYGWIN*|MINGW*|MSYS*)
    case "$machine" in
    x86_64) echo "windowsx64" ;;
    *) echo "exotic" ;;
    esac
    ;;
  *)
    echo "exotic"
    ;;
  esac
}

get_latest_version_from_sdkman() {
  local major_version="$1"
  local vendor_suffix="$2"
  local platform="$(detect_sdkman_platform)"
  
  if [ "$platform" = "exotic" ]; then
    return 1  # Cannot query SDKMAN for exotic platforms
  fi
  
  # Query SDKMAN API for all available versions
  local versions_api_url="https://api.sdkman.io/2/candidates/java/${platform}/versions/all"
  local all_versions
  
  if command -v curl >/dev/null; then
    all_versions="$(curl -s -f "$versions_api_url" 2>/dev/null)"
  elif command -v wget >/dev/null; then
    all_versions="$(wget -q -O - "$versions_api_url" 2>/dev/null)"
  else
    return 1  # No HTTP client available
  fi
  
  if [ -z "$all_versions" ]; then
    return 1  # API call failed
  fi
  
  # Find the latest version for the major version and vendor
  # SDKMAN returns versions in a comma-separated list
  local latest_version
  latest_version="$(echo "$all_versions" | tr ',' '\n' | grep "^${major_version}\." | grep -- "${vendor_suffix}\$" | head -1 | sed "s/${vendor_suffix}\$//")"
  
  if [ -n "$latest_version" ]; then
    echo "$latest_version"
    return 0
  else
    return 1  # No matching version found
  fi
}

echo "Testing version resolution..."

# Test JDK 17 with Temurin
echo "Resolving JDK 17 with Temurin..."
latest_17=$(get_latest_version_from_sdkman "17" "-tem")
if [ $? -eq 0 ] && [ -n "$latest_17" ]; then
    echo "✅ Found JDK 17: $latest_17"
else
    echo "❌ Failed to resolve JDK 17"
fi

# Test JDK 21 with Temurin
echo "Resolving JDK 21 with Temurin..."
latest_21=$(get_latest_version_from_sdkman "21" "-tem")
if [ $? -eq 0 ] && [ -n "$latest_21" ]; then
    echo "✅ Found JDK 21: $latest_21"
else
    echo "❌ Failed to resolve JDK 21"
fi

# Test JDK 11 with Amazon Corretto
echo "Resolving JDK 11 with Amazon Corretto..."
latest_11_amzn=$(get_latest_version_from_sdkman "11" "-amzn")
if [ $? -eq 0 ] && [ -n "$latest_11_amzn" ]; then
    echo "✅ Found JDK 11 Corretto: $latest_11_amzn"
else
    echo "❌ Failed to resolve JDK 11 Corretto"
fi

echo "Version resolution test completed!"
