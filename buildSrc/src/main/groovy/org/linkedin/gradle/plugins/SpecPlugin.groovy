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



package org.linkedin.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.util.DeleteOnExit
import org.linkedin.gradle.utils.JsonUtils
import org.linkedin.gradle.utils.Utils
import org.json.JSONException

/**
 * The goal of this plugin is to read/parse the product spec and make it available during the build
 * 
 * @author ypujante@linkedin.com */
class SpecPlugin implements Plugin<Project>
{
  Project project

  void apply(Project project)
  {
    this.project = project

    parseProjectSpec()
    configureProject()
  }

  /**
   * Configure the project based on the spec.
   */
  def configureProject()
  {
    if(project.hasProperty('snapshot') && project.snapshot.toString() == 'true')
    {
      if(!project.spec.version.endsWith('-SNAPSHOT'))
        project.spec.version = "${project.spec.version}-SNAPSHOT".toString()
    }
    
    if(project.hasProperty('release') && project.release.toString() == 'true')
    {
      if(project.spec.version.endsWith('-SNAPSHOT'))
        throw new IllegalStateException("${project.spec.version} is a snapshot version")
    }

    def mode = project.spec.version.endsWith('-SNAPSHOT') ? 'snapshot': 'release'
    project.logger.lifecycle "Working in ${mode} mode: ${project.spec.version}"
  }

  /**
   * Parses the project spec and replace the properties (@xx@ syntax).
   * @return the expanded spec
   */
  private def expandJSONProperties(File jsonProjectSpec, def props)
  {
    def tokens = [:]

    props.each { name, value ->
      tokens["spec.${name}".toString()] = value
    }

    def tmpFolder = File.createTempFile("project-spec", "")
    tmpFolder.delete()
    tmpFolder.mkdirs()
    DeleteOnExit.addFile(tmpFolder)

    project.copy {
      from(jsonProjectSpec) {
        filter(ReplaceTokens, tokens: tokens)
      }
      into tmpFolder
    }

    return parseProjectSpec(new File(tmpFolder, 'project-spec.json'), false)
  }

  private void parseProjectSpec()
  {
    File groovyProjectSpec = new File(project.rootDir, 'project-spec.groovy')
    if(groovyProjectSpec.exists())
    {
      project.apply from: groovyProjectSpec
    }
    else
    {
      project.spec = parseProjectSpec(new File(project.rootDir, 'project-spec.json'), true)
    }
  }

  protected def parseProjectSpec(File projectSpectFile, boolean expandProperties)
  {
    if(projectSpectFile.exists())
    {
      try
      {
        def projectSpec = JsonUtils.fromJSON(projectSpectFile.text)
        if(expandProperties)
          projectSpec = expandJSONProperties(projectSpectFile, Utils.flatten(projectSpec))
        return projectSpec
      }
      catch (JSONException e)
      {
        def explanation = JsonUtils.explainJsonParsingError(e.message, projectSpectFile)
        if(explanation)
          throw new RuntimeException("${e.message}:\n${explanation}", e)
        else
          throw e
      }
    }
    else
    {
      return [:]
    }
  }
}
