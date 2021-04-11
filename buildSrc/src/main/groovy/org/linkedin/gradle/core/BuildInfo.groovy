/*
 * Copyright (c) 2013-2021 Yan Pujante
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
import org.gradle.initialization.BuildRequestMetaData
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import org.gradle.internal.hash.HashUtil
import org.linkedin.gradle.utils.JsonUtils
import org.linkedin.gradle.utils.Utils

import java.text.SimpleDateFormat

/**
 * @author yan@pongasoft.com  */
public class BuildInfo
{
  BuildInfo previousBuildInfo

  String name
  String version
  String scmUrl
  String scmCommitVersion
  String gradleVersion
  String jvm
  String os
  long buildTime
  String buildTimeString
  long buildDuration = 0L
  String buildDurationString
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
    scmUrl = computeScmUrl(rootProject)
    scmCommitVersion = computeScmCommitVersion(rootProject)
    gradleVersion = rootProject.gradle.gradleVersion
    jvm = Jvm.current().toString()
    os = OperatingSystem.current().toString()

    buildTime = rootProject.gradle.services.get(BuildRequestMetaData).startTime
    buildTimeString = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z").format(new Date(buildTime))

    buildTasks = rootProject.gradle.startParameter.taskNames
    buildProperties = rootProject.gradle.startParameter.projectProperties

    rootProject.gradle.buildFinished { BuildResult result ->
      buildDuration = System.currentTimeMillis() - buildTime
      buildDurationString = Utils.prettyTime(buildDuration)
      rootProject.logger.info(toInternalJson())
      if(!result.failure)
      {
        def file = computeBuildInfoFile(rootProject)
        file.parentFile.mkdirs()
        file.text = toInternalJson()
      }
    }
  }

  Date getBuildDate()
  {
    new Date(buildTime)
  }

  void addReleasedArtifacts(Project project, Configuration configuration)
  {
    addArtifacts(project, configuration, releasedArtifacts)
  }

  void addPublishedArtifacts(Project project, Configuration configuration)
  {
    addArtifacts(project, configuration, publishedArtifacts)
  }

  private static void addArtifacts(Project project, Configuration configuration, def artifacts)
  {
    configuration.allArtifacts.all { PublishArtifact artifact ->
      artifacts << createArtifactMap(project, artifact)
    }
  }

  private static Map createArtifactMap(Project project, PublishArtifact artifact)
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

  public Map toExternalMap()
  {
    def map = [:]

    map.name = name
    map.version = version

    if(scmUrl)
      map.scmUrl = scmUrl

    if(scmCommitVersion)
      map.scmCommitVersion = scmCommitVersion

    map.gradleVersion = gradleVersion
    map.jvm = jvm
    map.os = os

    map.buildTime = buildTime
    map.buildTimeString = buildTimeString

    return map
  }

  public String toExternalJson()
  {
    JsonUtils.prettyPrint(toExternalMap())
  }

  private Map toInternalMap()
  {
    def map = [:]

    map.name = name
    map.version = version

    if(scmUrl)
      map.scmUrl = scmUrl

    if(scmCommitVersion)
      map.scmCommitVersion = scmCommitVersion

    map.gradleVersion = gradleVersion
    map.jvm = jvm
    map.os = os

    map.buildTime = buildTime
    map.buildTimeString = buildTimeString
    map.buildDuration = buildDuration
    map.buildDurationString = buildDurationString

    if(buildProperties)
      map.buildProperties = buildProperties

    if(buildTasks)
      map.buildTasks = buildTasks

    if(releasedArtifacts)
      map.releasedArtifacts = releasedArtifacts

    if(publishedArtifacts)
      map.publishedArtifacts = publishedArtifacts

    return map
  }

  private String toInternalJson()
  {
    JsonUtils.prettyPrint(toInternalMap())
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

    return null
  }

  private static String computeScmUrl(Project rootProject)
  {
    if(rootProject.hasProperty('spec'))
      return rootProject.spec.scmUrl
    else
      return null
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

  /**
   * @return <code>true</code> if was saved
   */
  public static boolean saveToExternalFileIfExists(Project project, File fileToSave)
  {
    project = project.rootProject

    if(!project.hasProperty('buildInfo'))
    {
      return false
    }

    fileToSave.text = findOrCreate(project).toExternalJson()

    return true
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