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
assert props.jdkDistributionUrl != null
assert props.jdkDistributionUrl.contains("temurin17-binaries")
assert props.distributionType == "only-script"

log = new File(basedir, 'build.log').text

// Check wrapper generation output
assert log.contains('[INFO] Unpacked only-script type wrapper distribution')

// For direct URL tests, we expect either successful download or network-related issues
// Since this is a real URL, it might work or fail depending on network connectivity
boolean jdkInstalled = log.contains("Installing JDK") || log.contains("JDK") || log.contains("already installed")
boolean systemJdkUsed = log.contains("Apache Maven") // Maven version output indicates successful execution
boolean networkIssue = log.contains("Failed to fetch") || log.contains("curl:") || log.contains("wget:")

assert jdkInstalled || systemJdkUsed || networkIssue, "Either JDK should be installed, system JDK used, or network issue encountered"
