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
import org.pongasoft.gradle.core.ReleaseType
import org.pongasoft.gradle.utils.Utils

/**
 * This plugin adds the `sonatype` extension to easily configure deployment to maven central.
 *
 * - populate the credentials by looking at argument on command line/env/userConfig/spec
 * - use snapshots vs non snapshots urls
 * - use `legacy` or `s01` repositories
 */
class SonatypePublishingPlugin implements Plugin<Project> {
  /**
   * Apply this plugin to the given target object.
   *
   * @param target The target object
   */
  @Override
  void apply(Project project) {
    project.getPlugins().apply(BasePlugin.class)
    project.getPlugins().apply("maven-publish")

    project.extensions.create("sonatype", SonatypePublishingPluginExtension, project)
  }
}

/**
 * Encapsulates url/snapshots url */
class SonatypeRepository {
  final String url
  final String snapshotsUrl

  SonatypeRepository(String baseUrl) {
    url = SonatypePublishingPluginExtension.url(baseUrl)
    snapshotsUrl = SonatypePublishingPluginExtension.snapshotsUrl(baseUrl)
  }

  String conditionedOn(ReleaseType releaseType) {
    releaseType.isSnapshot ? snapshotsUrl : url
  }
}

/**
 * Extension which gives access to the sonatype urls (legacy vs s01 and snapshots) and access to extracted
 * username and password */
class SonatypePublishingPluginExtension {
  private Project _project

  static final String LEGACY_URL = "https://oss.sonatype.org"
  static final String S01_URL = "https://s01.oss.sonatype.org"

  static final SonatypeRepository s01 = new SonatypeRepository(S01_URL)
  static final SonatypeRepository legacy = new SonatypeRepository(LEGACY_URL)

  SonatypePublishingPluginExtension(Project project) {
    _project = project
  }

  static String url(String baseUrl) { "$baseUrl/service/local/staging/deploy/maven2/".toString() }

  static String snapshotsUrl(String baseUrl) { "$baseUrl/content/repositories/snapshots/".toString() }

  String getUsername() {
    Utils.getConfigProperty(_project, "sonatype.username")
  }

  String getPassword() {
    Utils.getConfigProperty(_project, "sonatype.password")
  }
}