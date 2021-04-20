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

/**
 * In all methods <code>configureObject</code> can be:
 * <ul>
 * <li><code>String</code>: name of a repository within this container</li>
 * <li><code>RepositoryHandlerConfiguration</code>: another repository handler</li>
 * <li><code>Closure</code>: configuration closure (same as what RepositoryHandler takes)</li>
 * </ul>
 * @author ypujante@linkedin.com */
interface RepositoryHandlerContainer
{
  Map<String, RepositoryHandlerConfiguration> getAll()
  boolean has(String name)
  RepositoryHandlerConfiguration find(String name)

  /****************************************
   * This are convenient methods for the dsl
   ****************************************/
  RepositoryHandlerConfiguration getConfiguration(String name)
  RepositoryHandlerConfiguration addConfiguration(String name, Object configureObject)
  RepositoryHandlerConfiguration setConfiguration(String name, Object configureObject)
}
