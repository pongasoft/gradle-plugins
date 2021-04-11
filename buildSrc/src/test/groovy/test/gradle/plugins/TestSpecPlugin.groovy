/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2013-2021 Yan Pujante
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

package test.gradle.plugins

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * @author yan@pongasoft.com */
public class TestSpecPlugin
{
  @Test
  public void testJson()
  {
    ProjectBuilder builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testJson"))
    Project project = builder.build()
    project.ext.release = true
    project.pluginManager.apply("org.linkedin.spec")
    assertEquals('gradle-plugins', project.spec.name)
    assertEquals('org.linkedin', project.spec.group)
    assertEquals('1.0.0', project.spec.version)
    assertEquals(['a1', 'a2'], project.spec.artifacts)
    assertEquals('git@github.com:linkedin/gradle-plugins.git', project.spec.scmUrl)
    assertEquals('1.7.5', project.spec.versions.groovy)
    assertEquals('org.json:json:20090211', project.spec.external.json)
    assertEquals('org.codehaus.groovy:groovy:1.7.5', project.spec.external.groovy)
    assertEquals('gradle', project.spec.build.type)
    assertEquals('0.9-rc-2', project.spec.build.version)
    assertEquals('http://dist.codehaus.org/gradle/gradle-0.9-rc-2-all.zip', project.spec.build.uri)
    assertEquals('gradle -Psnapshot=true release', project.spec.build.commands.snapshot)
    assertEquals('gradle -Prelease=true release', project.spec.build.commands.release)
  }

  @Test
  public void testGroovy()
  {
    ProjectBuilder builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testGroovy"))
    Project project = builder.build()
    project.ext.release = true
    project.pluginManager.apply("org.linkedin.spec")
    assertEquals('gradle-plugins', project.spec.name)
    assertEquals('org.linkedin', project.spec.group)
    assertEquals('1.0.0', project.spec.version)
    assertEquals(['a1', 'a2'], project.spec.artifacts)
    assertEquals('git@github.com:linkedin/gradle-plugins.git', project.spec.scmUrl.toString())
    assertEquals('1.7.5', project.spec.versions.groovy)
    assertEquals('org.json:json:20090211', project.spec.external.json)
    assertEquals('org.codehaus.groovy:groovy:1.7.5', project.spec.external.groovy.toString())
    assertEquals('gradle', project.spec.build.type)
    assertEquals('0.9-rc-2', project.spec.build.version)
    assertEquals('http://dist.codehaus.org/gradle/gradle-0.9-rc-2-all.zip', project.spec.build.uri.toString())
    assertEquals('gradle -Psnapshot=true release', project.spec.build.commands.snapshot)
    assertEquals('gradle -Prelease=true release', project.spec.build.commands.release)
  }

  @Test
  public void testVersion()
  {
    // when release=true => version should not be snapshot
    ProjectBuilder builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testJson"))
    Project project = builder.build()
    project.ext.release = true
    project.pluginManager.apply("org.linkedin.spec")
    assertEquals('1.0.0', project.spec.version)

    // when nothing provided => version should be snapshot
    builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testJson"))
    project = builder.build()
    project.pluginManager.apply("org.linkedin.spec")
    assertEquals('1.0.0-SNAPSHOT', project.spec.version)

    // when release=true => version should not be snapshot (in this case it throws an exception)
    builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testVersion"))
    project = builder.build()
    project.ext.release = true
    try
    {
      project.pluginManager.apply("org.linkedin.spec")
      Assertions.fail("should have failed")
    }
    catch(Throwable e)
    {
      try
      {
        throw e.cause
      }
      catch(IllegalStateException ise)
      {
        // what should happen
      }
    }

    // when nothing provided => version should be snapshot
    builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testVersion"))
    project = builder.build()
    project.pluginManager.apply("org.linkedin.spec")
    assertEquals('1.0.0-SNAPSHOT', project.spec.version)
  }
}