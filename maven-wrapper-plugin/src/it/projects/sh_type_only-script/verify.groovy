
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

log = new File(basedir, 'build.log').text
// check "mvn wrapper:wrapper" output
assert log.contains('[INFO] Unpacked only-script type wrapper distribution org.apache.maven.wrapper:maven-wrapper-distribution:zip:only-script:')

// check "mvnw -v" output
assert log.contains("Couldn't find ")
assert log.contains(", downloading and installing it ...")

Properties props = new Properties()
new File(basedir,'.mvn/wrapper/maven-wrapper.properties').withInputStream {
    props.load(it)
}
assert props.wrapperVersion.equals(wrapperCurrentVersion)
assert props.distributionType.equals("only-script")
assert !props.containsKey('wrapperUrl')

// Check diagnostics without downloading a distribution or using the host JDK.
['home', 'aix', 'path'].each { layout ->
    ['java', 'javac', 'jar'].each { missingTool ->
        // AIX layout selection requires an executable jre/sh/java.
        if (layout == 'aix' && missingTool == 'java') {
            return
        }
        [false, true].each { nonExecutable ->
            File testDir = new File(basedir, "target/tools-${layout}-${missingTool}-${nonExecutable}")
            File binDir = new File(testDir, 'bin')
            File javaHome = new File(testDir, 'jdk with spaces')
            assert binDir.mkdirs()

            // Only expose the commands needed before JDK validation.
            ['uname', 'dirname', 'basename', 'tr', 'mktemp', 'mkdir', 'rm'].each { tool ->
                Process lookup = new ProcessBuilder('/bin/sh', '-c', "command -v ${tool}").start()
                String executable = lookup.inputStream.text.trim()
                assert lookup.waitFor() == 0
                java.nio.file.Files.createSymbolicLink(new File(binDir, tool).toPath(), new File(executable).toPath())
            }

            Map tools = [:]
            ['java', 'javac', 'jar'].each { tool ->
                String location = layout == 'aix' ? (tool == 'jar' ? 'jre/bin' : 'jre/sh') : 'bin'
                File executable = layout == 'path' ? new File(binDir, tool) : new File(javaHome, "${location}/${tool}")
                tools[tool] = executable
                if (tool != missingTool || nonExecutable) {
                    executable.parentFile.mkdirs()
                    executable.text = '#!/bin/sh\nexit 99\n'
                    assert executable.setExecutable(tool != missingTool)
                }
            }
            ProcessBuilder builder = new ProcessBuilder('/bin/sh', new File(basedir, 'mvnw').absolutePath, '-v')
            builder.directory(basedir)
            builder.redirectErrorStream(true)
            builder.environment().put('PATH', binDir.absolutePath)
            builder.environment().put('MAVEN_USER_HOME', new File(testDir, 'maven-user-home').absolutePath)
            builder.environment().put('MVNW_VERBOSE', 'false')
            if (layout == 'path') {
                builder.environment().remove('JAVA_HOME')
            } else {
                builder.environment().put('JAVA_HOME', javaHome.absolutePath)
            }
            Process process = builder.start()
            String output = process.inputStream.text
            assert process.waitFor() == 1 : output
            if (layout == 'path') {
                assert output.contains("The ${missingTool} command does not exist or is not executable in PATH nor is JAVA_HOME set") : output
            } else {
                File expected = missingTool == 'jar' && layout == 'home' ? new File(javaHome, 'jre/bin/jar') : tools[missingTool]
                assert output.contains("JAVA_HOME is set to \"${javaHome.absolutePath}\", but \"${expected.absolutePath}\" does not exist or is not executable.") : output
            }
        }
    }
}

return true
