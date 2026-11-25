
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

import java.security.MessageDigest
import java.util.zip.ZipFile

// the maven-wrapper.jar under test is built by this build: compute its SHA-512 sum from the bin distribution,
// in uppercase to check that the scripts accept it
def distribution = new File(localRepositoryPath, "org/apache/maven/wrapper/maven-wrapper-distribution/${wrapperCurrentVersion}/maven-wrapper-distribution-${wrapperCurrentVersion}-bin.zip")
def wrapperJar = new ZipFile(distribution).withCloseable { zip ->
    zip.getInputStream(zip.getEntry('.mvn/wrapper/maven-wrapper.jar')).bytes
}

context.get('userProperties').put('wrapperSha512Sum', MessageDigest.getInstance('SHA-512').digest(wrapperJar).encodeHex().toString().toUpperCase())
