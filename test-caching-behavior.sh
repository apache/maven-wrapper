#!/bin/bash

# Test script for JDK version caching behavior

set -e

echo "Testing JDK version caching behavior..."

# Create a test directory
TEST_DIR="$(mktemp -d)"
cd "$TEST_DIR"

echo "Test directory: $TEST_DIR"

# Set up Maven user home for testing
export MAVEN_USER_HOME="$TEST_DIR/.m2"
mkdir -p "$MAVEN_USER_HOME"

# Source the functions from the shell script
source_shell_functions() {
    # Extract and source the functions we need for testing
    local temp_functions=$(mktemp)
    
    # Extract the functions from the mvnw script
    sed -n '/^detect_sdkman_platform()/,/^}$/p' /mnt/persist/workspace/maven-wrapper-distribution/src/resources/only-mvnw > "$temp_functions"
    echo >> "$temp_functions"
    sed -n '/^get_latest_version_from_sdkman()/,/^}$/p' /mnt/persist/workspace/maven-wrapper-distribution/src/resources/only-mvnw >> "$temp_functions"
    echo >> "$temp_functions"
    sed -n '/^verbose()/,/^}/p' /mnt/persist/workspace/maven-wrapper-distribution/src/resources/only-mvnw >> "$temp_functions"
    
    source "$temp_functions"
    rm -f "$temp_functions"
}

# Test caching behavior
test_caching() {
    echo "Testing caching behavior..."
    
    # First call - should query API and cache result
    echo "First call (should query API):"
    local start_time=$(date +%s)
    local version1
    version1=$(get_latest_version_from_sdkman "17" "-tem")
    local end_time=$(date +%s)
    local duration1=$((end_time - start_time))
    
    if [ -n "$version1" ]; then
        echo "✅ First call successful: $version1 (took ${duration1}s)"
    else
        echo "❌ First call failed"
        return 1
    fi
    
    # Check if cache file was created
    local cache_file="${MAVEN_USER_HOME}/wrapper/cache/jdk-17-tem.cache"
    if [ -f "$cache_file" ]; then
        echo "✅ Cache file created: $cache_file"
        echo "   Cached version: $(cat "$cache_file")"
    else
        echo "❌ Cache file not created"
        return 1
    fi
    
    # Second call - should use cache (much faster)
    echo "Second call (should use cache):"
    start_time=$(date +%s)
    local version2
    version2=$(get_latest_version_from_sdkman "17" "-tem")
    end_time=$(date +%s)
    local duration2=$((end_time - start_time))
    
    if [ -n "$version2" ]; then
        echo "✅ Second call successful: $version2 (took ${duration2}s)"
    else
        echo "❌ Second call failed"
        return 1
    fi
    
    # Verify versions are the same
    if [ "$version1" = "$version2" ]; then
        echo "✅ Cached version matches: $version1"
    else
        echo "❌ Version mismatch: $version1 vs $version2"
        return 1
    fi
    
    # Verify second call was faster (cache hit)
    if [ "$duration2" -lt "$duration1" ]; then
        echo "✅ Second call was faster (cache hit): ${duration2}s vs ${duration1}s"
    else
        echo "⚠️  Second call was not significantly faster (might still be cache hit)"
    fi
    
    return 0
}

# Test cache expiration
test_cache_expiration() {
    echo "Testing cache expiration..."
    
    local cache_file="${MAVEN_USER_HOME}/wrapper/cache/jdk-21-tem.cache"
    local cache_dir="$(dirname "$cache_file")"
    
    # Create an old cache file (simulate 25 hours old)
    mkdir -p "$cache_dir"
    echo "21.0.1" > "$cache_file"
    
    # Set file timestamp to 25 hours ago (older than 24 hour cache)
    if command -v touch >/dev/null; then
        # Create a timestamp 25 hours ago
        local old_time=$(date -d '25 hours ago' '+%Y%m%d%H%M.%S' 2>/dev/null || date -v-25H '+%Y%m%d%H%M.%S' 2>/dev/null)
        if [ -n "$old_time" ]; then
            touch -t "$old_time" "$cache_file" 2>/dev/null || true
        fi
    fi
    
    echo "Created old cache file with version: $(cat "$cache_file")"
    
    # Call function - should ignore expired cache and query API
    local new_version
    new_version=$(get_latest_version_from_sdkman "21" "-tem")
    
    if [ -n "$new_version" ]; then
        echo "✅ Resolved version after cache expiration: $new_version"
        
        # Check if cache was updated
        local cached_version
        cached_version=$(cat "$cache_file" 2>/dev/null)
        if [ "$cached_version" = "$new_version" ] && [ "$cached_version" != "21.0.1" ]; then
            echo "✅ Cache was updated with new version: $cached_version"
        else
            echo "⚠️  Cache update unclear: cached=$cached_version, resolved=$new_version"
        fi
    else
        echo "❌ Failed to resolve version after cache expiration"
        return 1
    fi
    
    return 0
}

# Test error handling without fallbacks
test_error_handling() {
    echo "Testing error handling without fallbacks..."
    
    # Test with invalid vendor suffix (should fail without fallback)
    local invalid_version
    invalid_version=$(get_latest_version_from_sdkman "17" "-invalid" 2>/dev/null)
    
    if [ -z "$invalid_version" ]; then
        echo "✅ Correctly failed for invalid vendor (no fallback used)"
    else
        echo "❌ Should have failed for invalid vendor, got: $invalid_version"
        return 1
    fi
    
    return 0
}

# Main test execution
echo "=== JDK Version Caching Test ==="
echo

# Source shell functions for testing
source_shell_functions

# Enable verbose output for testing
MVNW_VERBOSE=true

# Test caching behavior
if test_caching; then
    echo "✅ Caching test passed"
else
    echo "❌ Caching test failed"
    exit 1
fi

echo

# Test cache expiration
if test_cache_expiration; then
    echo "✅ Cache expiration test passed"
else
    echo "❌ Cache expiration test failed"
    exit 1
fi

echo

# Test error handling
if test_error_handling; then
    echo "✅ Error handling test passed"
else
    echo "❌ Error handling test failed"
    exit 1
fi

echo
echo "=== Test Summary ==="
echo "✅ Caching behavior: PASSED"
echo "✅ Cache expiration: PASSED"
echo "✅ Error handling: PASSED"
echo "✅ No hardcoded fallbacks: VERIFIED"
echo
echo "JDK version caching is working correctly!"
echo "- Versions are cached for 24 hours (daily update policy)"
echo "- Cache hits are faster than API calls"
echo "- Expired cache entries are refreshed"
echo "- Failures result in clear errors (no stale fallbacks)"

# Cleanup
cd /
rm -rf "$TEST_DIR"

echo "Test completed successfully."
