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
import org.gradle.api.artifacts.PublishException
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.util.hash.HashUtil
import org.gradle.util.hash.HashValue
import org.linkedin.gradle.core.ArtifactInfo
import org.linkedin.gradle.core.BuildInfo
import org.linkedin.gradle.core.PublishArtifactImpl
import org.linkedin.gradle.utils.Utils

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

  RepositoryPlugin repositoryPlugin
  Project project

  void apply(Project project)
  {
    this.project = project

    BuildInfo buildInfo = BuildInfo.findOrCreate(project)

    project.getPlugins().apply(BasePlugin.class);

    if(!project.plugins.hasPlugin('org.linkedin.repository'))
    {
      project.apply plugin: RepositoryPlugin
    }

    repositoryPlugin = project.plugins.getPlugin(RepositoryPlugin)

    def extension = project.extensions.create("release", ReleasePluginExtension)

    // creating release configuration
    Configuration releaseMasterConfiguration = findOrAddConfiguration(project, RELEASE_MASTER_CONFIGURATION)

    // creating publish configuration
    Configuration publishMasterConfiguration
    if(project.gradle.startParameter.taskNames.containsAll(['release', 'publish']) ||
       Utils.getConfigBooleanProjectProperty(project, 'rebuild', false))
    {
      def unused
      (publishMasterConfiguration, unused) = addExtendsFrom(project,
                                                            PUBLISH_MASTER_CONFIGURATION,
                                                            RELEASE_MASTER_CONFIGURATION)
    }
    else
    {
      publishMasterConfiguration = findOrAddConfiguration(project, PUBLISH_MASTER_CONFIGURATION)
    }

    /**
     * Needs to be executed after the build script has been evaluated
     */
    project.afterEvaluate {

      extension.releaseConfigurations.each { String configurationName ->
        addToReleaseMaster(project, configurationName)
      }

      def releaseRepositoryName =
        project.version.endsWith('-SNAPSHOT') ? extension.snapshotRelease : extension.release

      //////////////////////////////////////
      // handling release
      //////////////////////////////////////
      project.uploadReleaseMaster {
        def releaseRepo = RepositoryPlugin.findRepository(project, releaseRepositoryName)
        if(releaseRepo)
          releaseRepo.configure(repositories)
        else
          project.logger.warn("[${project.name}]: Could not locate configuration for release [${releaseRepositoryName}]")
      }

      // we add the artifacts that were just released to the build info
      project.uploadReleaseMaster.doLast {
        buildInfo.addReleasedArtifacts(project, releaseMasterConfiguration)
      }

      //////////////////////////////////////
      // handling publish
      //////////////////////////////////////
      def publishRepositoryName =
        project.version.endsWith('-SNAPSHOT') ? extension.snapshotPublish : extension.publish

      project.uploadPublishMaster {
        def publishRepo = RepositoryPlugin.findRepository(project, publishRepositoryName)
        if(publishRepo)
          publishRepo.configure(repositories)
        else
          project.logger.warn("[${project.name}]: Could not locate configuration for publish [${publishRepositoryName}]")
      }

      // we make sure that if both release and publish are provided, release happens first!
      project.uploadPublishMaster.mustRunAfter 'release'

      // if publish is 'detached' then we make sure that the previous build actually did
      // build the artifacts!
      if(!project.gradle.startParameter.taskNames.containsAll(['release', 'publish']))
      {
        if(!publishMasterConfiguration.getExtendsFrom().contains(releaseMasterConfiguration))
        {
          project.logger.debug("${publishMasterConfiguration.name} is orphan => populating from previous build")

          // prior to releasing, we need to ensure that the artifacts were previously released!
          project.uploadPublishMaster.doFirst {
            copyArtifacts(releaseMasterConfiguration,
                          publishMasterConfiguration,
                          buildInfo.previousBuildInfo)
          }
        }
        else
        {
          project.logger.debug("${publishMasterConfiguration.name} depends on ${releaseMasterConfiguration.name}")
        }
      }

      // we add the artifacts that were just published to the build info
      project.uploadPublishMaster.doLast {
        buildInfo.addPublishedArtifacts(project, publishMasterConfiguration)
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
   * Simply copy all the artifacts that were released in the previous build into the publish
   * configuration while performing some sanity checks
   */
  protected void copyArtifacts(Configuration release,
                               Configuration publish,
                               BuildInfo previousBuildInfo)
  {
    def previouslyReleasedArtifacts =
      previousBuildInfo?.releasedArtifacts?.findAll { it.project == project.name } ?: []

    previouslyReleasedArtifacts.addAll(previousBuildInfo?.publishedArtifacts?.findAll { it.project == project.name } ?: [])

    release.allArtifacts.each { artifact ->

      File artifactFile = artifact.file.canonicalFile

      // 1. make sure the artifact actually exists!
      def previousArtifact = previouslyReleasedArtifacts.find { it.file == artifactFile.path }
      if(!previousArtifact || !artifactFile.exists())
        throw new PublishException("Cannot locate previously built artifact [${artifactFile}]", null)

      // 2. sanity checks
      ['name', 'extension', 'type', 'classifier'].each { p ->
        def previousValue = previousArtifact."${p}"
        def currentValue = artifact."${p}"
        if(previousValue != currentValue)
          throw new PublishException("Mismatch ${p} [${previousValue} != ${currentValue}] for artifact [${artifactFile}]", null)
      }

      // 3. check the checksum
      def previousChecksum = HashValue.parse(previousArtifact.sha1?.toString())
      def currentChecksum = HashUtil.sha1(artifactFile)
      if(previousChecksum != currentChecksum)
        throw new PublishException("Mismatch checksum [${previousChecksum.asHexString()} != ${currentChecksum.asHexString()}] for artifact [${artifactFile}]", null)

      def noDependencyArtifact =
        new PublishArtifactImpl(artifact: new ArtifactInfo(name: artifact.name,
                                                           extension: artifact.extension,
                                                           type: artifact.type,
                                                           classifier: artifact.classifier),
                                file: artifact.file,
                                date: previousBuildInfo.buildDate,
                                buildDependencies: new DefaultTaskDependency())
      publish.artifacts.add(noDependencyArtifact)

      project.logger.info("[${noDependencyArtifact.name}] publishing [${artifactFile}]")
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

  def snapshotRelease = 'snapshotRelease'
  def release = 'release'
  def snapshotPublish = 'snapshotPublish'
  def publish = 'publish'
}
