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

package org.linkedin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.resources.MissingResourceException

/**
 * @author ypujante@linkedin.com */
class Utils
{
  static Collection<File> getFilesToLoad(Project project, String baseFilename, String extension)
  {
    // if a property is provided on the command line (-P<baseFilename>.<extension>=xxx) then this
    // value will prevail over all others
    def propertyName = "${baseFilename}.${extension}"
    if(project.hasProperty(propertyName))
    {
      return [new File(project."${propertyName}")]
    }

    def homeDir = new File(System.getProperty('user.home'))

    def names = [
      "${baseFilename}.${extension}"
      ]

    names.addAll(computePropertyNamesFromLessToMoreSpecific(project).collect {
      "${baseFilename}-${it}.${extension}" }
    )

    def filesToTry = new LinkedHashSet()

    // 1. (root) local files
    if(project.rootProject != project)
    {
      names.each {
        filesToTry << project.rootProject.file(it)
      }
    }

    // 2. local files
    names.each {
      filesToTry << project.file(it)
    }

    // 3. $HOME/.<group>/*
    names.each {
      filesToTry << new File(homeDir, ".${project.group}/${it}")
    }

    // 4. $HOME/.gradle/*
    names.each {
      filesToTry << new File(homeDir, ".gradle/${it}")
    }

    // 5. $HOME/.*
    names.each {
      filesToTry << new File(homeDir, ".${it}")
    }

    return filesToTry.findAll { it.exists() }
  }

  static def computePropertyNamesFromLessToMoreSpecific(Project project)
  {
    if(project != project.rootProject)
    {
      [
        "${project.group}",
        "${project.rootProject.name}",
        "${project.name}",
        "${project.group}-${project.name}",
        "${project.rootProject.name}-${project.group}",
        "${project.rootProject.name}-${project.name}",
        "${project.rootProject.name}-${project.group}-${project.name}"
      ]
    }
    else
    {
      [
        "${project.group}",
        "${project.name}",
        "${project.group}-${project.name}"
      ]
    }
  }

  /**
   * @return a (optional) boolean configuration property
   */
  static boolean getConfigBooleanProjectProperty(Project project,
                                                 String propertyName,
                                                 boolean defaultValue)
  {
    if(project.hasProperty(propertyName))
      return project."${propertyName}".toString() == 'true'
    else
      return defaultValue
  }

  /**
   * Try to locate the config property using a dotted name (ex: top.build.dir). First try
   * the project, then the userConfig (if present), then the spec object (if present). When the
   * property is not found, the action specifies how to handle it.
   *
   * @param allVariants represent all possible different names for the property
   * @return the property (can be anything specified)
   */
  static def getOptionalConfigProperty(Project project,
                                       String dottedConfigPropertyName,
                                       Collection<String> allVariants = null,
                                       def defaultConfigProperty = null)
  {
    getConfigProperty(project,
                      dottedConfigPropertyName,
                      allVariants,
                      MissingConfigPropertyAction.NULL) ?: defaultConfigProperty
  }

  /**
   * Try to locate the config property using a dotted name (ex: top.build.dir). First try
   * the project, then the userConfig (if present), then the spec object (if present). When the
   * property is not found, the action specifies how to handle it.
   *
   * @param allVariants represent all possible different names for the property
   * @return the property (can be anything specified)
   */
  static def getConfigProperty(Project project,
                               String dottedConfigPropertyName,
                               Collection<String> allVariants = null,
                               MissingConfigPropertyAction action = MissingConfigPropertyAction.THROW)
  {
    if(!dottedConfigPropertyName)
      throw new IllegalArgumentException("missing property name")

    if(!allVariants)
      allVariants = [dottedConfigPropertyName]

    def configProperty = allVariants.findResult { p ->
      doGetConfigProperty(project, p)
    }

    if(configProperty != null)
      return configProperty

    // 4. no property found... handle missing property
    switch(action)
    {
      case MissingConfigPropertyAction.NULL:
        return null

      case MissingConfigPropertyAction.THROW:
        throwMissingConfigPropertyException(dottedConfigPropertyName)
        break // not reached

      case MissingConfigPropertyAction.PROMPT:
        def cp = System.console()?.readLine("\n====> Please enter ${dottedConfigPropertyName}: ")
        if(cp == null) // no console...
          throwMissingConfigPropertyException(dottedConfigPropertyName)
        return cp

      case MissingConfigPropertyAction.PROMPT_PASSWORD:
        def cp = System.console()?.readPassword("\n====> Please enter ${dottedConfigPropertyName}: ")
        if(cp == null) // no console...
          throwMissingConfigPropertyException(dottedConfigPropertyName)
        return cp

      default:
        throw new RuntimeException('not reached')
    }

  }

  /**
   * Try to locate the config property using a dotted name (ex: top.build.dir). First try
   * the project, then the userConfig (if present), then the spec object (if present). When the
   * property is not found
   *
   * @return the property (can be anything specified)
   * @return <code>null</code> if not found
   */
  private static def doGetConfigProperty(Project project,
                                         String dottedConfigPropertyName)
  {
    if(!dottedConfigPropertyName)
      throw new IllegalArgumentException("missing property name")

    // make sure it is a string
    dottedConfigPropertyName = dottedConfigPropertyName.toString()

    def configPropertyNameParts = dottedConfigPropertyName.split('\\.')

    // 1. we check user config
    if(project.hasProperty('userConfig'))
    {
      def userConfig = project.userConfig
      def configProperty = userConfig."${dottedConfigPropertyName}"
      if(configProperty instanceof ConfigObject)
      {
        if(configPropertyNameParts.size() > 1)
        {
          configProperty = userConfig."${configPropertyNameParts[0]}"
          configPropertyNameParts[1..-1].each { p ->
            configProperty = configProperty?."${p}"
          }
        }
      }

      // found the config property => we are done...
      if(!(configProperty instanceof ConfigObject))
        return configProperty
    }

    // 2. we check spec
    if(project.hasProperty('spec'))
    {
      def spec = project.spec
      def configProperty = spec."${dottedConfigPropertyName}"
      if(configProperty == null)
      {
        if(configPropertyNameParts.size() > 1)
        {
          configProperty = spec."${configPropertyNameParts[0]}"
          configPropertyNameParts[1..-1].each { p ->
            configProperty = configProperty?."${p}"
          }
        }

      }

      // found the config property => we are done...
      if(configProperty != null)
        return configProperty
    }

    return null
  }

  private static void throwMissingConfigPropertyException(String propertyName)
  {
    throw new MissingResourceException("Cannot locate property ${propertyName}: either use userConfig or spec plugin to define it or -P${propertyName}=xxx on the command line")
  }

  /**
   * Note that this class is copied from linkedin-utils.
   * We don't really want to have this kind of dependency in this base project.
   * 
   * Flattens the map. Ex: [a: 1, b: [1,2], c: [d: 1]] returns a map:
   * <code>[a: 1, 'b[0]': 1, 'b[1]': 2, 'c.d': 1]</code>
   * @return a new map
   */
  static Map flatten(Map map)
  {
    if(map == null)
      return null

    Map flattenedMap = [:]

    doFlatten(map, flattenedMap, '')

    return flattenedMap
  }

  /**
   * Same as {@link #flatten(Map)} but use <code>destMap</code> for the result
   * @return <code>destMap</code>
   */
  static Map flatten(Map srcMap, Map destMap)
  {
    if(srcMap == null)
      return

    doFlatten(srcMap, destMap, '')

    return destMap
  }

  private static void doFlatten(Map map, def flattenedMap, String prefix)
  {
    map?.each { k, v ->

      def key = prefix ? "${prefix}.${k}".toString() : k

      switch(v)
      {
        case { v instanceof Map}:
          doFlatten(v, flattenedMap, key)
          break

        case { v instanceof Collection}:
          doFlatten(v, flattenedMap, key)
          break

        default:
          flattenedMap[key] = v

      }
    }
  }

  private static void doFlatten(Collection c, def flattenedMap, String prefix)
  {
    c?.eachWithIndex { e, idx ->
      def key = "${prefix}[${idx}]".toString()

      switch(e)
      {
        case { e instanceof Map}:
          doFlatten(e, flattenedMap, key)
          break

        case { e instanceof Collection}:
          doFlatten(e, flattenedMap, key)
          break

        default:
          flattenedMap[key] = e
      }
    }
  }

  /**
   * Converts the string into a boolean value. <code>true</code> or
   * <code>yes</code> are considered to be <code>true</code>. <code>false</code>
   * or <code>no</code> are <code>false</code>.
   *
   * @param s the string to convert
   * @return a <code>boolean</code>  */
  public static boolean convertToBoolean(def s)
  {
    if(s == null)
      return false;

    s = s.toString()

    if(s == "false" || s == "no" || s == "off")
      return false;

    if(s == "true" || s == "yes" || s == "on")
      return true;

    throw new IllegalArgumentException("cannot convert ${s} to a boolean")
  }

  /**
   * Removed from Gradle... copied back...  */
  private static final long MS_PER_MINUTE = 60000;
  private static final long MS_PER_HOUR = MS_PER_MINUTE * 60;

  public static String prettyTime(long timeInMs) {
    StringBuilder result = new StringBuilder();
    if (timeInMs > MS_PER_HOUR) {
      result.append(timeInMs / MS_PER_HOUR).append(" hrs ");
    }
    if (timeInMs > MS_PER_MINUTE) {
      result.append((timeInMs % MS_PER_HOUR) / MS_PER_MINUTE).append(" mins ");
    }
    result.append((timeInMs % MS_PER_MINUTE) / 1000.0).append(" secs");
    return result.toString();
  }
}
