/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2013 Yan Pujante
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
import org.gradle.api.plugins.BasePlugin

/**
 * This plugin adds a 'release' and 'publish' task.
 *
 * <p>For 'release', it uses the 'releaseMaster' configuration and configure the
 * <code>uploadReleaseMaster</code> task with the following repositories
 * <ul>
 * <li>release => uses allRespositories.release</li>
 * <li>release (snapshot mode) => uses allRespositories.snapshotRelease</li>
 * </ul>
 *
 * <p>For 'publish', it uses the 'publishMaster' configuration and configure the
 * <code>uploadPublishMaster</code> task with the following repositories
 * <ul>
 * <li>publish => uses allRespositories.publish</li>
 * <li>publish (snapshot mode) => uses allRespositories.snapshotPublish</li>
 * </ul>
 * 
 * If you want your artifacts to be 'released'/'published' then simply add them to either
 * the 'releaseMaster' configuration or to a new configuration and make 'releaseMaster' extends
 * from it.
 *
 * By convention ({@link ReleasePluginExtension#releaseConfigurations}) 'releaseMaster' extends
 * from 'archives'.
 *
 * @author ypujante@linkedin.com */
class ReleasePlugin implements Plugin<Project>
{
  public static final String RELEASE_MASTER_CONFIGURATION = 'releaseMaster'
  public static final String PUBLISH_MASTER_CONFIGURATION = 'publishMaster'

  void apply(Project project)
  {
    project.getPlugins().apply(BasePlugin.class);

    if(!project.plugins.hasPlugin('org.linkedin.repository'))
    {
      project.apply plugin: RepositoryPlugin
    }

    def extension = project.extensions.create("release", ReleasePluginExtension)

    // creating the configurations
    findOrAddConfiguration(project, RELEASE_MASTER_CONFIGURATION)
    addExtendsFrom(project, PUBLISH_MASTER_CONFIGURATION, RELEASE_MASTER_CONFIGURATION)

    /**
     * Needs to be executed after the build script has been evaluated
     */
    project.afterEvaluate {

      extension.releaseConfigurations.each { String configurationName ->
        addToReleaseMaster(project, configurationName)
      }

      def releaseRepositoryName =
        project.version.endsWith('-SNAPSHOT') ? extension.snapshotRelease : extension.release
      def publishRepositoryName =
        project.version.endsWith('-SNAPSHOT') ? extension.snapshotPublish : extension.publish

      // handling release
      project.uploadReleaseMaster {
        def releaseRepo = RepositoryPlugin.findRepository(project, releaseRepositoryName)
        if(releaseRepo)
          releaseRepo.configure(repositories)
        else
          project.logger.warn("Could not locate configuration for release [${releaseRepositoryName}]")
      }

      // handling publish
      project.uploadPublishMaster {
        def publishRepo = RepositoryPlugin.findRepository(project, publishRepositoryName)
        if(publishRepo)
          publishRepo.configure(repositories)
        else
          project.logger.warn("Could not locate configuration for publish [${publishRepositoryName}]")
      }

      /********************************************************
       * task: release
       ********************************************************/
      project.task([dependsOn: 'uploadReleaseMaster',
                   description: "Releases in a releaseRepo [${releaseRepositoryName}]"],
                   'release')

      /********************************************************
       * task: publish
       ********************************************************/
      project.task([dependsOn: 'uploadPublishMaster',
                   description: "Publishes in a releaseRepo [${publishRepositoryName}]"],
                   'publish')

      boolean hasSources = false
      def javaSources = project.tasks.findByName('javadoc')?.source
      if(javaSources && !javaSources.isEmpty())
      {
        /********************************************************
         * task: javadocJar
         ********************************************************/
        project.task([type: Jar, dependsOn: "javadoc"], 'javadocJar') {
            classifier = 'javadoc'
            from project.javadoc.destinationDir
        }

        hasSources = true
      }

      def groovySources = project.tasks.findByName('groovydoc')?.source
      if(groovySources && !groovySources.isEmpty())
      {
        /********************************************************
         * task: groovydocJar
         ********************************************************/
        project.task([type: Jar, dependsOn: "groovydoc"], 'groovydocJar') {
            classifier = 'groovydoc'
            from project.groovydoc.destinationDir
        }

        hasSources = true
      }

      if(hasSources)
      {
        /********************************************************
         * task: sourcesJar
         ********************************************************/
        project.task([type: Jar, dependsOn: "classes"], 'sourcesJar') {
            classifier = 'sources'
            from project.sourceSets.main.allSource
        }
      }

      [
          'sourcesJar': extension.sourcesConfigurations,
          'javadocJar': extension.javadocConfigurations,
          'groovydocJar': extension.groovydocConfigurations
      ].each { taskName, configurations ->
        if(project.tasks.findByName(taskName))
        {
          configurations.each { configurationName, extendsFroms ->
            addToReleaseMaster(project, configurationName)
            extendsFroms?.each { addExtendsFrom(project, configurationName, it) }
            project.artifacts {
              "${configurationName}" project."${taskName}"
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

    def mrc = project.configurations.findByName(RELEASE_MASTER_CONFIGURATION)

    Configuration ac = null
    if(mrc)
    {
      (mrc, ac) = addExtendsFrom(project,
                                 RELEASE_MASTER_CONFIGURATION,
                                 configuration)
    }

    return ac
  }

  /**
   * Equivalent to <code>configuration.extendsFrom(extendsFromConfiguration)</code> but do proper
   * checking to create the configuration if does not exist in the first place.
   *
   * @return configuration
   */
  static def addExtendsFrom(Project project,
                            String configurationName,
                            String extendsFromConfigurationName)
  {
    Configuration configuration = findOrAddConfiguration(project, configurationName)
    Configuration extendsFromConfiguration =
      findOrAddConfiguration(project, extendsFromConfigurationName)
    if(configuration != extendsFromConfiguration &&
       !configuration.extendsFrom.contains(extendsFromConfiguration))
      configuration.extendsFrom(extendsFromConfiguration)
    return [configuration, extendsFromConfiguration]
  }

  /**
   * Find or add a configuration if does not exist yet
   * @return the configuration
   */
  static Configuration findOrAddConfiguration(Project project, String configurationName)
  {
    Configuration configuration = project.configurations.findByName(configurationName)
    if(!configuration)
      configuration = project.configurations.add(configurationName)
    return configuration
  }

}

class ReleasePluginExtension
{
  def releaseConfigurations = ['archives'] as Set
  def sourcesConfigurations = [sources: []]
  def javadocConfigurations = [javadoc: ['docs']]
  def groovydocConfigurations = [groovydoc: ['docs']]

  String snapshotRelease = 'snapshotRelease'
  String release = 'release'
  String snapshotPublish = 'snapshotPublish'
  String publish = 'publish'
}
