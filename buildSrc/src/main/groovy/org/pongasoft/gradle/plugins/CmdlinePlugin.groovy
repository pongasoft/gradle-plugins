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



package org.pongasoft.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.pongasoft.gradle.core.BuildInfo
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.pongasoft.gradle.tasks.SingleArtifactTask

class CmdlinePlugin implements Plugin<Project>
{
  public static final String LIB_CONFIGURATION = 'lib'

  Project project

  void apply(Project project)
  {
    project.getPlugins().apply(BasePlugin.class);

    this.project = project

    def libConfiguration = ReleasePlugin.findOrAddConfiguration(project, LIB_CONFIGURATION)

    def convention = new CmdLinePluginConvention(project)
    convention.resourcesConfigurations[libConfiguration] = 'lib'

    project.convention.plugins.cmdline = convention


    /********************************************************
     * task: package-assemble
     ********************************************************/
    def packageAssembleTask =
    project.task([description: "Assembles the package (exploded)"], 'package-assemble') {
      doLast {
        convention.resourcesConfigurations?.each { configuration, dir ->
          configuration = findConfiguration(configuration)
          if(dir instanceof String)
          {
            dir = new File(convention.assemblePackageFile, dir)
          }
          if(configuration?.resolve())
          {
            project.copy {
              from configuration.resolve()
              into dir
            }
          }

          if(configuration?.artifacts)
          {
            project.copy {
              from configuration.artifacts.file
              into dir
            }
          }
        }

        convention.resources?.each { resource ->
          def resourceInto = convention.assemblePackageFile
          def replaceTokens = true
          def replaceTokensInclude = '**/*'
          def replaceTokensExclude = null
          def resourceFrom = null

          if(resource instanceof Map)
          {
            resourceInto = resource.into ?: resourceInto
            replaceTokens = resource.replaceTokens == null ? replaceTokens : resource.replaceTokens
            replaceTokensInclude = resource.replaceTokensInclude
            replaceTokensExclude = resource.replaceTokensExclude
            resourceFrom = resource.from
          }
          else
          {
            resourceFrom = resource
          }

          if(!resourceFrom)
          {
            throw new IllegalArgumentException("missing 'from' for ${resource}")
          }

          project.copy {
            if(replaceTokens && convention.replacementTokens)
            {
              // copy and replace tokens for only the ones that need to be replaced
              from(resourceFrom) {
                if(replaceTokensInclude)
                  include replaceTokensInclude
                if(replaceTokensExclude)
                  exclude replaceTokensExclude
                filter(tokens: convention.replacementTokens, ReplaceTokens)
              }
              // copy without replacing tokens the opposite
              from(resourceFrom) {
                if(replaceTokensInclude)
                  exclude replaceTokensInclude
                if(replaceTokensExclude)
                  include replaceTokensExclude
              }
            }
            else
            {
              // copy everything... no tokens to replace
              from(resourceFrom)
            }

            // destination
            into resourceInto
          }
        }

        // adding build info to the root of the package
        if(!convention.noBuildInfo)
        {
          BuildInfo.saveToExternalFileIfExists(project,
                                               new File(convention.assemblePackageFile,
                                                        "build.info"))
        }

        convention.folders?.each { project.mkdir(new File(convention.assemblePackageFile, it)) }

        logger."${convention.cmdlineLogLevel}"("Assembled package [${convention.assemblePackageFile}]")
      }
    }

    /**
     * Code that needs to run after evaluate is done
     */
    project.afterEvaluate {
      // setting the dependency on the packageAssembleTask
      // TODO MED YP: this should work with a closure, thus avoiding afterEvaluate
      if(convention.dependsOn)
      {
        packageAssembleTask.dependsOn = convention.dependsOn
      }

      // adding all the configurations as dependencies
      convention.resourcesConfigurations?.keySet()?.each { c ->
        Configuration configuration = findConfiguration(c)
        if(configuration)
          packageAssembleTask.dependsOn(configuration)
      }

      /********************************************************
       * task: package-install
       ********************************************************/
      if(!convention.noPackageInstallTask)
      {
        project.task([dependsOn: 'package',
                       description: "Install the package (locally)"],
                     'package-install') {

          doLast {
            def installDir = convention.installDir

            if(convention.includeRoot && installDir.name == convention.packageName)
            {
              installDir = installDir.parentFile
            }

            project.copy {
              from project.tarTree(convention.packageFile)
              into installDir
            }

            logger."${convention.cmdlineLogLevel}"("Installed in ${convention.installDir}")
          }
        }

        /********************************************************
         * task: package-clean-install
         ********************************************************/
        project.task([description :"Cleans the installed package"],
                     'package-clean-install') {
          doLast {
            project.delete convention.installFile
            logger."${convention.cmdlineLogLevel}"("Deleted [${convention.installFile}]")
          }
        }
      }

      /********************************************************
       * task: package
       ********************************************************/
      def packageTask =
        project.task([dependsOn: 'package-assemble',
                       type: Tar,
                       description: "Create the package"],
                     'package') {
          def root = convention.includeRoot ?
            convention.assemblePackageFile.parentFile :
            convention.assemblePackageFile
          def pattern = convention.includeRoot ? "${convention.assemblePackageFile.name}/**" : '**'

          from root
          include pattern
          compression = convention.compression
          destinationDirectory = convention.packageFile.parentFile
          archiveFileName = convention.packageFile.name
        }

      packageTask.doLast {
        logger."${convention.cmdlineLogLevel}"("Created package [${convention.packageFile}]")
      }

      project.tasks.findByName(ReleasePlugin.RELEASE_MASTER_TASK_NAME)?.dependsOn(packageTask)

      project.task([type: SingleArtifactTask, dependsOn: packageTask],
                   "package-assemble-artifact") {
        artifactFile        = convention.packageFile
        artifactReleaseInfo = [
          name:           convention.basePackageName,
          extension:      convention.packageExtension,
          configurations: convention.artifactConfigurations
        ]
     }
    }
  }

  private Configuration findConfiguration(c)
  {
    if(c == null)
      return null

    if(c instanceof Configuration)
      return c
    else
      return project.configurations.findByName(c.toString())
  }
}

class CmdLinePluginConvention
{
  def dependsOn = []
  boolean includeRoot = true
  String basePackageName
  String packageClassifier
  String packageVersion
  String packageName
  File assemblePackageDir
  File assemblePackageFile
  File packageDir
  File packageFile
  File installDir
  File installFile
  boolean noBuildInfo = false
  boolean noPackageInstallTask = false
  def replacementTokens = [:]

  /**
   * Each entry in the list is a map with the following:
   * <li>from: anything that can be used directly in the 'from' field of the copy task</li>
   * <li>into: anything that can be used directly in the 'into'
   *          field of the copy task (optional => default to {@link #assemblePackageFile})</li>
   * <li>replaceTokens: a <code>boolean</code> to replace tokens or not (default to
   *                    <code>true</code>)</li>
   * <li>replaceTokensInclude: include only those files in the token replacements (other files
   *                           will be copied as is)</li>
   * <li>replaceTokensExclude: exclude those files from the token replacements (other files
   *                           will be token replaced)</li>
   *
   * For convenience, you can use a shortcut notation containing only the 'from' part in which case
   * it will be converted as:
   * <pre>
   * [from: from, into: <assemblePackageFile>, replaceTokens: true]
   * </pre>
   */
  def resources = ['src/cmdline/resources']
  def folders = ['logs']
  String cmdlineLogLevel = "info"
  Compression compression = Compression.GZIP
  String packageExtension
  def artifactConfigurations = ['package']
  
  /**
   * Map of configurations: key is configuration name,
   * value is folder (relative to convention.assemblePackageFile if String)
   */
  def resourcesConfigurations = [:]

  private final Project _project

  def CmdLinePluginConvention(Project project)
  {
    _project = project;
    basePackageName = project.name
    packageVersion = project.version
  }

  def cmdline(Closure closure)
  {
    closure.delegate = this
    def projectResources = closure.thisObject.resources
    try
    {
      // this issue is that closure.thisObject is of type Project and "resources" is already
      // taken => we change the meaning during execution of the closure and we restore it
      // afterwards!
      closure.thisObject.metaClass.getResources = {
        return getResources()
      }
      closure()
    }
    finally
    {
      closure.thisObject.metaClass.getResources =  { projectResources }
    }
  }

  String getPackageName()
  {
    if(packageName)
      return packageName
    else
    {
      def res = []
      res << basePackageName
      if(packageClassifier)
        res << packageClassifier
      res << packageVersion

      return res.join('-')
    }
  }

  def getDependsOn()
  {
    // set to null in the script => no depedencies
    if(dependsOn == null)
      return null

    // set in the script => use this one
    if(dependsOn)
      return dependsOn

    // unset in the script => if 'jar' task exist depend on it
    if(_project.tasks.findByName('jar'))
      return ['jar']
    else
      return null
  }

  File getAssemblePackageFile()
  {
    if(assemblePackageFile)
      return assemblePackageFile
    else
      return new File(getAssemblePackageDir(), getPackageName())
  }

  File getAssemblePackageDir()
  {
    if(assemblePackageDir)
      return assemblePackageDir
    else
      return new File(_project.buildDir, "package")
  }

  File getPackageDir()
  {
    if(packageDir)
      return packageDir
    else
      return new File(_project.buildDir, "distributions")
  }

  File getPackageFile()
  {
    if(packageFile)
      return packageFile
    else
      return new File(getPackageDir(), "${getPackageName()}.${getPackageExtension()}")
  }

  File getInstallDir()
  {
    if(installDir)
      return installDir
    else
    {
      return new File(UserConfigPlugin.getConfig(_project).top.install.dir ?: "${_project.buildDir}/install")
    }
  }

  File getInstallFile()
  {
    if(installFile)
      return installFile
    else
      return new File(getInstallDir(), getPackageName())
  }

  String getPackageExtension()
  {
    if(!packageExtension)
    {
      return compression.defaultExtension
    }
    else
    {
      return packageExtension
    }
  }

  void setCompression(String value)
  {
    compression = Compression.valueOf(value?.toUpperCase())
  }

  void setReplacementTokens(def tokens)
  {
    tokens?.each { k, v ->
      replacementTokens[k.toString()] = v.toString()
    }
  }
}
