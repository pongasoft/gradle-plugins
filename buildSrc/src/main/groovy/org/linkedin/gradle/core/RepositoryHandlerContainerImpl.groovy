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

package org.linkedin.gradle.core

import org.gradle.api.artifacts.dsl.RepositoryHandler

/**
 * @author ypujante@linkedin.com */
class RepositoryHandlerContainerImpl implements RepositoryHandlerContainer
{
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

  /************* build **************/
  def RepositoryHandlerConfiguration getBuild()
  {
    getOrCreate('build')
  }

  def RepositoryHandlerConfiguration build(Object configureObject)
  {
    doConfigure('build', false, configureObject)
  }

  def RepositoryHandlerConfiguration setBuild(Object configureObject)
  {
    doConfigure('build', true, configureObject)
  }

  /************* buildScript **************/
  def RepositoryHandlerConfiguration getBuildscript()
  {
    getOrCreate('buildscript')
  }

  def RepositoryHandlerConfiguration buildscript(Object configureObject)
  {
    doConfigure('buildscript', false, configureObject)
  }

  def RepositoryHandlerConfiguration setBuildscript(Object configureObject)
  {
    doConfigure('buildscript', true, configureObject)
  }

  /************* release **************/
  def RepositoryHandlerConfiguration getRelease()
  {
    getOrCreate('release')
  }

  def RepositoryHandlerConfiguration release(Object configureObject)
  {
    doConfigure('release', false, configureObject)
  }

  def RepositoryHandlerConfiguration setRelease(Object configureObject)
  {
    doConfigure('release', true, configureObject)
  }

  /************* snapshotRelease **************/
  def RepositoryHandlerConfiguration getSnapshotRelease()
  {
    getOrCreate('snapshotRelease')
  }

  def RepositoryHandlerConfiguration snapshotRelease(Object configureObject)
  {
    doConfigure('snapshotRelease', false, configureObject)
  }

  def RepositoryHandlerConfiguration setSnapshotRelease(Object configureObject)
  {
    doConfigure('snapshotRelease', true, configureObject)
  }

  /************* publish **************/
  def RepositoryHandlerConfiguration getPublish()
  {
    getOrCreate('publish')
  }

  def RepositoryHandlerConfiguration publish(Object configureObject)
  {
    doConfigure('publish', false, configureObject)
  }

  def RepositoryHandlerConfiguration setPublish(Object configureObject)
  {
    doConfigure('publish', true, configureObject)
  }

  /************* snapshotPublish **************/
  def RepositoryHandlerConfiguration getSnapshotPublish()
  {
    getOrCreate('snapshotPublish')
  }

  def RepositoryHandlerConfiguration snapshotPublish(Object configureObject)
  {
    doConfigure('snapshotPublish', false, configureObject)
  }

  def RepositoryHandlerConfiguration setSnapshotPublish(Object configureObject)
  {
    doConfigure('snapshotPublish', true, configureObject)
  }

  def RepositoryHandler configure(RepositoryHandler repository,
                                  Object repositoryHandlerConfiguration)
  {
    RepositoryHandlerConfiguration config = null
    switch(repositoryHandlerConfiguration)
    {
      case { it instanceof String }:
        config = getOrCreate(repositoryHandlerConfiguration as String)
        break;

      case { it instanceof RepositoryHandlerConfiguration }:
        config = repositoryHandlerConfiguration
        break;

      default:
        throw new IllegalArgumentException("invalid type ${repository.class.name}")
    }
    return config.configure(repository)
  }

  private RepositoryHandlerConfigurationImpl getOrCreate(String name)
  {
    if(!repositories[name])
      repositories[name] = new RepositoryHandlerConfigurationImpl(container: this)
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
