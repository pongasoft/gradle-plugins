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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.bundling.Compression
import org.linkedin.gradle.tasks.Tar

class CmdLinePlugin implements Plugin<Project>
{
  void apply(Project project)
  {
    def convention = new CmdLinePluginConvention(project)

    project.convention.plugins.cmdline = convention

    /********************************************************
     * task: package-assemble
     ********************************************************/
    def packageAssembleTask =
    project.task([description: "Assembles the package (exploded)"], 'package-assemble') << {
      def lib = new File(convention.assemblePackageFile, 'lib')

      project.copy {
        from project.configurations.default
        into lib
      }

      project.copy {
        from project.configurations.archives.artifacts.file
        into lib
      }

      project.copy {
        from(convention.resourcesDir) {
          if(convention.replacementTokens)
            filter(tokens: convention.replacementTokens, ReplaceTokens)
        }
        into convention.assemblePackageFile
      }

      convention.folders?.each { project.mkdir(new File(convention.assemblePackageFile, it)) }

      project.ant.chmod(dir: convention.assemblePackageFile, perm: 'ugo+rx', includes: '**/bin/*')

      logger."${convention.cmdlineLogLevel}"("Assembled package [${convention.assemblePackageFile}]")
    }

    /********************************************************
     * task: package
     ********************************************************/
    def packageTask =
    project.task([dependsOn: 'package-assemble',
                 type: Tar,
                 description: "Create the package"],
                 'package')

    packageTask << {
      logger."${convention.cmdlineLogLevel}"("Created package [${convention.packageFile}]")
    }

    /********************************************************
     * task: package-install
     ********************************************************/
    project.task([dependsOn: 'package',
                 description: "Install the package (locally)"],
                 'package-install') << {

      def installDir = convention.installDir

      if(convention.includeRoot && installDir.name == convention.packageName)
      {
        installDir = installDir.parentFile
      }

      project.ant.untar(src: convention.packageFile,
                        dest: installDir,
                        compression: convention.compression.name().toLowerCase())

      project.ant.chmod(dir: installDir, perm: 'ugo+rx', includes: '**/bin/*')

      logger."${convention.cmdlineLogLevel}"("Installed in ${convention.installDir}")
    }

    /********************************************************
     * task: package-clean-install
     ********************************************************/
    project.task([description :"Cleans the installed package"],
                 'package-clean-install') << {
      project.delete convention.installFile
      logger."${convention.cmdlineLogLevel}"("Deleted [${convention.installFile}]")
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

      project.configure(packageTask) {
        archiveSourcePath      = convention.assemblePackageFile
        archiveDestinationPath = convention.packageFile
        compression            = convention.compression
        includeRoot            = convention.includeRoot
        // setting release info for the package
        artifactReleaseInfo    = [
            name:           convention.basePackageName,
            extension:      convention.packageExtension,
            configurations: convention.artifactConfigurations
        ]
      }
    }
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
  def replacementTokens = [:]
  File resourcesDir
  def folders = ['logs']
  String cmdlineLogLevel = "info"
  Compression compression = Compression.GZIP
  String packageExtension
  def artifactConfigurations = ['package']

  private final Project _project

  def CmdLinePluginConvention(Project project)
  {
    _project = project;
    basePackageName = project.name
    packageVersion = project.version
    resourcesDir = new File(project.projectDir, 'src/cmdline/resources')
  }

  def cmdline(Closure closure)
  {
    closure.delegate = this
    closure()
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
      return compression.extension
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
