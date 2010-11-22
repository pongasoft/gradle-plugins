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

package test.gradle.plugins

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import org.linkedin.gradle.plugins.SpecPlugin

/**
 * @author yan@pongasoft.com */
public class TestSpecPlugin extends GroovyTestCase
{
  public void testJson()
  {
    ProjectBuilder builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testJson"))
    Project project = builder.build()
    project.apply plugin: SpecPlugin
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

  public void testVersion()
  {
    // when release=true => version should not be snapshot
    ProjectBuilder builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testJson"))
    Project project = builder.build()
    project.release = true
    project.apply plugin: SpecPlugin
    assertEquals('1.0.0', project.spec.version)

    // when snapshot=true => version should be snapshot
    builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testJson"))
    project = builder.build()
    project.snapshot = true
    project.apply plugin: SpecPlugin
    assertEquals('1.0.0-SNAPSHOT', project.spec.version)

    // when release=true => version should not be snapshot (in this case it throws an exception)
    builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testVersion"))
    project = builder.build()
    project.release = true
    shouldFail(IllegalStateException) { project.apply plugin: SpecPlugin }

    // when snapshot=true => version should be snapshot
    builder = ProjectBuilder.builder()
    builder.withProjectDir(new File("src/test/resources/TestSpecPlugin/testVersion"))
    project = builder.build()
    project.snapshot = true
    project.apply plugin: SpecPlugin
    assertEquals('1.0.0-SNAPSHOT', project.spec.version)
  }
}