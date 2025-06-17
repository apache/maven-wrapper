#!/bin/bash

# Test script to verify JDK management in shell script mode

set -e

echo "Testing JDK management in Maven Wrapper shell script mode..."

# Create a test directory
TEST_DIR="$(mktemp -d)"
cd "$TEST_DIR"

echo "Test directory: $TEST_DIR"

# Create .mvn/wrapper directory
mkdir -p .mvn/wrapper

# Create maven-wrapper.properties with JDK configuration
cat > .mvn/wrapper/maven-wrapper.properties << 'EOF'
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
wrapperVersion=3.3.0
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
jdkVersion=17
jdkVendor=temurin
EOF

# Copy the enhanced mvnw script
cp /mnt/persist/workspace/maven-wrapper-distribution/src/resources/only-mvnw ./mvnw
chmod +x ./mvnw

echo "Created test setup with JDK 17 configuration"

# Test JDK URL resolution
echo "Testing JDK URL resolution..."

# Source the functions from mvnw script
source <(sed -n '/^detect_platform()/,/^}$/p' ./mvnw)
source <(sed -n '/^detect_architecture()/,/^}$/p' ./mvnw)
source <(sed -n '/^resolve_jdk_url()/,/^}$/p' ./mvnw)
source <(sed -n '/^resolve_temurin_url()/,/^}$/p' ./mvnw)

# Test platform detection
PLATFORM=$(detect_platform)
ARCH=$(detect_architecture)
echo "Detected platform: $PLATFORM"
echo "Detected architecture: $ARCH"

# Test JDK URL resolution
JDK_URL=$(resolve_jdk_url "17" "temurin")
echo "Resolved JDK URL: $JDK_URL"

# Verify URL format
if [[ "$JDK_URL" == *"adoptium"* ]] && [[ "$JDK_URL" == *"17"* ]]; then
    echo "✅ JDK URL resolution works correctly"
else
    echo "❌ JDK URL resolution failed"
    exit 1
fi

# Test property parsing
echo "Testing property parsing..."
source <(sed -n '/^while IFS="=" read -r key value; do$/,/^done <.*maven-wrapper.properties.*$/p' ./mvnw)

if [[ -n "$jdkVersion" ]] && [[ "$jdkVersion" == "17" ]]; then
    echo "✅ JDK version property parsed correctly: $jdkVersion"
else
    echo "❌ JDK version property parsing failed"
    exit 1
fi

if [[ -n "$jdkVendor" ]] && [[ "$jdkVendor" == "temurin" ]]; then
    echo "✅ JDK vendor property parsed correctly: $jdkVendor"
else
    echo "❌ JDK vendor property parsing failed"
    exit 1
fi

echo "✅ All tests passed!"

# Cleanup
cd /
rm -rf "$TEST_DIR"

echo "Test completed successfully. JDK management in shell script mode is working."
