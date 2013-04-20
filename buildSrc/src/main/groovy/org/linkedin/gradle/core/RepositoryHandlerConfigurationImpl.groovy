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

package org.linkedin.gradle.core

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.util.ConfigureUtil

/**
 * @author ypujante@linkedin.com */
class RepositoryHandlerConfigurationImpl implements RepositoryHandlerConfiguration
{
  String name
  Project project
  RepositoryHandlerContainer container

  def configurations = []

  @Override
  RepositoryHandler configure()
  {
    configure(project.repositories)
  }

  @Override
  RepositoryHandler configure(RepositoryHandler repository)
  {
    configurations?.each { c ->
      switch(c)
      {
        case { it instanceof Closure }:
          ConfigureUtil.configure(c as Closure, repository)
          break;

        case { it instanceof String }:
          container.find(c as String)?.configure(repository)
          break;

        case { it instanceof RepositoryHandlerConfiguration }:
          c.configure(repository)
          break;

        default:
          throw new IllegalArgumentException("invalid type ${c.class.name}")
      }
    }

    return repository;
  }

  private boolean checkConfig(Object configuration)
  {
    if(!(configuration instanceof Closure ||
         configuration instanceof String ||
         configuration instanceof RepositoryHandlerConfiguration))
      throw new IllegalArgumentException("invalid type ${configuration.class.name}")

    return !this.is(configuration)

  }

  RepositoryHandlerConfiguration add(Object configuration)
  {
    if(checkConfig(configuration))
      configurations << configuration
    return this
  }

  RepositoryHandlerConfiguration leftShift(Object configuration)
  {
    return add(configuration)
  }

  RepositoryHandlerConfiguration set(Object configuration)
  {
    if(checkConfig(configuration))
      configurations = [configuration]
    return this
  }
}
