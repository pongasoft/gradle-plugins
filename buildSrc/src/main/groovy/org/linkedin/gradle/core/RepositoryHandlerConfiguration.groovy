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
interface RepositoryHandlerConfiguration
{
  /**
   * Configures the repository
   * @return <code>repository</code>
   */
  RepositoryHandler configure(RepositoryHandler repository)


  /**
   * Adds a configuration to this configuration.
   * @return <code>this</code> 
   */
  RepositoryHandlerConfiguration add(Object configuration)

  /**
   * Adds a configuration to this configuration.
   * @return <code>this</code>
   */
  RepositoryHandlerConfiguration leftShift(Object configuration)

  /**
   * Sets this configuration to the provided one
   * @return <code>this</code>
   */
  RepositoryHandlerConfiguration set(Object configuration)
}
