
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

properties = new File(basedir,'.mvn/wrapper/maven-wrapper.properties')
assert properties.exists()
assert properties.text.contains('wrapperSha512Sum=256cdc53261371d6f6fefd92e99d85df5295d1f83ab826106768094a34e6f1b0eb4f7c30e75ada80218ed5bb384bdce334a6697354eef561f50adfc2113c881d')

log = new File(basedir, 'build.log').text
// check "mvn wrapper:wrapper" output
assert log.contains('Error: Failed to validate Maven wrapper SHA-512, your Maven wrapper might be compromised.')
assert !log.contains('shasum:')

// check "mvnw -v" output
assert !log.contains('Apache Maven ')
