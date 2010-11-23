/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */



package org.linkedin.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.artifacts.Configuration

/**
 * This plugin adds a 'release' task and creates a 'releaseMaster' configuration.
 * This plugin also adds a 'publish' taks and creates a 'publishMaster' configuration (extends
 * from releaseMaster) .
 * If you want your artifacts to be 'released'/'published' then simply add them to a configuration
 * and make masterRelease extends from it.
 *
 * @author ypujante@linkedin.com */
class ReleasePlugin implements Plugin<Project>
{
  public static final String RELEASE_MASTER_CONFIGURATION = 'releaseMaster'
  public static final String DOCS_CONFIGURATION = 'docs'
  public static final String SOURCES_CONFIGURATION = 'sources'
  public static final String PUBLISH_MASTER_CONFIGURATION = 'publishMaster'

  void apply(Project project)
  {
    if(!project.rootProject.plugins.hasPlugin('org.linkedin.repository'))
    {
      project.rootProject.apply plugin: RepositoryPlugin
    }

    def convention = new ReleasePluginConvention()

    project.convention.plugins.release = convention

    // creating the configurations
    def mrc = project.configurations.add(RELEASE_MASTER_CONFIGURATION)
    project.configurations.add(PUBLISH_MASTER_CONFIGURATION).extendsFrom(mrc)

    /**
     * Needs to be executed after the build script has been evaluated
     */
    project.afterEvaluate {

      convention.releaseConfigurations.each { String configurationName ->
        addToReleaseMaster(project, configurationName)
      }

      def releaseRepositoryName =
        project.version.endsWith('-SNAPSHOT') ? 'snapshotRelease' : 'release'
      def publishRepositoryName =
        project.version.endsWith('-SNAPSHOT') ? 'snapshotPublish' : 'publish'

      project.uploadReleaseMaster {
        project.allRepositories.find(releaseRepositoryName)?.configure(repositories)
      }

      project.uploadPublishMaster {
        project.allRepositories.find(publishRepositoryName)?.configure(repositories)
      }

      /********************************************************
       * task: release
       ********************************************************/
      project.task([dependsOn: 'uploadReleaseMaster',
                   description: "Releases in a repository [${releaseRepositoryName}]"],
                   'release')

      /********************************************************
       * task: publish
       ********************************************************/
      project.task([dependsOn: 'uploadPublishMaster',
                   description: "Publishes in a repository [${publishRepositoryName}]"], 
                   'publish')

      if(project.plugins.hasPlugin('java'))
      {
        /********************************************************
         * task: sourcesJar
         ********************************************************/
        project.task([type: Jar, dependsOn: "classes"], 'sourcesJar') {
            classifier = 'sources'
            from project.sourceSets.main.allSource
        }

        /********************************************************
         * task: javadocJar
         ********************************************************/
        project.task([type: Jar, dependsOn: "javadoc"], 'javadocJar') {
            classifier = 'javadoc'
            from project.javadoc.destinationDir
        }
      }

      if(project.plugins.hasPlugin('groovy'))
      {
        /********************************************************
         * task: groovydocJar
         ********************************************************/
        project.task([type: Jar, dependsOn: "groovydoc"], 'groovydocJar') {
            classifier = 'groovydoc'
            from project.groovydoc.destinationDir
        }
      }

      /**
       * Adding sources and java/groovy doc to the archives configuration when running
       * release or publish or install
       */
      def taskNames = ['release', 'publish', 'install'] as Set
      if(project.gradle.startParameter.taskNames.find { taskNames.contains(it)})
      {
        [
            'sourcesJar': SOURCES_CONFIGURATION,
            'javadocJar': DOCS_CONFIGURATION,
            'groovydocJar': DOCS_CONFIGURATION
        ].each { taskName, configuration ->
          if(project.tasks.findByName(taskName))
          {
            addToReleaseMaster(project, configuration)
            project.artifacts {
              "${configuration}" project."${taskName}"
            }
          }
        }
      }
    }
  }

  /**
   * Convenient call to add the given configuration to releaseMaster
   */
  static Configuration addToReleaseMaster(Project project, String configuration)
  {
    if(!configuration)
      return null

    def mrc = project.configurations.findByName(ReleasePlugin.RELEASE_MASTER_CONFIGURATION)

    Configuration ac = null
    if(mrc)
    {
      ac = project.configurations.findByName(configuration)
      if(!ac)
      {
        ac = project.configurations.add(configuration)
      }
      if(!mrc.extendsFrom.contains(ac))
        mrc.extendsFrom(ac)
    }

    return ac
  }
}

class ReleasePluginConvention
{
  def releaseConfigurations = ['archives'] as Set

  def release(Closure closure)
  {
    closure.delegate = this
    closure()
  }
}
