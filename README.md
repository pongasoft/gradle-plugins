1 - Introduction
================
This project contains a set of useful gradle plugins

2 - Usage
=========
In order to use the plugins you need to add this to your build script:

        buildscript {
          dependencies {
            classpath 'org.pongasoft:org.pongasoft.gradle-plugins:3.0.0'
          }
        }

Note that since 3.0.0, this plugin is now published in maven central.

3 - Plugins
===========

3.0 - Release type concept
--------------------------
Most plugins exposes the `releaseType` property (of type `org.pongasoft.gradle.core.ReleaseType`) which is an 
enumeration describing for what type of release the current build is being done. 

- whether the build is producing snapshots or not (determined by whether the version ends with `-SNAPSHOT`). If you use
  the `org.pongasoft.spec` as described below then it will be in snapshot mode unless you provide `-Prelease=true`
  when building.
- whether the release is being done in a local repository or remote repository (determined by the `remote` property
  which can be set by `-Premote=true` when building)

Note that this concept is exported by most plugins, but is actually not being used by them. It is a convenience that allows
you to conditionally setup publications and repositories or create/disable tasks based on this value. See `publishing.gradle`
for a good example on how this property is being used to dynamically define the kind of publication and repository to use.

  
3.1 - `org.pongasoft.userConfig`
-------------------------------
This plugin plugin attempts to load user configuration (for the gradle
build) in the following files (values read last overrides previous values) and make it available
to all gradle build files as a `userConfig` object (instance of `groovy.util.ConfigObject`). 
Check [groovy.util.ConfigSlurper](http://groovy-lang.org/gapi/groovy/util/ConfigSlurper.html) for details on the syntax.

        userConfig.properties
        userConfig-${project.group}.properties
        userConfig-${project.name}.properties
        userConfig-${project.group}-${project.name}.properties

Those files are looked into the following folders:

        ./${filename}
        ${user.home}/.${project.group}/${filename}
        ${user.home}/.gradle/${filename}
        ${user.home}/.${filename}

You can provide your own location by using `-PuserConfig.properties=...` (this will override the
entire list).

This plugin should be used only in the root project when doing a multi project build.

3.2 - `org.pongasoft.spec`
-------------------------
This plugin reads a file called `project-spec.groovy` (or
`project-spec.json`) and makes it available in all build files as a `spec` object (instance of
`java.util.Map`).

This plugin automatically handles `spec.version` in this fashion: always force
snapshot mode (meaning version ends with `-SNAPSHOT`) unless `-Prelease=true` is provided when
running the build. See an [example](https://github.com/pongasoft/gradle-plugins/blob/master/project-spec.groovy) of
this file and how it is being used in this project itself!

The recommended approach is to define it this way (at the root level)

        apply plugin: 'org.pongasoft.spec'

        allprojects {
          group = spec.group
          version = spec.version
        }



3.3. - `org.pongasoft.externalRepositories`
-------------------------------------------
This plugin allows you to externalize repository configuration
and override it with your own defaults (for example if you do not want to use maven central). Similarly to the 
`org.pongasoft.userConfig` plugin, it reads an optional set of files (values
read last overrides previous values) and makes it available to all build files as a
`externalRepositories` object (instance of [org.pongasoft.gradle.core.RepositoryPluginExtension](https://github.com/pongasoft/gradle-plugins/blob/master/buildSrc/src/main/groovy/org/linkedin/gradle/core/RepositoryPluginExtension.groovy)).

        repositories.gradle
        repositories-${project.group}.gradle
        repositories-${project.rootProject.name}.gradle
        repositories-${project.name}.gradle
        repositories-${project.group}-${project.name}.gradle
        repositories-${project.rootProject.name}-${project.group}.gradle
        repositories-${project.rootProject.name}-${project.name}.gradle
        repositories-${project.rootProject.name}-${project.group}-${project.name}.gradle

Those files are looked into the following folders:

        ${project.rootProject.projectDir}/${filename} // if different from .
        ./${filename}
        ${user.home}/.${project.group}/${filename}
        ${user.home}/.gradle/${filename}
        ${user.home}/.${filename}

You can provide your own location by using `-Prepositories.gradle=...` (this will override the
entire list).

You can define any repository names, and they are made available into any build script:

        buildscript {
          externalRepositories.buildscript.configure()
        }

        externalRepositories.build.configure()


This plugin no longer supports bintray following [JFrog sunsetting bintray](https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/)

Check the `repositories.gradle` file that comes with this project for examples.

3.4 - `org.pongasoft.release`
----------------------------
This plugin does the following (by default):

- creates a `releaseMaster` configuration which extends `archives`
- creates `javadocJar`, `groovydocJar` and `sourcesJar` tasks (depending on the kind of artifacts built)
- creates a `release` task which automatically publishes all the artifacts that are part of the `releaseMaster` configuration using a publication named `release` in a repository named `release`

This plugin is highly configurable:

- you can change the configurations (`release.releaseConfigurations`, `release.sourcesConfigurations`, `release.javadocConfigurations`, `release.groovydocConfigurations`)
- you can change the name of the task, repository or publication (all defaulting to `release`)
- you can disable the creation of the task entirely by setting `release.repositoryName` (or `release.publicationName`) to `null`
- you can invoke `release.createConfigurationPublicationTask(...)` to create your own task(s) to publish a particular configuration in a particular combination of publication/repository (useful after disabling the main task generation)

This plugin exports the `releaseType` concept.

Note that since 3.0.0, this plugin is no longer aware of snapshots or remote as this is directly handled in the 
publication itself by using the `releaseType` concept.

3.5 - `org.pongasoft.cmdline`
----------------------------
This plugin adds the following tasks:

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

This plugin is highly configurable through the [CmdlinePluginConvention](https://github.com/pongasoft/gradle-plugins/blob/master/buildSrc/src/main/groovy/org/linkedin/gradle/plugins/CmdlinePlugin.groovy)
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

If the `buildInfo` object is defined, this plugin will automatically save a `build.info` file at
the root of the package. To disable this feature use:

        cmdline {
          noBuildInfo = true
        }

3.6 - `org.pongasoft.buildInfo`
-------------------------------
This plugin adds the property `buildInfo` to the root project and
generates a file (at the root of the build) when the build completes. The content of this file is
a json representation of the `buildInfo`. Example of content:

        {
            "name": "gradle-plugins",
            "version": "2.1.0",
            "scmUrl": "git@github.com:pongasoft/gradle-plugins.git",
            "scmCommitVersion": "24134d260317bc077ba76f590e114ec2740e6117",
            "gradleVersion": "1.5",
            "jvm": "1.7.0_21 (Oracle Corporation 23.21-b01)",
            "os": "Mac OS X 10.8.3 x86_64",
            "buildTime": 1367010673446,
            "buildTimeString": "2013/04/26 11:11:13 HST",
            "buildDuration": 13996,
            "buildDurationString": "13.996 secs",
            "buildTasks": [
                "release"
            ]
        }

3.7 - `org.pongasoft.signing`
-----------------------------
This plugin does the following:

- automatically populates `signing.keyId`, `signing.password` and `signing.secretKeyRingFile` properties
  from values defined either in `userConfig` file (see `org.pongasoft.userConfig` plugin), `project-spec.groovy`
  (see `org.pongasoft.spec` plugin), command line (ex: `-Psigning.keyId=xxx`) or an environment variable
  (ex: `SIGNING_KEY`)
- apply the `signing` plugin to the publications (`sign project.publishing.publications`)

3.8 - `org.pongasoft.externalPublishing`
----------------------------------------
This plugin allows you to externalize publishing configuration and override it with your own defaults. Similarly to the 
`org.pongasoft.userConfig` plugin, it reads an optional set of files (values read last overrides previous values). See
`publishing.gradle` in this project for an example.

        publishing.gradle
        publishing-${project.group}.gradle
        publishing-${project.rootProject.name}.gradle
        publishing-${project.name}.gradle
        publishing-${project.group}-${project.name}.gradle
        publishing-${project.rootProject.name}-${project.group}.gradle
        publishing-${project.rootProject.name}-${project.name}.gradle
        publishing-${project.rootProject.name}-${project.group}-${project.name}.gradle

Those files are looked into the following folders:

        ${project.rootProject.projectDir}/${filename} // if different from .
        ./${filename}
        ${user.home}/.${project.group}/${filename}
        ${user.home}/.gradle/${filename}
        ${user.home}/.${filename}

You can provide your own location by using `-Ppublishing.gradle=...` (this will override the entire list).

3.9 - `org.pongasoft.sonatypePublishing`
----------------------------------------
This plugin adds the `sonatype` extension property (of type `org.pongasoft.gradle.plugins.SonatypePublishingExtension`) to allow 
easy configuration when publishing to maven central. For example:

        repositories {
          maven {
            name = "release"
            url = sonatype.s01.conditionedOn(releaseType)
            credentials {
                username = sonatype.username
                password = sonatype.password
            }
          }
        }

`sonatype.username` and `sonatype.password` behaves like the `org.pongasoft.signing` plugin in how to populate this
values (user config, project spec, command line or environment variable).

4 - Compilation
===============
In order to compile the code you need

* java 1.8+

At the top simply run

        ./gradlew test

which should compile and run all the tests.

5 - Directory structure
=======================
* `buildSrc`
  *  Contains the code of the plugins

* `org.pongasoft.gradle-plugins`
  * Project that creates the jar file for the plugins (using the `java-gradle-plugin` plugin) so that it can be 
    released

6 - Build configuration
=======================
The project uses the `org.pongasoft.userConfig` plugin and as such can be configured the way
described in the plugin

        Example:
        ~/.userConfig.properties
        top.build.dir="/Volumes/Disk2/deployment/${userConfig.project.name}"
        top.release.dir="/export/content/repositories/release"
