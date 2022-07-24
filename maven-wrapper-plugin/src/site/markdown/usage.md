# Usage
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

Wrapper scripts are a set of files that can be added to your Maven project.
If people want to build this project, they don't need to install Maven first.
Instead they can call the Maven wrapper script (like `mvnw`/`mvnw.cmd`), which will download and unpack Maven into their `${user.home}/.m2/wrapper/dists` folder.
It is also an easy way to let everyone build the project with the same Maven version.

The Apache Maven Wrapper Plugin makes it easier to add these wrapper scripts to your project.

The scripts work like this:
- download the maven-wrapper jar, if it is not available yet,
- the maven-wrapper.jar contains the code to download, install and run Apache Maven

Apache Maven Wrapper Distribution Types
-----

There are 4 types available:

- **only-script**: the lite implementation of `mvnw`/`mvnw.cmd` scripts will download the maven directly via wget or curl on *nix, or PowerShell on Windows,
then exec/call the original `mvn`/`mvn.cmd` scripts of the downloaded maven distribution. This type does not use `maven-wrapper.jar` nor `MavenWrapperDownloader.java`,
only the wrapper scripts are required.

- **script**: With this type the scripts will try to download the scripts via wget or curl in case of Unix based system, or use Powershell in case of Windows

- **bin** _(default)_: With this type the maven-wrapper jar is already available in the `.mvn/wrapper` folder, so you don't have to rely on wget/curl or Powershell 
The downside is that the project will contain a binary file in the source control management system.

- **source**: Since Maven already requires Java to run, why not compile and run a classfile to download the maven-wrapper jar? 
This type comes with a `.mvn/wrapper/MavenWrapperDownloader.java` which will be compiled and executed on the fly.

The type can be specified with `mvn wrapper:wrapper -Dtype=x`, where x is any of the types listed above.

Maven Version
-------------
By default the plugin will assume the same version as the Maven runtime (calling `mvn -v`). But you can pick a different version.
Either call `mvn wrapper:wrapper -Dmaven=x`, where x is any valid Apache Maven Release, see https://search.maven.org/artifact/org.apache.maven/apache-maven
Another option is adjust the `distributionUrl` in `.mvn/wrapper/maven-wrapper.properties`

Debugging
---------

The apache-maven-wrapper distributions all contains the `mvnwDebug`-script for both Windows and Unix based operating systems. 
This makes it possible to debug through Apache Maven, Maven Plugin or Maven Extension. 
You can include these scripts by calling `mvn wrapper:wrapper -DincludeDebug=true`
