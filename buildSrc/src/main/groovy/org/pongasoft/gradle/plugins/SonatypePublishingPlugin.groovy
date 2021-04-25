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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.BasePlugin
import org.pongasoft.gradle.core.ReleaseType
import org.pongasoft.gradle.utils.Utils

/**
 * This plugin adds the `sonatype` extension to easily configure deployment to maven central.
 *
 * - populate the credentials by looking at argument on command line/env/userConfig/spec
 * - use snapshot vs non snapshot urls automatically
 * - you can use `legacy` or `s01` configurator depending on situation
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
 * Configure the maven repository for sonatype */
class SonatypeMavenConfigurator implements Action<MavenArtifactRepository> {
  private Project _project
  private String _name
  private def _url

  SonatypeMavenConfigurator(Project project, String name, def url) {
    _project = project
    _name = name
    _url = url
  }

  void execute(MavenArtifactRepository repository) {
    repository.name = _name
    repository.url = _url
    repository.credentials {
      username = Utils.getConfigProperty(_project, "sonatype.username")
      password = Utils.getConfigProperty(_project, "sonatype.password")
    }
  }
}

/**
 * Simple extension which offers 2 default configurators `legacy` and `s01` and a `generic` one to provide your own
 * url */
class SonatypePublishingPluginExtension {
  private Project _project

  SonatypePublishingPluginExtension(Project project) {
    _project = project
  }

  SonatypeMavenConfigurator legacy(String name) {
    return new SonatypeMavenConfigurator(_project, name, computeUrlFromReleaseType("https://oss.sonatype.org"))
  }

  SonatypeMavenConfigurator s01(String name) {
    return new SonatypeMavenConfigurator(_project, name, computeUrlFromReleaseType("https://s01.oss.sonatype.org"))
  }

  SonatypeMavenConfigurator generic(String name, def url) {
    return new SonatypeMavenConfigurator(_project, name, url)
  }

  def computeUrlFromReleaseType(def baseUrl) {
    return ReleaseType.from(_project).isSnapshot ?
        "$baseUrl/content/repositories/snapshots/" :
        "$baseUrl/service/local/staging/deploy/maven2/"
  }
}