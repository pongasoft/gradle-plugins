2.2.6 (2014/02/21)
------------------
* use of `gradle 1.11`

2.2.5 (2013/11/24)
------------------
* use of `gradle 1.9`

2.2.4 (2013/08/13)
------------------
* use of `gradle 1.7`

2.2.3 (2013/07/20)
------------------
* Fixed [#2](https://github.com/pongasoft/gradle-plugins/issues/2): _release task must build packages_
* allow to conditionally create package-install task

2.2.1 (2013/05/31)
------------------
* use gradle built-in tar task to ensure executable files maintain their execution flag

2.2.0 (2013/05/10)
------------------
* use of `gradle 1.6`
* fixed small issue when build dir unavailable

2.1.0 (2013/04/26)
------------------
* Implemented [#1](https://github.com/pongasoft/gradle-plugins/issues/1): _Allow for "orphan" publishing_

  * Now by default, `publish` does not do a build and simply publishes what has been released in a
    previous build.

* Added `org.pongasoft.buildInfo` to generate a `build.info` file


2.0.2 (2013/04/23)
------------------
* handle include/exclude for replacement tokens

2.0.1 (2013/04/21)
------------------
* fixed username/password in ivy distribution

2.0.0 (2013/04/20)
------------------
* use of jdk1.7
* use of `gradle 1.5`
* added support for bintray and publish to [bintray](https://bintray.com/pkg/show/general/pongasoft/binaries/gradle-plugins) (jcenter)
* forked project under [pongasoft/gradle-plugins](https://github.com/pongasoft/gradle-plugins)

1.6.0 (2013/03/25)
------------------
* use of `gradle 1.4`
* added `gradlew` so that you don't need to install gradle anymore

1.5.0 (2010/12/20)
------------------
* changed `org.linkedin.cmdline` plugin to allow more configurable `resources` (every entry can now be a map)
* use of official `gradle 0.9` release!

1.4.0 (2010/12/14)
------------------
* changed `org.linkedin.cmdline` plugin to use a `resources` convention (instead of `resourcesDir`) which is a list of whatever can be provided as an input of `CopySpec.from` (with a default of `'src/cmdline/resources'`)

        Example:
        cmdline {
          resources << fileTree(dir: rootDir, includes: ['*.txt', '*.md'])
        }

1.3.1 (2010/12/02)
------------------
* changed `org.linkedin.spec` to be in snapshot mode by default and use `-Prelease=true` to force non snapshot
* simplified configuration in `org.linkedin.release`

1.2.0 (2010/11/28)
------------------
* `org.linkedin.cmdline` uses a `lib` configuration by default. Example:

        dependencies {
          lib project('a:'b:c)
        }

1.1.0 (2010/11/25)
------------------
* handle javadoc/groovydoc/sources properly

1.0.0 (2010/11/20)
------------------
* First release