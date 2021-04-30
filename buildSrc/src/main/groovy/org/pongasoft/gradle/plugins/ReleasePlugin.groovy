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
import org.pongasoft.gradle.core.PublishArtifactImpl
import org.pongasoft.gradle.utils.Utils

/**
 * This plugin adds a 'release' task (can be changed with convention)
 *
 * If you want your artifacts to be released then simply add them to either
 * the 'releaseMaster' configuration or to a new configuration and make 'releaseMaster' extends
 * from it.
 *
 * @author ypujante@linkedin.com */
class ReleasePlugin implements Plugin<Project>
{
  public static final String RELEASE_MASTER_CONFIGURATION = 'releaseMaster'
  public static final String RELEASE_MASTER_TASK_NAME = 'releaseMaster'

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

    def extension = project.extensions.create("release", ReleasePluginExtension, project)

    // creating releaseMaster configuration
    Configuration releaseMasterConfiguration = findOrAddConfiguration(project, RELEASE_MASTER_CONFIGURATION)

    // Create the task so that it is available in other plugins
    project.tasks.register(RELEASE_MASTER_TASK_NAME)

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

      // 3. Populate releaseMaster task
      if(extension.publicationName && extension.repositoryName) {
        Task releaseMasterTask = extension.task(RELEASE_MASTER_TASK_NAME,
                                                extension.publicationName,
                                                extension.repositoryName,
                                                extension.releaseComponent,
                                                releaseMasterConfiguration.name)

        if(releaseMasterTask) {
          // 4. add the artifacts that were just released to the build info
          releaseMasterTask.doLast {
            buildInfo.addReleasedArtifacts(project, releaseMasterConfiguration)
            if(extension.releaseComponent)
              buildInfo.addReleasedComponent(extension.releaseComponent)
          }

          // 6. create the "release" convenient task
          project.task([dependsOn: releaseMasterTask,
                        description: "Releases [${RELEASE_MASTER_CONFIGURATION}] configuration to publication(s)"],
              extension.taskName)
        }
      }
    }
  }

  /**
   * Look for java/groovy to add javadoc/groovydoc and sources
   */
  private void addSourcesAndDocs(ReleasePluginExtension extension) {

    def javaSources = project.tasks.findByName('javadoc')?.source
    boolean hasJavaSources = javaSources && !javaSources.isEmpty()

    def groovySources = project.tasks.findByName('groovydoc')?.source
    boolean hasGroovySources = groovySources && !groovySources.isEmpty()

    boolean hasSources = hasJavaSources || hasGroovySources

    if (hasSources) {

      if(hasJavaSources && !project.tasks.findByName('javadocJar')) {
        /********************************************************
         * task: javadocJar
         ********************************************************/
          project.task([type: Jar, dependsOn: 'javadoc'], 'javadocJar') {
            archiveClassifier.set('javadoc')
            from project.javadoc.destinationDir
          }
        }

      if(hasGroovySources && !project.tasks.findByName('groovydocJar')) {
        /********************************************************
         * task: groovydocJar
         ********************************************************/
        project.task([type: Jar, dependsOn: 'groovydoc'], 'groovydocJar') {
          // maven central requires javadoc so if only groovy we need to rename it
          archiveClassifier.set(hasJavaSources ? 'groovydoc' : 'javadoc')
          from project.groovydoc.destinationDir
        }
      }

      /********************************************************
       * task: sourcesJar
       ********************************************************/
      if(!project.tasks.findByName('sourcesJar')) {
        project.task([type: Jar, dependsOn: "classes"], 'sourcesJar') {
          archiveClassifier.set('sources')
          from project.sourceSets.main.allSource
        }
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
  private final Project _project
  
  ReleasePluginExtension(Project project) {
    _project = project
  }
  
  def releaseComponent = 'java'
  def releaseConfigurations = [] as Set
  def sourcesConfigurations = [sources: []]
  def javadocConfigurations = [javadoc: ['docs']]
  def groovydocConfigurations = [groovydoc: ['docs']]

  def taskName = "release"
  def repositoryName = "release"
  def publicationName = "release"

  /**
   * Registers (or reuse if exists) a task with the provided name. The (optional) component is added to the
   * publication. The artifacts from the (optional) configuration are also added to the publication. The task
   * is a shortcut to invoking the <code>publishXXXPublicationToYYYRepository</code> with a more manageable name
   * (provided as argument to this call)
   *
   * @return the task or <code>null</code> if the configuration, component, publication or repository does not exist */
  Task task(String taskName,
            String publicationName,
            String repositoryName,
            String componentName = null,
            String configurationName = null) {

    def publication = _project.publishing.publications.findByName(publicationName)

    if(publication) {
      // add the main software components
      if(componentName) {
        def component = _project.components.findByName(componentName)
        if(component) {
          publication.from(component)
        } else {
          _project.logger.warn("[${_project.name}]: Could not find a component named [${componentName}]")
          return null
        }
      }

      // add other artifacts
      if(configurationName) {
        def configuration = _project.configurations.findByName(configurationName)
        if (configuration == null) {
          _project.logger.warn("[${_project.name}]: Could not find a configuration named [${configurationName}]")
          return null
        }

        configuration.allArtifacts.each { artifact ->
          if (artifact instanceof PublishArtifactImpl)
            artifact.addToPublication(publication)
          else
            publication.artifact(artifact)
        }
      }
    }

    def repository = _project.publishing.repositories.findByName(repositoryName)

    if(repository != null && publication != null) {
      Task task = _project.tasks.findByName(taskName) ?: _project.tasks.register(taskName).get()
      task.dependsOn("publish${publication.name.capitalize()}PublicationTo${repository.name.capitalize()}Repository")
      return task
    }

    if(!repository)
      _project.logger.warn("[${_project.name}]: Could not find a repository named [${repositoryName}] in publishing.repositories")
    if(!publication)
      _project.logger.warn("[${_project.name}]: Could not find a publication named [${publicationName}] in publishing.publications")

    return null
  }
}
