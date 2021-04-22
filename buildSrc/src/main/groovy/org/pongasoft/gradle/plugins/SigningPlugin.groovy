package org.pongasoft.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.pongasoft.gradle.utils.MissingConfigPropertyAction
import org.pongasoft.gradle.utils.Utils

/**
 * The purpose of this plugin is to apply the `signing` plugin and automatically:
 *
 * - populate the necessary properties by looking at argument on command line/env/userConfig/spec
 * - add releaseMaster to the signed artifact (which adds a signReleaseMaster task) if
 *   `org.pongasoft.release` is used
 */
class SigningPlugin implements Plugin<Project> {
  private Project project

  void apply(Project project) {
    this.project = project
    project.getPluginManager().apply(BasePlugin.class)

    // for userConfig file
    Utils.applyPluginIfExists(project, UserConfigPlugin)

    // for project-spec.groovy file
    Utils.applyPluginIfExists(project, SpecPlugin)

    // applying the signing plugin
    project.apply plugin: "signing"

    // populate the required info for signing
    project.ext["signing.keyId"] =
        Utils.getConfigProperty(project, "signing.keyId")
    project.ext["signing.password"] =
        Utils.getConfigProperty(project, "signing.password", null, MissingConfigPropertyAction.PROMPT_PASSWORD)
    project.ext["signing.secretKeyRingFile"] =
        Utils.getConfigProperty(project, "signing.secretKeyRingFile")

    // when using the Release plugin, we automatically sign the `releaseMaster` configuration
    if(project.plugins.hasPlugin(ReleasePlugin))
    {
      // we make sure it is applied first
      Utils.applyPluginIfExists(project, ReleasePlugin)

      // sign the releaseMaster configuration
      project.signing {
        sign project.configurations.releaseMaster
      }
    }
  }
}
