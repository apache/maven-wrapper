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
assert props.jdkVersion == "17"
assert props.jdkDistribution == "temurin"
assert props.distributionType == "only-script"

log = new File(basedir, 'build.log').text

// Check wrapper generation output
assert log.contains('[INFO] Unpacked only-script type wrapper distribution')

// With MVNW_SKIP_JDK=true, we should see the skip message or no JDK installation
boolean skipMessageFound = log.contains("Skipping JDK installation due to MVNW_SKIP_JDK")
boolean noJdkInstallation = !log.contains("Installing JDK")
boolean systemJdkUsed = log.contains("Apache Maven") // Maven version output indicates successful execution

// Either we should see the skip message, or no JDK installation should occur
// And Maven should still work with system JDK
assert (skipMessageFound || noJdkInstallation) && systemJdkUsed, "JDK installation should be skipped and system JDK should be used"
