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

package org.linkedin.gradle.tasks

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Compression

/**
 * The purpose of this class is to 'fix' the fact that the gradle tar tax does not
 * maintain executable status.
 *
 * @author ypujante@linkedin.com */
class Tar extends ReleasableArtifactTask
{
  File archiveSourcePath
  File archiveDestinationPath
  Compression compression = Compression.NONE
  boolean includeRoot = true

  @TaskAction
  protected void tar()
  {
    def root = includeRoot ? archiveSourcePath.parentFile : archiveSourcePath
    def pattern = includeRoot ? "${archiveSourcePath.name}/**" : '**'

    // TODO HIGH YP: cannot figure out how to make it work with filetrees...
    // handle only bin folders for now...
//    def allFiles = getProject().fileTree(from: root, include: pattern)
//
//    def executables = allFiles.matching {
//      include { f ->
//        f.file.isDirectory() || (f.file.isFile() && f.file.canExecute())
//      }
//    }
//
//    def nonExecutables = allFiles.matching {
//      include { f ->
//        f.file.isDirectory() || (f.file.isFile() && !f.file.canExecute())
//      }
//    }
//
//    println executables.patternSet // .findAll { it.isFile() && it.canExecute() }
//    println nonExecutables.patternSet

    ant.tar(tarFile: archiveDestinationPath, compression: compression.name().toLowerCase(), longfile: "gnu") {
      tarfileset(dir: root, includes: "${pattern}/bin/*", filemode: "755")
      fileset(dir: root, includes: pattern, excludes: "${pattern}/bin/*")
    }
  }

  File getArtifactFile()
  {
    return archiveDestinationPath
  }
}
