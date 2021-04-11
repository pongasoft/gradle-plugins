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

package org.linkedin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.linkedin.gradle.core.PublishArtifactImpl
import org.linkedin.gradle.plugins.ReleasePlugin
import org.linkedin.gradle.core.ArtifactInfo

/**
 * Represents a task that produces an artifact that can be 'released'
 *
 * @author ypujante@linkedin.com */
abstract class ReleasableArtifactTask extends DefaultTask
{
  /**
   * Called to set information about how to 'release' this artifact
   */
  public def setArtifactReleaseInfo(artifactReleaseInfo)
  {
    def res = []

    if(artifactReleaseInfo)
    {
      ArtifactInfo artifactInfo = new ArtifactInfo(artifactReleaseInfo)

      artifactInfo.configurations.each { configuration ->
        def conf = ReleasePlugin.addToReleaseMaster(project, configuration)
        if(conf)
        {
          def artifact = new PublishArtifactImpl(artifact: artifactInfo,
                                                 file: artifactFile,
                                                 taskDependency: this)
          conf.artifacts.add(artifact)

          res << artifact
        }
      }
    }
    return res
  }

  @OutputFile
  abstract File getArtifactFile()
}
