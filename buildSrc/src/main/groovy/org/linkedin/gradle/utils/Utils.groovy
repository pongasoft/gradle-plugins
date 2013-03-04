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

package org.linkedin.gradle.utils

import org.gradle.api.Project

/**
 * @author ypujante@linkedin.com */
class Utils
{
  static Collection<File> getFilesToLoad(Project project, String baseFilename, String extension)
  {
    // if a property is provided on the command line (-P<baseFilename>.<extension>=xxx) then this
    // value will prevail over all others
    def propertyName = "${baseFilename}.${extension}"
    if(project.hasProperty(propertyName))
    {
      return [new File(project."${propertyName}")]
    }

    def homeDir = new File(System.getProperty('user.home'))

    def names = [
      "${baseFilename}.${extension}",
      "${baseFilename}-${project.group}.${extension}",
      "${baseFilename}-${project.name}.${extension}",
      "${baseFilename}-${project.group}-${project.name}.${extension}"
    ]

    def filesToTry = []

    // 1st local files
    names.each {
      filesToTry << project.file(it)
    }

    // 2nd $HOME/.org.linkedin/*
    names.each {
      filesToTry << new File(homeDir, ".org.linkedin/${it}")
    }

    // 3rd $HOME/.gradle/*
    names.each {
      filesToTry << new File(homeDir, ".gradle/${it}")
    }

    // 4th $HOME/.*
    names.each {
      filesToTry << new File(homeDir, ".${it}")
    }

    return filesToTry.findAll { it.exists() }
  }

  /**
   * Note that this class is copied from linkedin-utils.
   * We don't really want to have this kind of dependency in this base project.
   * 
   * Flattens the map. Ex: [a: 1, b: [1,2], c: [d: 1]] returns a map:
   * <code>[a: 1, 'b[0]': 1, 'b[1]': 2, 'c.d': 1]</code>
   * @return a new map
   */
  static Map flatten(Map map)
  {
    if(map == null)
      return null

    Map flattenedMap = [:]

    doFlatten(map, flattenedMap, '')

    return flattenedMap
  }

  /**
   * Same as {@link #flatten(Map)} but use <code>destMap</code> for the result
   * @return <code>destMap</code>
   */
  static Map flatten(Map srcMap, Map destMap)
  {
    if(srcMap == null)
      return

    doFlatten(srcMap, destMap, '')

    return destMap
  }

  private static void doFlatten(Map map, def flattenedMap, String prefix)
  {
    map?.each { k, v ->

      def key = prefix ? "${prefix}.${k}".toString() : k

      switch(v)
      {
        case { v instanceof Map}:
          doFlatten(v, flattenedMap, key)
          break

        case { v instanceof Collection}:
          doFlatten(v, flattenedMap, key)
          break

        default:
          flattenedMap[key] = v

      }
    }
  }

  private static void doFlatten(Collection c, def flattenedMap, String prefix)
  {
    c?.eachWithIndex { e, idx ->
      def key = "${prefix}[${idx}]".toString()

      switch(e)
      {
        case { e instanceof Map}:
          doFlatten(e, flattenedMap, key)
          break

        case { e instanceof Collection}:
          doFlatten(e, flattenedMap, key)
          break

        default:
          flattenedMap[key] = e
      }
    }
  }
}
