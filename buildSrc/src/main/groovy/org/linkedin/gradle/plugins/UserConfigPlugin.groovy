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



package org.linkedin.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.linkedin.gradle.utils.Utils

/**
 * This plugin will read external configuration properties and make them available
 *
 * @author ypujante@linkedin.com */
class UserConfigPlugin implements Plugin<Project>
{
  Project project

  void apply(Project project)
  {
    this.project = project

    Properties p = new Properties()
    p['project.name'] = project.name
    def userConfig = new ConfigSlurper().parse(p)

    def filesToLoad = Utils.getFilesToLoad(project, 'userConfig', 'properties')

    filesToLoad.each {

      def config = readConfig(userConfig, it)
      if(config)
      {
        userConfig.merge(config)
      }
    }

    project.ext.set("userConfig", userConfig)
  }

  def readConfig(ConfigObject userConfig, def configFile)
  {
    configFile = new File(configFile.toString())
    if(configFile.exists())
    {
      project.logger.info "Loading user config [${configFile.toURL()}]"
      def slurper = new ConfigSlurper()
      slurper.setBinding([userConfig: userConfig])
      try
      {
        slurper.parse(configFile.toURL())
      }
      catch (Exception e)
      {
        project.logger.warn "Exception while parsing ${configFile}: ${e.message}... skipping"
        return null
      }
    }
    else
    {
      project.logger.debug "No user config found [${configFile.toURL()}]"
      return null
    }
  }

  static ConfigObject getConfig(Project project)
  {
    if(project.hasProperty('userConfig'))
      return project.userConfig
    else
      return new ConfigObject()
  }

//  static <T> T getConfigValue(Project project, T defaultValue, Closure closure)
//  {
//
//  }
}
