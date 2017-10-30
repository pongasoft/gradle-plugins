1 - Introduction
================
This project contains a set of useful gradle plugins

2 - Usage
=========
In order to use the plugins you need to add this to your build script:

        buildscript {
          repositories {
             mavenRepo(url: 'http://jcenter.bintray.com')
          }
          dependencies {
            classpath 'org.pongasoft:org.linkedin.gradle-plugins:1.7.0'
          }
        }

3 - Plugins
===========
  
3.1 - `org.linkedin.userConfig`
-------------------------------
`org.linkedin.userConfig` is a plugin which attempts to load user configuration (for the gradle
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

3.2 - `org.linkedin.spec`
-------------------------
`org.linkedin.spec` is a plugin which reads a file called `project-spec.groovy` (or
`project-spec.json`) and makes it available in all build files as a `spec` object (instance of
`java.util.Map`). This plugin automatically handles `spec.version` in this fashion: always force
snapshot mode (meaning version ends with `-SNAPSHOT`) unless `-Prelease=true` is provided when
running the build. See an [example](https://github.com/pongasoft/gradle-plugins/blob/master/project-spec.groovy) of
this file and how it is being used in this project itself!

3.3. - `org.linkedin.repository`
--------------------------------
`org.linkedin.repository` is a plugin which allows you to externalize repository configuration
and override it with your own defaults (for example if you do not want to use maven central). In a
similar fashion to the `org.linkedin.userConfig` plugin, it reads an optional set of files (values
read last overrides previous values) and makes it available to all build files as a
`allRepositories` object (instance of [org.linkedin.gradle.core.RepositoryPluginExtension](https://github.com/pongasoft/gradle-plugins/blob/master/buildSrc/src/main/groovy/org/linkedin/gradle/core/RepositoryPluginExtension.groovy)).

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

This plugin can be used only in the root project when doing a multi project build. But if you want
to access `project` specific values, then you need to apply it in each sub projects

You can define any repository name and they are made available into any build script:

        buildscript {
          allRepositories.buildscript.configure()
        }

        allRepositories.build.configure()


This plugin supports bintray by using the syntax `allRepositories.bintray.<xxx>` (for publishing)
and can use `org.linkedin.spec` and `org.linkedin.userConfig` to configure it (typically
your `bintray.apiKey` would live in your `${user.home}/.userConfig.properties` file and the rest
of the configuration in `project-spec.groovy`). If the `apiKey` is not found, then it will be
prompted! This plugin adds the `bintray` extension for easy configurations:

        allRepositories.build = {
          bintray.jcenter()
        }

        def pomConfig = {
          // ...
        }

        allRepositories.bintray.binaries = bintray.binaries.mavenRepo {
          pom.whenConfigured(pomConfig)
        }

The `binaries` repository gets configured this way in `project-spec.groovy:

        spec.bintray = [
          apiBaseUrl: 'https://bintray.com/api/v1',
          username: 'yan',
          pkgOrganization: 'pongasoft',
          repositories: [
            binaries: [
              pkgRepository: 'binaries',
              pkgName: spec.name
            ],
        ]

The `bintray` extension is configured this way for any undefined property:

        apiBaseUrl -> 'https://bintray.com/api/v1'
        username -> System.getProperty("user.name")
        apiKey -> prompted on the command line
        pkgOrganization -> username
        pkgRepository -> project.rootProject.group
        pkgName -> project.rootProject.name

Check the `repositories.gradle` file that comes with this project for examples.

3.4 - `org.linkedin.release`
----------------------------
`org.linkedin.release` is a plugin which adds `release` and `publish` tasks. `release` is supposed
to build and release in a local repository. `publish` is supposed to publish in a remote
repository. By default, `publish` will publish (without rebuilding!) what has been released when
invoking the `release` task on a previous build. This allows the following use case: build and
release (locally), do some sanity check and if everything is ok, then do a `publish` which will
simply publish what has already been built. If you want to rebuild on `publish` then simply add
the property `-Prebuild=true`. If it is a java or groovy project, it also releases/publishes
sources, javadoc and groovydoc. The plugin also knows about snapshots (where the version ends
with `-SNAPSHOT`). The repositories are configured using the `org.linkedin.repository` plugin
with the following values:

        allRepositories.release -> for release
        allRepositories.snapshotRelease -> for release of snapshots
        allRepositories.publish -> for publish
        allRepositories.snapshotPublish -> for publish of snapshots

See [repositories.gradle](https://github.com/pongasoft/gradle-plugins/blob/master/repositories.gradle)
for an example of configuration.

Note that local vs remote is not enforced and totally depends on how you set up the repositories.
By default `release` does a build and release, and `publish` does a publish of what has already
been built and released in a previous build.

The plugin adds a `release` extension which allows you to change which repository gets used on a
per project basis:

        release  {
          publish = allRepositories.distributions
        }

This plugin is used in every project that needs to be released.

3.5 - `org.linkedin.cmdline`
----------------------------
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

This plugin is highly configurable through the [CmdLinePluginConvention](https://github.com/pongasoft/gradle-plugins/blob/master/buildSrc/src/main/groovy/org/linkedin/gradle/plugins/CmdLinePlugin.groovy)
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
`org.pongasoft.buildInfo` is a plugin which adds the property `buildInfo` to the root project and
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
                "publish"
            ]
        }

4 - Compilation
===============
In order to compile the code you need

* java 1.7+

At the top simply run

        ./gradlew test

which should compile and run all the tests.

5 - Directory structure
=======================
* `buildSrc`
  *  Contains the code of the plugins

* `org.linkedin.gradle-plugins`
  * Simple wrapper which uses the plugin themselves to recompile them and make them available for
release/publish

6 - Build configuration
=======================
The project uses the `org.linkedin.userConfig` plugin and as such can be configured the way
described in the plugin

        Example:
        ~/.userConfig.properties
        top.build.dir="/Volumes/Disk2/deployment/${userConfig.project.name}"
        top.install.dir="/export/content/${userConfig.project.name}"
        top.release.dir="/export/content/repositories/release"
        top.publish.dir="/export/content/repositories/publish"