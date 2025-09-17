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

assert new File(basedir,'mvnw').exists()
assert new File(basedir,'mvnw.cmd').exists()

wrapperProperties = new File(basedir,'.mvn/wrapper/maven-wrapper.properties')
assert wrapperProperties.exists()

Properties props = new Properties()
wrapperProperties.withInputStream {
    props.load(it)
}

// Verify JDK configuration is present in properties (even though it will be skipped)
// Using JDK 99 (non-existent) to ensure we're actually skipping JDK installation
assert props.jdkVersion == "99"
assert props.jdkDistribution == "temurin"
assert props.distributionType == "only-script"

log = new File(basedir, 'build.log').text

// Check wrapper generation output
assert log.contains('[INFO] Unpacked only-script type wrapper distribution')

// This test validates that JDK configuration is correctly generated in wrapper properties
// The MVNW_SKIP_JDK environment variable should prevent any JDK download attempts
// Using JDK 99 ensures that if JDK download was attempted, it would fail
// But with MVNW_SKIP_JDK=true, the wrapper should skip JDK installation entirely

// Test passes if we reach this point - configuration was generated correctly
assert true
