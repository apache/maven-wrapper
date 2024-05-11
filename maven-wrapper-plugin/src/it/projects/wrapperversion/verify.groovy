
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
assert !(new File(basedir,'mvnwDebug').exists())
assert !(new File(basedir,'mvnwDebug.cmd').exists())

def propertiesFile = new File(basedir,'.mvn/wrapper/maven-wrapper.properties')
assert propertiesFile.exists()

Properties props = new Properties()
propertiesFile.withInputStream {
    props.load(it)
}
assert props.wrapperVersion == '3.3.1'
assert props.wrapperUrl.endsWith("/org/apache/maven/wrapper/maven-wrapper/${props.wrapperVersion}/maven-wrapper-${props.wrapperVersion}.jar")
assert props.distributionUrl.endsWith("/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip")

log = new File(basedir, 'build.log').text
// check "mvn wrapper:wrapper" output
assert log.contains("[INFO] Unpacked script type wrapper distribution org.apache.maven.wrapper:maven-wrapper-distribution:zip:script:${props.wrapperVersion}\n[INFO] Configuring .mvn/wrapper/maven-wrapper.properties to use Maven $mavenVersion and download from ")
// check "mvnw -v" output
assert log.contains("[INFO] Apache Maven Wrapper ${props.wrapperVersion}\nApache Maven $mavenVersion")
