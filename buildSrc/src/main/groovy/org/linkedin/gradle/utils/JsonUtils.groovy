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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Contains utilities for json. Note that this class is copied from linkedin-utils.
 * We don't really want to have this kind of dependency in this base project.
 * 
 * @author ypujante@linkedin.com
 */
class JsonUtils
{
  /**
   * Converts the value to a json object and displays it nicely indented
   */
  static String prettyPrint(value)
  {
    JsonOutput.prettyPrint(JsonOutput.toJson(value))
  }

  /**
   * Given a json string, convert it to a value (maps / lists)
   */
  static def fromJSON(String json)
  {
    if(json == null)
      return null
    json = json.trim()

    new JsonSlurper().parseText(json)
  }
}