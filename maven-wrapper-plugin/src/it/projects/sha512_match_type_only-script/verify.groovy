
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
assert properties.text.contains('distributionSha512Sum=1C095ED556EDA06C6D82FDF52200BC4F3437A1BAB42387E801D6F4C56E833FB82B16E8BF0AAB95C9708DE7BFB55EC27F653A7CF0F491ACEBC541AF234EDED94D')

log = new File(basedir, 'build.log').text
// check "mvnw -v" output: the distribution was downloaded, validated and started
assert log.contains("Couldn't find ")
assert log.contains(", downloading and installing it ...")
assert log.contains('Apache Maven 3.6.3')
