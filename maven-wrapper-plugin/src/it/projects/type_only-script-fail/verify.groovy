
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

log = new File(basedir, 'build.log').text
boolean isWindows = System.getProperty('os.name', 'unknown').startsWith('Windows')

if (isWindows) {
    // on Windows: just the fact it failed is enough
    assert log.contains('Exception calling "DownloadFile"')
} else {
    // on non-Windows: verify clear messages as well
    // cover all methods: point is, there is no Maven version 0.0.0
    assert log.contains('wget: Failed to fetch') || log.contains('curl: Failed to fetch') || log.contains('- Error downloading:')
}
