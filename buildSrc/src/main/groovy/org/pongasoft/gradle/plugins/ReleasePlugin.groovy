/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2013-2021 Yan Pujante
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

package org.pongasoft.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.bundling.Jar
import org.pongasoft.gradle.core.BuildInfo
import org.pongasoft.gradle.utils.Utils

/**
 * This plugin adds a 'release' task (can be changed with convention)
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

  Project project

  void apply(Project project)
  {
    this.project = project

    BuildInfo buildInfo = BuildInfo.findOrCreate(project)

    project.getPlugins().apply(BasePlugin.class)
    project.getPlugins().apply(ReleaseTypePlugin.class)
    project.getPlugins().apply("publishing")

    // optional as publishing can be defined inline
    Utils.applyPluginIfExists(project, ExternalPublishingPlugin)

    def extension = project.extensions.create("release", ReleasePluginExtension)

    // creating releaseMaster configuration
    Configuration releaseMasterConfiguration = findOrAddConfiguration(project, RELEASE_MASTER_CONFIGURATION)

    /**
     * Needs to be executed after the build script has been evaluated
     */
    project.afterEvaluate {

      // 1. Add add all the release configurations to releaseMaster
      extension.releaseConfigurations.each { String configurationName ->
        addToReleaseMaster(project, configurationName)
      }

      // 2. Add sources and docs
      addSourcesAndDocs(extension)

      // 3. setup the publication
      def publication = project.publishing.publications.findByName(extension.publicationName)
      if(publication) {
        releaseMasterConfiguration.allArtifacts.each { artifact ->
          publication.artifact(artifact)
        }
      }

      /********************************************************
       * task: release
       ********************************************************/
      def releaseRepositoryName = extension.repositoryName

      def releaseRepository = project.publishing.repositories.findByName(releaseRepositoryName)

      if(releaseRepository != null && publication != null) {
        Task releaseMasterTask = project.tasks.register("releaseMaster") {
          dependsOn("publish${publication.name.capitalize()}PublicationTo${releaseRepository.name.capitalize()}Repository")
        }.get()

        // we add the artifacts that were just released to the build info
        releaseMasterTask.doLast {
          buildInfo.addReleasedArtifacts(project, releaseMasterConfiguration)
        }

        project.task([dependsOn: releaseMasterTask,
                      description: "Releases in a releaseRepo [${releaseRepositoryName}]"],
            extension.taskName)
      } else {
        if(!releaseRepository)
          project.logger.warn("[${project.name}]: Could not find a repository named [${releaseRepositoryName}] in publishing.repositories (either define one in your build file or in the allRespositories.publishing section of repositories.gradle)")
        if(!publication)
          project.logger.warn("[${project.name}]: Could not find a publication named [${extension.publicationName}] in publishing.publications")
      }
    }
  }

  /**
   * Look for java/groovy to add javadoc/groovydoc and sources
   */
  private void addSourcesAndDocs(ReleasePluginExtension extension) {
    boolean hasSources = false
    def javaSources = project.tasks.findByName('javadoc')?.source
    if (javaSources && !javaSources.isEmpty()) {
      /********************************************************
       * task: javadocJar
       ********************************************************/
      project.task([type: Jar, dependsOn: "javadoc"], 'javadocJar') {
        archiveClassifier.set('javadoc')
        from project.javadoc.destinationDir
      }

      hasSources = true
    }

    def groovySources = project.tasks.findByName('groovydoc')?.source
    if (groovySources && !groovySources.isEmpty()) {
      /********************************************************
       * task: groovydocJar
       ********************************************************/
      project.task([type: Jar, dependsOn: "groovydoc"], 'groovydocJar') {
        archiveClassifier.set('groovydoc')
        from project.groovydoc.destinationDir
      }

      hasSources = true
    }

    if (hasSources) {
      /********************************************************
       * task: sourcesJar
       ********************************************************/
      project.task([type: Jar, dependsOn: "classes"], 'sourcesJar') {
        archiveClassifier.set('sources')
        from project.sourceSets.main.allSource
      }
    }

    [
        'sourcesJar'  : extension.sourcesConfigurations,
        'javadocJar'  : extension.javadocConfigurations,
        'groovydocJar': extension.groovydocConfigurations
    ].each { taskName, configurations ->
      if (project.tasks.findByName(taskName)) {
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
      configuration = project.configurations.create(configurationName)
    return configuration
  }
}

class ReleasePluginExtension
{
  def releaseConfigurations = ['archives'] as Set
  def sourcesConfigurations = [sources: []]
  def javadocConfigurations = [javadoc: ['docs']]
  def groovydocConfigurations = [groovydoc: ['docs']]

  def taskName = "release"

  def repositoryName = "release"
  def publicationName = "release"
}
