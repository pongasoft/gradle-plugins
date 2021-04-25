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
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.BasePlugin
import org.pongasoft.gradle.core.RepositoryHandlerConfiguration
import org.pongasoft.gradle.core.RepositoryHandlerContainerImpl
import org.pongasoft.gradle.utils.Utils

/**
 * The goal of this plugin is to load and apply external repositories configuration.
 *
 * @author ypujante@linkedin.com */
class ExternalRepositoriesPlugin implements Plugin<Project>
{
  private Project project

  void apply(Project project)
  {
    this.project = project

    project.getPlugins().apply(BasePlugin.class)
    project.getPlugins().apply(ReleaseTypePlugin.class)

    def factory = project.services.get(RepositoryHandler.class)

    def container = new RepositoryHandlerContainerImpl(project: project,
                                                       repositoryHandlerFactory: factory)

    project.extensions.create("externalRepositories", ExternalRepositoriesPluginExtension, container)

    def filesToLoad = Utils.getFilesToLoad(project, 'repositories', 'gradle')

    filesToLoad.each { loadRepositoryFile(it) }
  }

  def loadRepositoryFile(File repositoryFile)
  {
    project.apply from: repositoryFile
    project.logger.debug("Loaded ${repositoryFile}.")
  }

  static RepositoryHandlerConfiguration findRepository(Project project, def name)
  {
    if(name instanceof RepositoryHandlerConfiguration)
      return name
    else
      project.externalRepositories."${name}"
  }
}

class ExternalRepositoriesPluginExtension
{
  private final RepositoryHandlerContainerImpl _container
  private final _rootName

  ExternalRepositoriesPluginExtension(RepositoryHandlerContainerImpl container, String rootName = null)
  {
    _container = container
    _rootName = rootName
  }

  private String computeName(String name)
  {
    if(_rootName)
      return "${_rootName}.${name}".toString()
    else
      return name
  }

  // handle externalRepositories.<name> << { } => add another configuration
  // handle externalRepositories.<name>.configure()
  def propertyMissing(String name)
  {
    _container.find(computeName(name))
  }

  // handle externalRepositories.<name> = { } => value can be a closure, another repo or a string
  def propertyMissing(String name, value)
  {
    _container.setConfiguration(computeName(name), value)
  }
}