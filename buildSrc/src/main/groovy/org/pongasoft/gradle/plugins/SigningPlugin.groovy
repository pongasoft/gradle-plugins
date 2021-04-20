package org.pongasoft.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.pongasoft.gradle.utils.MissingConfigPropertyAction
import org.pongasoft.gradle.utils.Utils

/**
 * The purpose of this plugin is to apply the `signing` plugin and automatically:
 *
 * - populate the necessary properties by looking at argument on command line/env/userConfig/spec
 * - add releaseMaster to the signed artifact (which adds a signReleaseMaster task)
 */
class SigningPlugin implements Plugin<Project> {
  private Project project

  void apply(Project project) {
    this.project = project

    // we depend on release plugin
    if(!project.plugins.hasPlugin('org.pongasoft.release'))
      project.apply plugin: ReleasePlugin

    // applying the signing plugin
    project.apply plugin: "signing"

    project.ext["signing.keyId"] =
        Utils.getConfigProperty(project, "signing.keyId")
    project.ext["signing.password"] =
        Utils.getConfigProperty(project, "signing.password", null, MissingConfigPropertyAction.PROMPT_PASSWORD)
    project.ext["signing.secretKeyRingFile"] =
        Utils.getConfigProperty(project, "signing.secretKeyRingFile")

    project.signing {
      sign project.configurations.releaseMaster
    }
  }
}

