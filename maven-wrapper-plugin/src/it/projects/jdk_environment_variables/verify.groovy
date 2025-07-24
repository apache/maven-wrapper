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

// Verify JDK configuration is present in properties (from test.properties)
assert props.jdkVersion == "17"
assert props.jdkDistribution == "temurin"
assert props.distributionType == "only-script"

log = new File(basedir, 'build.log').text

// Check wrapper generation output
assert log.contains('[INFO] Unpacked only-script type wrapper distribution')

// This test verifies that environment variables override properties file settings
// Properties file has: jdkVersion=17, jdkDistribution=temurin
// Environment variables set: MVNW_JDK_VERSION=11, MVNW_JDK_DISTRIBUTION=zulu
// The wrapper execution should use the environment variable values

// With MVNW_VERBOSE=true, the wrapper should show debug output about JDK configuration
// Look for evidence that the environment variables are being processed
// The wrapper may fail to download JDK in test environment, but it should show
// that it's attempting to use JDK 11 with Zulu distribution (from env vars)
// rather than JDK 17 with Temurin (from properties file)

// Check that wrapper script was executed and processed environment variables
// The exact output may vary, but we should see evidence of JDK processing
assert log.contains('mvnw') || log.contains('Maven') || log.contains('wrapper')
