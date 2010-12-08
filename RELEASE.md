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