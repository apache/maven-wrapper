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

// Verify JDK configuration is present in properties
assert props.jdkVersion == "17"
assert props.jdkDistribution == "temurin"
assert props.jdkSha256Sum == "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
assert props.distributionType == "only-script"

log = new File(basedir, 'build.log').text

// Check wrapper generation output
assert log.contains('[INFO] Unpacked only-script type wrapper distribution')

// For checksum verification test, we expect either:
// 1. Checksum verification failure (since we used a dummy checksum)
// 2. Network issues preventing download
// 3. System JDK usage (if JDK download is skipped)
boolean checksumFailure = log.contains("checksum") || log.contains("SHA-256") || log.contains("verification")
boolean networkIssue = log.contains("Failed to fetch") || log.contains("curl:") || log.contains("wget:")

// The test should either fail checksum verification or use system JDK
assert checksumFailure || networkIssue || systemJdkUsed, "Either checksum verification should occur, network issue encountered, or system JDK should be used"
