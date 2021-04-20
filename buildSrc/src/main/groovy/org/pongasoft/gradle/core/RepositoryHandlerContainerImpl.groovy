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

package org.pongasoft.gradle.core

import org.gradle.api.Project

/**
 * @author ypujante@linkedin.com */
class RepositoryHandlerContainerImpl implements RepositoryHandlerContainer
{
  Project project
  def repositoryHandlerFactory

  Map<String, RepositoryHandlerConfiguration> repositories = [:]

  def Map<String, RepositoryHandlerConfiguration> getAll()
  {
    return repositories
  }

  def boolean has(String name)
  {
    return repositories.containsKey(name)
  }

  def RepositoryHandlerConfiguration find(String name)
  {
    return repositories[name]
  }

  @Override
  RepositoryHandlerConfiguration getConfiguration(String name)
  {
    getOrCreate(name)
  }

  @Override
  RepositoryHandlerConfiguration addConfiguration(String name, Object configureObject)
  {
    doConfigure(name, false, configureObject)
  }

  @Override
  RepositoryHandlerConfiguration setConfiguration(String name, Object configureObject)
  {
    doConfigure(name, true, configureObject)
  }

  private RepositoryHandlerConfigurationImpl getOrCreate(String name)
  {
    if(!repositories[name])
      repositories[name] = new RepositoryHandlerConfigurationImpl(name: name,
                                                                  project: project,
                                                                  container: this)
    return repositories[name]
  }

  private RepositoryHandlerConfiguration doConfigure(String name, boolean isSetAction, Object configureObject)
  {
    RepositoryHandlerConfigurationImpl res = getOrCreate(name)

    if(configureObject == null)
      return res

    if(isSetAction)
      res.set(configureObject)
    else
      res.add(configureObject)

    return res
  }
}
