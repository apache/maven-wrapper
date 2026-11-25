
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

properties = new File(basedir,'.mvn/wrapper/maven-wrapper.properties')
assert properties.text.contains('distributionSha512Sum=1c095ed556eda06c6d82fdf52200bc4f3437a1bab42387e801d6f4c56e833fb82b16e8bf0aab95c9708de7bfb55ec27f653a7cf0f491acebc541af234eded94d')
assert properties.text =~ /wrapperSha512Sum=[0-9A-F]{128}/

log = new File(basedir, 'build.log').text
// check "mvnw -v" output: the wrapper jar and the distribution were validated, then Maven was started
assert log.contains('Validated SHA-512 hash for ')
assert log.contains('Apache Maven 3.6.3')
