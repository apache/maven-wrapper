
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
assert !(new File( basedir, 'mvnwDebug' ).exists())
assert !(new File( basedir, 'mvnwDebug.cmd' ).exists())

def wrapperProperties = new File(basedir,'.mvn/wrapper/maven-wrapper.properties')
assert wrapperProperties.exists()

Properties props = new Properties()
wrapperProperties.withInputStream {
    props.load(it)
}

// Assert that distribution URL was updated to the highest version automatically resolved
// in the specified range: [3.0,3.10-rc)
assert props.distributionUrl.endsWith("/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip")

def log = new File(basedir, 'build.log').text
assert log.contains('Apache Maven 3.9.16')
