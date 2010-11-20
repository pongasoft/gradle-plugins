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

  /**
   * Configures the repository with the provided configuration
   *
   * @return <code>repository</code>
   */
  RepositoryHandler configure(RepositoryHandler repository, Object repositoryHandlerConfiguration)

  /****************************************
   * This are convenient methods for the dsl
   ****************************************/
  // used by the build script itself
  RepositoryHandlerConfiguration getBuildscript()
  RepositoryHandlerConfiguration buildscript(Object configureObject)
  RepositoryHandlerConfiguration setBuildscript(Object configureObject)

  // used during the build/compilation phase
  RepositoryHandlerConfiguration getBuild()
  RepositoryHandlerConfiguration build(Object configureObject)
  RepositoryHandlerConfiguration setBuild(Object configureObject)

  // used during the release phase (local release)
  RepositoryHandlerConfiguration getRelease()
  RepositoryHandlerConfiguration release(Object configureObject)
  RepositoryHandlerConfiguration setRelease(Object configureObject)

  // used during the release phase (local release) (for snapshots)
  RepositoryHandlerConfiguration getSnapshotRelease()
  RepositoryHandlerConfiguration snapshotRelease(Object configureObject)
  RepositoryHandlerConfiguration setSnapshotRelease(Object configureObject)

  // used during the publish phase (remote release)
  RepositoryHandlerConfiguration getPublish()
  RepositoryHandlerConfiguration publish(Object configureObject)
  RepositoryHandlerConfiguration setPublish(Object configureObject)

  // used during the publish phase (remote release) (for snapshots)
  RepositoryHandlerConfiguration getSnapshotPublish()
  RepositoryHandlerConfiguration snapshotPublish(Object configureObject)
  RepositoryHandlerConfiguration setSnapshotPublish(Object configureObject)
}
