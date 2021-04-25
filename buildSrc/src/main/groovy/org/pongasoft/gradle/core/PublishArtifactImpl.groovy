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

package org.pongasoft.gradle.core

import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.publish.Publication
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.Task

/**
 * @author ypujante@linkedin.com */
class PublishArtifactImpl implements PublishArtifact
{
  ArtifactInfo artifact
  File file
  Date date
  TaskDependency buildDependencies

  void setTaskDependency(Task task)
  {
    buildDependencies = new DefaultTaskDependency()
    buildDependencies.add(task)
  }

  String getName()
  {
    return artifact.name
  }

  String getExtension()
  {
    return artifact.extension
  }

  String getType()
  {
    return artifact.type
  }

  String getClassifier()
  {
    return artifact.classifier
  }

  void addToPublication(Publication publication) {
    publication.artifact([
            source: file.path,
            extension: extension,
            classifier: classifier
        ])
  }
}
