<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Maven Wrapper

The Maven Wrapper is an easy way to ensure a user of your Maven build has
everything necessary to run your Maven build.

_Why might this be necessary?_ Maven to date has been very stable for users, is
available on most systems or is easy to procure: but with many of the recent
changes in Maven it will be easier for users to have a fully encapsulated build
setup provided by the project. With the Maven Wrapper, this is very easy to do
and it's a great idea borrowed from Gradle.

The easiest way to setup the Maven Wrapper for your project is to use the
[Maven Wrapper Plugin][1] with its provided `wrapper` goal. To add or update all
the necessary Maven Wrapper files to your project execute the following command:

```bash
mvn wrapper:wrapper
```

Normally you instruct users to install a specific version of Apache Maven, put
it on the PATH and then run the `mvn` command like the following:

```bash
mvn clean install
```

But now, with a Maven Wrapper setup, you can instruct users to run wrapper
scripts:

```bash
./mvnw clean install
```

or on Windows

```bash
mvnw.cmd clean install
```

A normal Maven build will be executed, with the one important change that if the
user doesn't have the necessary version of Maven specified in
`.mvn/wrapper/maven-wrapper.properties` it will be downloaded for the user
first, installed and then used.

Subsequent uses of `mvnw`/`mvnw.cmd` use the previously downloaded, specific
version as needed.

## Supported Systems

The wrapper should work on various operating systems including

* Linux (numerous versions, tested on Ubuntu and CentOS)
* OSX / macOS
* Windows (various newer versions)
* Solaris (10 and 11)

For all those *nix operating systems, various shells should work including

* sh
* bash
* dash
* zsh

In terms of Apache Maven versions itself, the wrapper should work with any Maven
3.x version and it defaults to the release used when setting up the wrapper. We do NOT
plan to support the deprecated, EOL'ed Maven 2.x.

The `maven-wrapper.jar` (to download and install target Maven version required by `mvnw`) and the Maven Wrapper Plugin (to inject Wrapper into a project) use Java 7.

## Verbose Mode

The wrapper supports a verbose mode in which it outputs further information. It
is activated by setting the `MVNW_VERBOSE` environment variable to `true`.

By default it is off.

## Usage without Binary JAR

By default, the Maven Wrapper JAR archive is added to the using project as small
binary file `.mvn/wrapper/maven-wrapper.jar`. It is used to bootstrap the
download and invocation of Maven from the wrapper shell scripts.

If your project is not allowed to contain binary files like this, you can
configure your version control system to exclude checkin/commit of the wrapper
jar.

If the JAR is not found to be available by the scripts they will attempt to
download the file from the URL specified in
`.mvn/wrapper/maven-wrapper.properties` under `wrapperUrl` and put it in place.
The download is attempted via curl, wget and, as last resort, by compiling the
inline java downloader source and executing the resulting class.

If your Maven repository is password protected you can specify your username via
the environment variable `MVNW_USERNAME` and the password via the environment
variable `MVNW_PASSWORD`.

## Using a Different Version of Maven

To switch the version of Maven used to build a project, you can initialize it
using:

```bash
mvn wrapper:wrapper -Dmaven=3.5.4
```

which works for any version except snapshots. Once you have a wrapper you can
change its version by setting the `distributionUrl` in
`.mvn/wrapper/maven-wrapper.properties`, e.g.

```bash
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.5.4/apache-maven-3.5.4-bin.zip
```

## Using Basic Authentication for Distribution Download

To download Maven from a location that requires Basic Authentication you have 2
options:

1. Set the environment variables `MVNW_USERNAME` and `MVNW_PASSWORD`

    or

2. add user and password to the distributionUrl like that:
`distributionUrl=https://username:password@<yourserver>/maven2/org/apache/maven/apache-maven/3.2.1/apache-maven-3.2.1-bin.zip`

[1]: ./maven-wrapper-plugin

## Specifying Maven Distribution Base Path

<!--- TODO: check this feature -->

This is a feature of Maven itself and the wrapper just happens to take it into
account. Simply set `MAVEN_USER_HOME` to the desired path and the wrapper uses
it as the base of the Maven distro installation.

See https://www.lewuathe.com/maven-wrapper-home.html and
https://github.com/takari/maven-wrapper/issues/17

## Using a Maven Repository Manager

When using an internal Maven repository manager, you have two options:

1. Just set the correct URL to wrapper jar and Maven distro in
  `maven-wrapper.properties` in your project
2. Leave the default URL in the project pointing to Maven Central and set the
  environment variable `MVNW_REPOURL` to your repo manager URL such as
  `https://repo.example.com/central-repo-proxy`.

If `MVNW_REPOURL` is set during the wrapper installation with the
maven-wrapper-plugin, the URL is used in the maven-wrapper.properties file.

If not set, but your mirror URL in your settings.xml is configured, it will be
used.

## Internals

Maven Wrapper is composed of 3 pieces:
 
1. [maven-wrapper](./maven-wrapper/), providing the `maven-wrapper.jar` that downloads, installs and runs target Maven distribution,

2. [maven-wrapper-distribution](./maven-wrapper-distribution/), that provides `mvnw`/`mvnw.cmd` scripts distributions,

3. [maven-wrapper-plugin](./maven-wrapper-plugin/), the `wrapper` plugin to ease installing Wrapper into a project.
