/*
 * Copyright (c) 2013 Yan Pujante
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

import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.hash.HashUtil
import org.linkedin.gradle.utils.JsonUtils

import java.text.SimpleDateFormat

/**
 * @author yan@pongasoft.com  */
public class BuildInfo
{
  BuildInfo previousBuildInfo

  String name
  String version
  String scmCommitVersion
  String gradleVersion
  String jvm
  String os
  long buildTimestamp = System.currentTimeMillis()
  String buildTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z").format(new Date(buildTimestamp))
  long buildDuration = 0L
  def buildTasks
  def buildProperties
  def releasedArtifacts = Collections.synchronizedCollection([])
  def publishedArtifacts = Collections.synchronizedCollection([])

  private BuildInfo()
  {
    // for previous build...
  }

  private BuildInfo(Project rootProject)
  {
    previousBuildInfo = fromFile(rootProject)

    name = rootProject.name
    version = rootProject.version
    scmCommitVersion = computeScmCommitVersion(rootProject)
    gradleVersion = rootProject.gradle.gradleVersion
    jvm = Jvm.current().toString()
    os = OperatingSystem.current().toString()

    buildTasks = rootProject.gradle.startParameter.taskNames
    buildProperties = rootProject.gradle.startParameter.projectProperties

    rootProject.gradle.buildFinished { BuildResult result ->
      buildDuration = System.currentTimeMillis() - buildTimestamp
      rootProject.logger.info(toJson())
      if(!result.failure)
      {
        computeBuildInfoFile(rootProject).text = toJson()
      }
    }
  }

  Date getBuildDate()
  {
    new Date(buildTimestamp)
  }

  void addReleasedArtifacts(Project project, Configuration configuration)
  {
    releasedArtifacts.addAll(configuration.allArtifacts.collect { toArtifactMap(project, it) })
  }

  void addPublishedArtifacts(Project project, Configuration configuration)
  {
    publishedArtifacts.addAll(configuration.allArtifacts.collect { toArtifactMap(project, it) })
  }

  private Map toArtifactMap(Project project, PublishArtifact artifact)
  {
    [
      project: project.name,
      name: artifact.name,
      extension: artifact.extension,
      type: artifact.type,
      classifier: artifact.classifier,
      file: artifact.file.canonicalPath,
      sha1: HashUtil.sha1(artifact.file).asHexString()
    ]
  }

  private String toJson()
  {
    def json =[:]

    json.name = name
    json.version = version

    if(scmCommitVersion)
      json.scmCommitVersion = scmCommitVersion

    json.gradleVersion = gradleVersion
    json.jvm = jvm
    json.os = os

    json.buildTimestamp = buildTimestamp
    json.buildTime = buildTime
    json.buildDuration = buildDuration

    if(buildProperties)
      json.buildProperties = buildProperties

    if(buildTasks)
      json.buildTasks = buildTasks

    if(releasedArtifacts)
      json.releasedArtifacts = releasedArtifacts

    if(publishedArtifacts)
      json.publishedArtifacts = publishedArtifacts

    JsonUtils.prettyPrint(json)
  }

  private static String computeScmCommitVersion(Project rootProject)
  {
    if(rootProject.hasProperty('spec'))
    {
      switch(rootProject.spec.scm)
      {
        case 'git':
          return 'git rev-parse HEAD'.execute().text.trim()
          break
      }
    }

    return '<unknown>'
  }

  private static File computeBuildInfoFile(Project rootProject)
  {
    if(rootProject.hasProperty('topBuildDir'))
      new File(rootProject.topBuildDir, "build.info")
    else
      new File(rootProject.projectDir, "build/build.info")
  }

  public static BuildInfo findOrCreate(Project project)
  {
    project = project.rootProject
    if(!project.hasProperty('buildInfo'))
    {
      project.ext.buildInfo = new BuildInfo(project)
    }

    return project.buildInfo
  }

  private static BuildInfo fromJson(Project rootProject, String json)
  {
    try
    {
      return new BuildInfo(JsonUtils.fromJSON(json))
    }
    catch(Throwable th)
    {
      rootProject.logger.warn("Could not load previous build info", th)
      return null
    }
  }

  private static BuildInfo fromFile(Project rootProject)
  {
    def previousBuildInfo = computeBuildInfoFile(rootProject)
    if(previousBuildInfo.exists())
      return fromJson(rootProject,previousBuildInfo.text)
    else
      return null
  }

}