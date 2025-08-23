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

// Verify wrapper files were generated
assert new File(basedir, 'mvnw').exists()
assert new File(basedir, 'mvnw.cmd').exists()

// Verify wrapper properties file exists and contains snapshot URL
def wrapperProperties = new File(basedir, '.mvn/wrapper/maven-wrapper.properties')
assert wrapperProperties.exists()

Properties props = new Properties()
wrapperProperties.withInputStream {
    props.load(it)
}

// Verify it's only-script type
assert props.distributionType.equals("only-script")

// The plugin should have used our custom timestamped snapshot URL
println "Generated distribution URL: ${props.distributionUrl}"
assert props.distributionUrl.contains("apache-maven-4.9.999-20250710.120440-1-bin.zip"), "Expected timestamped snapshot distribution URL but got: ${props.distributionUrl}"
assert props.distributionUrl.contains("/org/apache/maven/apache-maven/4.9.999-SNAPSHOT/"), "Expected Maven repository path but got: ${props.distributionUrl}"

println "✓ Plugin correctly used custom distributionUrl parameter"
println "✓ Distribution URL: ${props.distributionUrl}"

// Test that the wrapper scripts were created correctly
def mvnwScript = new File(basedir, 'mvnw')
def mvnwCmd = new File(basedir, 'mvnw.cmd')

assert mvnwScript.exists(), "mvnw script should exist"
assert mvnwScript.canExecute(), "mvnw script should be executable"
assert mvnwCmd.exists(), "mvnw.cmd script should exist"

println "✓ Snapshot distribution integration test passed!"
println "✓ Plugin correctly accepted custom distributionUrl parameter"
println "✓ Wrapper configured to use timestamped snapshot URL: ${props.distributionUrl}"
println "✓ This tests the scenario where ZIP filename != directory name inside ZIP"
println "✓ Our fix should handle: apache-maven-4.1.0-20250710.120440-1-bin.zip -> apache-maven-4.1.0-SNAPSHOT/"

log = new File(basedir, 'build.log').text
// check "mvnw" output
assert log.contains('Apache Maven 4.9.999 from SNAPSHOT distribution'), "Expected 'Apache Maven 4.9.999 from SNAPSHOT distribution' in build.log but not found"
