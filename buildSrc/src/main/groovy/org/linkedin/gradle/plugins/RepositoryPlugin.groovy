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

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import org.gradle.api.Project
import org.gradle.api.Plugin

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.linkedin.gradle.core.RepositoryHandlerConfiguration
import org.linkedin.gradle.core.RepositoryHandlerContainerImpl
import org.linkedin.gradle.utils.MissingConfigPropertyAction
import org.linkedin.gradle.utils.Utils

/**
 * The goal of this plugin is to load and apply external repositories configuration.
 *
 * @author ypujante@linkedin.com */
class RepositoryPlugin implements Plugin<Project>
{
  static def BINTRAY_API_KEY = null

  void apply(Project project)
  {
    def factory = project.services.get(RepositoryHandler.class)

    def container = new RepositoryHandlerContainerImpl(project: project,
                                                       repositoryHandlerFactory: factory)

    project.ext {
      allRepositories = new RepositoryPluginExtension(container)
    }

    addBintrayExtension(project)

    def filesToLoad = Utils.getFilesToLoad(project, 'repositories', 'gradle')

    filesToLoad.each { loadRepositoryFile(project, it) }
  }

  def loadRepositoryFile(Project project, File repositoryFile)
  {
    project.apply from: repositoryFile
    project.logger.debug("Loaded ${repositoryFile}.")
  }

  /**
   * Add bintray extension based on whether bintray is used at all
   */
  protected void addBintrayExtension(Project project)
  {
    def repositories = [] as Set

    // collect the names of the repositories (used in repositories.gradle to publish)
    // this is usually defined outside the project and allow for overrides
    if(project.hasProperty('userConfig'))
    {
      project.userConfig.bintray?.repositories?.each { k,v -> repositories << k}
    }
    // this is usually define in the project itself
    if(project.hasProperty('spec'))
    {
      project.spec.bintray?.repositories?.each { k,v -> repositories << k}
    }

    // set defaults for all properties
    def rootProperties = [:]

    rootProperties.apiBaseUrl =
      Utils.getOptionalConfigProperty(project, 'bintray.apiBaseUrl', "https://bintray.com/api/v1")
    rootProperties.username =
      Utils.getOptionalConfigProperty(project, 'bintray.username', System.getProperty("user.name"))
    rootProperties.apiKey =
      Utils.getOptionalConfigProperty(project, 'bintray.apiKey', null)
    rootProperties.pkgOrganization =
      Utils.getOptionalConfigProperty(project, 'bintray.pkgOrganization', rootProperties.username)
    rootProperties.pkgRepository =
      Utils.getOptionalConfigProperty(project, 'bintray.pkgRepository', project.group)
    rootProperties.pkgName =
      Utils.getOptionalConfigProperty(project, 'bintray.pkgName', project.rootProject.name)
    rootProperties.pkgVersion = project.version

    // add 'bintray' extension
    def bintrayExtension =
      project.extensions.create("bintray", BintrayRepositoryExtension, 'root', rootProperties)

    def apiKey = rootProperties.apiKey

    def repositoryExtensions = []

    // for each repository add an extension to 'bintray' with the name of the repository
    repositories.each { name ->
      def repositoryProperties = rootProperties.keySet().collectEntries { k ->
        def propertyName = "bintray.repositories.${name}.${k}"
        [k, Utils.getOptionalConfigProperty(project,
                                            propertyName,
                                            rootProperties[k])]
      }

      if(!repositoryProperties.apiKey)
      {
        apiKey =  promptForApiKey(project)
        rootProperties.apiKey = apiKey
        repositoryProperties.apiKey = apiKey
      }

      repositoryExtensions << bintrayExtension.extensions.create(name,
                                                                 BintrayRepositoryExtension,
                                                                 name,
                                                                 repositoryProperties)
    }

    // add a task to create the (missing) packages
    if(repositoryExtensions)
    {
      project.task([description: "Ensures that bintray packages are created"], 'createBintrayPackages') << {
        repositoryExtensions.each { ext -> createBintrayPackages(project, ext) }
      }
    }
  }

  static def promptForApiKey(Project project)
  {
    if(BINTRAY_API_KEY == null)
    {
      BINTRAY_API_KEY = Utils.getConfigProperty(project,
                                                "bintray.apiKey",
                                                MissingConfigPropertyAction.PROMPT_PASSWORD)
    }

    return BINTRAY_API_KEY
  }

  /**
   * Use bintray rest api to create the proper packages (if they don't exist)
   */
  protected void createBintrayPackages(Project project, BintrayRepositoryExtension extension)
  {
    def path = "packages/${extension.pkgOrganization}/${extension.pkgRepository}/${extension.pkgName}".toString()
    def http = createHttpBuilder(extension)
    http.request(GET, JSON) {
      uri.path = "${uri.path}/${path}"
      response.'404' = {
        project.logger.info("Missing bintray package [/${path}]: trying to create...")
        http = createHttpBuilder(extension)
        http.request(POST, JSON) {
          uri.path = "${uri.path}/packages/${extension.pkgOrganization}/${extension.pkgRepository}".toString()
          body = [name: extension.pkgName, desc: 'auto', desc_url: 'auto']

          response.success = { resp ->
            project.logger.info("Successfully created package: [/${path}]")
          }
        }
      }
    }
  }

  /**
   * Creates
   */
  protected HTTPBuilder createHttpBuilder(BintrayRepositoryExtension extension)
  {
    def http = new HTTPBuilder(extension.apiBaseUrl)
    http.auth.basic extension.username.toString() , extension.apiKey.toString()
    return http
  }


  static RepositoryHandlerConfiguration findRepository(Project project, String name)
  {
    project.allRepositories."${name}"
  }
}

class BintrayRepositoryExtension
{
  String name

  def apiBaseUrl
  def username
  def apiKey
  def pkgOrganization
  def pkgRepository
  def pkgName
  def pkgVersion

  BintrayRepositoryExtension(String name, Map config)
  {
    this.name = name

    apiBaseUrl = config.apiBaseUrl
    username = config.username
    apiKey = config.apiKey
    pkgOrganization = config.pkgOrganization
    pkgRepository = config.pkgRepository
    pkgName = config.pkgName
    pkgVersion = config.pkgVersion

  }
}

class RepositoryPluginExtension
{
  private final RepositoryHandlerContainerImpl _container

  RepositoryPluginExtension(RepositoryHandlerContainerImpl container)
  {
    this._container = container
  }

  // handle allRepositories.<name> << { } => add another configuration
  // handle allRepositories.<name>.configure()
  def propertyMissing(String name)
  {
    _container.find(name)
  }

  // handle allRepositories.<name> = { } => value can be a closure, another repo or a string
  def propertyMissing(String name, value)
  {
    _container.setConfiguration(name, value)
  }
}