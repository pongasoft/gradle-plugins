1. Introduction
===============
This project contains a set of useful gradle plugins

2. Usage
========
In order to use the plugins you need to add this to your build script:

        buildscript {
          repositories {
            mavenCentral()
          }
          dependencies {
            classpath 'org.linkedin:org.linkedin.gradle-plugins:1.3.1'
          }
        }

3. Plugins
==========
  
3.1 `org.linkedin.userConfig`
-----------------------------
`org.linkedin.userConfig` is a plugin which attempts to load user configuration (for the gradle
build) in the following files (values read last overrides previous values) and make it available
to all gradle build files as a `userConfig` object (instance of `groovy.util.ConfigObject`). 
Check [groovy.util.ConfigSlurper](http://groovy.codehaus.org/gapi/groovy/util/ConfigSlurper.html) for details on the syntax.

        ./userConfig.properties
        ./userConfig-${project.name}.properties
        ${user.home}/.org.linkedin/userConfig.properties
        ${user.home}/.org.linkedin/userConfig-${project.name}.properties
        ${user.home}/.gradle/userConfig.properties
        ${user.home}/.gradle/userConfig-${project.name}.properties
        ${user.home}/.userConfig.properties
        ${user.home}/.userConfig-${project.name}.properties

You can provide your own location by using `-PuserConfig.properties=...` (this will override the
entire list).

This plugin should be used only in the root project when doing a multi project build.

3.2 `org.linkedin.spec`
-----------------------
`org.linkedin.spec` is a plugin which reads a file called `project-spec.groovy` (or
`project-spec.json`) and makes it available in all build files as a `spec` object (instance of
`java.util.Map`). This plugin automatically handles `spec.version` in this fashion: always force
snapshot mode (meaning version ends with `-SNAPSHOT`) unless `-Prelease=true` is provided when
running the build. See an [example](https://github.com/linkedin/gradle-plugins/blob/master/project-spec.groovy) of 
this file and how it is being used in this project itself!

3.3. `org.linkedin.repository`
------------------------------
`org.linkedin.repository` is a plugin which allows you to externalize repository configuration
and override it with your own defaults (for example if you do not want to use maven central). In a
similar fashion to the `org.linkedin.userConfig` plugin, it reads an optional set of files (values
read last overrides previous values) and makes it available to all build files as a
`allRepositories` object (instance of [org.linkedin.gradle.core.RepositoryHandlerContainer](https://github.com/linkedin/gradle-plugins/blob/master/buildSrc/src/main/groovy/org/linkedin/gradle/core/RepositoryHandlerContainer.groovy)).

        ./repositories.gradle
        ./repositories-${project.name}.gradle
        ${user.home}/.org.linkedin/repositories.gradle
        ${user.home}/.org.linkedin/repositories-${project.name}.gradle
        ${user.home}/.gradle/repositories.gradle
        ${user.home}/.gradle/repositories-${project.name}.gradle
        ${user.home}/.repositories.gradle
        ${user.home}/.repositories-${project.name}.gradle

You can provide your own location by using `-Prepositories.gradle=...` (this will override the 
entire list).

This plugin should be used only in the root project when doing a multi project build.

3.4 `org.linkedin.release`
--------------------------
`org.linkedin.release` is a plugin which adds `release` and `publish` tasks. `release` is supposed
to build and release in a local repository. `publish` is supposed to build and publish in a remote
repository. None of this is enforced and you can still use whichever convention you want. If it is a
java or groovy project, it also releases/publishes sources, javadoc and groovydoc. The plugin also
knows about snapshots (where the version ends with `-SNAPSHOT`). The repositories are configured
using the `org.linkedin.repository` plugin with the following values:

        allRepositories.release -> for release
        allRepositories.snapshotRelease -> for release of snapshots
        allRepositories.publish -> for publish
        allRepositories.snapshotPublish -> for publish of snapshots

See [repositories.gradle](https://github.com/linkedin/gradle-plugins/blob/master/repositories.gradle) 
for an example of configuration.

This plugin is used in every project that needs to be released.

3.5 `org.linkedin.cmdline`
--------------------------
`org.linkedin.cmdline` is a plugin which adds the following tasks:

* `package-assemble`: Assembles the package (exploded)
* `package`: Create the package
* `package-install`: Install the package (locally)
* `package-clean-install`: Cleans the (previously) installed package

and a `lib` configuration on which you add your dependencies.

By convention (configurable), the result of the `package` task is a tgz which contains a directory
structure like this:

        lib/*.jar (all dependencies)
        + whatever directory structure was under 'src/cmdline/resources' (also configurable)

If there is a `src/cmdline/resources/bin` folder, then all files in this folder will be tarred up
as executables.

All files under `src/cmdline/resources` are also processed through a replacement token plugin if
you provide replacement tokens.

This plugin is highly configurable through the [CmdLinePluginConvention](https://github.com/linkedin/gradle-plugins/blob/master/buildSrc/src/main/groovy/org/linkedin/gradle/plugins/CmdLinePlugin.groovy) 
available in the build file
as `cmdline`:

        example:
        dependencies {
          lib project(':project1')
          lib 'org.json:json:20090211'
        }
        cmdline {
          replacementTokens = [__version__: project.version]
          resources << fileTree(dir: rootDir, includes: ['*.txt', '*.md'])
        }

4. Compilation
==============
In order to compile the code you need

* java 1.6
* [gradle 0.9-rc2](http://www.gradle.org/)

At the top simply run

        gradle test

which should compile and run all the tests.

5. Directory structure
======================
* `buildSrc`
  *  Contains the code of the plugins

* `org.linkedin.gradle-plugins`
  * Simple wrapper which uses the plugin themselves to recompile them and make them available for
release/publish

6. Build configuration
======================
The project uses the `org.linkedin.userConfig` plugin and as such can be configured the way
described in the plugin

        Example:
        ~/.userConfig.properties
        top.build.dir="/Volumes/Disk2/deployment/${userConfig.project.name}"
        top.install.dir="/export/content/${userConfig.project.name}"
        top.release.dir="/export/content/repositories/release"
        top.publish.dir="/export/content/repositories/publish"