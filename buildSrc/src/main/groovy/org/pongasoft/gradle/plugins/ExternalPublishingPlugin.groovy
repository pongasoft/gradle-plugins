/*
 * Copyright (c) 2021 Yan Pujante
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
import org.gradle.api.plugins.BasePlugin
import org.pongasoft.gradle.utils.Utils

class ExternalPublishingPlugin implements Plugin<Project> {

  private Project project

  /**
   * Apply this plugin to the given target object.
   *
   * @param target The target object
   */
  @Override
  void apply(Project project) {
    this.project = project

    project.getPlugins().apply(BasePlugin.class)
    project.getPlugins().apply(ReleaseTypePlugin.class)
    project.getPlugins().apply("publishing")

    def filesToLoad = Utils.getFilesToLoad(project, 'publishing', 'gradle')
    filesToLoad.each { loadPublishingFile(it) }
  }

  private def loadPublishingFile(File publishingFile)
  {
    project.apply from: publishingFile
    project.logger.debug("Loaded ${publishingFile}.")
  }

}
