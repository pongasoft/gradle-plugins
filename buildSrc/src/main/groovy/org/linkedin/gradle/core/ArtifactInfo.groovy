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

/**
 * @author ypujante@linkedin.com */
class ArtifactInfo
{
  def configurations = []
  String name
  String extension
  String type
  String classifier = ''

  ArtifactInfo() {}
  
  ArtifactInfo(ArtifactInfo other)
  {
    configurations = other.configurations
    name = other.name
    extension = other.name
    type = other.type
    classifier = other.classifier
  }

  String getType()
  {
    if(type)
      return type
    else
      return getExtension()
  }
}
