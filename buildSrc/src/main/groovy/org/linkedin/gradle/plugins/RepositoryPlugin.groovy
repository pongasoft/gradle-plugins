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

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.linkedin.gradle.core.RepositoryHandlerContainerImpl
import org.linkedin.gradle.utils.Utils

/**
 * The goal of this plugin is to load and apply external repositories configuration.
 *
 * @author ypujante@linkedin.com */
class RepositoryPlugin implements Plugin<Project>
{
  void apply(Project project)
  {
    def factory = project.services.get(RepositoryHandler.class)

    project.allRepositories = new RepositoryHandlerContainerImpl(repositoryHandlerFactory: factory)

    def filesToLoad = Utils.getFilesToLoad(project, 'repositories', 'gradle')

    filesToLoad.each { loadRepositoryFile(project, it) }
  }

  def loadRepositoryFile(Project project, File repositoryFile)
  {
    project.apply from: repositoryFile
    project.logger.debug("Loaded ${repositoryFile}.")
  }
}
