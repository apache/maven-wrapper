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

def props = new Properties()
propertiesFile.withInputStream {
    props.load(it)
}

assert props.distributionUrl.equals('https://archive.apache.org/dist/maven/mvnd/0.9.0/maven-mvnd-0.9.0-bin.zip')

log = new File(basedir, 'build.log').text

// check "mvn wrapper:wrapper" output
// works only on 'only-script' type
assert log.contains('[INFO] Unpacked only-script type wrapper distribution org.apache.maven.wrapper:maven-wrapper-distribution:zip:only-script:')

// check "mvnw --status" output
assert log.contains("Status")
assert log.contains("RSS")
assert log.contains("Java home")
