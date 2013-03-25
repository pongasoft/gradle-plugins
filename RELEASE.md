1.6.0 (2012/03/25)
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