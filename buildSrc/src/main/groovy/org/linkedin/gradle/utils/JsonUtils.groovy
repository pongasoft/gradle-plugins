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


package org.linkedin.gradle.utils

import org.json.JSONObject
import org.json.JSONArray

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
    prettyPrint(value, 2)
  }

  /**
   * Converts the value to a json object and displays it nicely indented
   */
  static String prettyPrint(value, int indent)
  {
    def json = toJSON(value)
    if(json == null)
      return null
    return json.toString(indent)
  }

  /**
   * Given a json string, convert it to a value (maps / lists): equivalent to
   * <code>toValue(new JSONObject(json))</code> or <code>toValue(new JSONArray(json)</code>
   * depending on if the json starts with <code>[</code> or <code>{</code>
   * (with proper <code>null</code> handling).
   */
  static def fromJSON(String json)
  {
    if(json == null)
      return null
    json = json.trim()
    if(json.startsWith('['))
      return toValue(new JSONArray(json))
    else
      return toValue(new JSONObject(json))
  }

  /**
   * Converts the value into its non JSON counter part: if the value is a {@link org.json.JSONObject}
   * or a {@link org.json.JSONArray} then it will call the appropriate method, otherwise simply return the
   * value.
   */
  static def toValue(value)
  {
    if(value == null)
      return null

    if(value instanceof JSONObject)
    {
      return toMap((JSONObject) value)
    }
    else if(value instanceof JSONArray)
    {
      return toList((JSONArray) value)
    }

    return value
  }

  /**
   * Converts a json object into a map (recursive call).
   */
  static def toMap(JSONObject json)
  {
    if(json == null)
      return null

    def map = [:]
    json?.keys().each { key ->
      map[key] = toValue(json.get(key))
    }
    return map
  }

  /**
   * Converts the json array into a list (recursive call).
   */
  static def toList(JSONArray array)
  {
    if(array == null)
      return null

    def list = []

    (0..<array.length()).each { idx ->
      list << toValue(array.get(idx))
    }

    return list
  }

  /**
   * Reverse method which converts a value into a json value
   */
  static toJSON(value)
  {
    if(value == null)
      return null

    if(value instanceof Map)
    {
      return mapToJSON((Map) value)
    }

    if(value instanceof Collection)
    {
      return collectionToJSON((Collection) value)
    }

    return value
  }

  static mapToJSON(Map map)
  {
    if(map == null)
      return null

    JSONObject json = new JSONObject()
    map.each {k, v ->
      json.put(k?.toString(), toJSON(v))
    }
    return json
  }

  static collectionToJSON(Collection list)
  {
    if(list == null)
      return null

    JSONArray array = new JSONArray()
    list.each { elt ->
      array.put(toJSON(elt))
    }
    
    return array
  }

  static String explainJsonParsingError(String errorMessage, def reader)
  {
    def matcher = errorMessage =~ /at character ([0-9]+)$/
    if(matcher.hasGroup())
    {
      String previousLine = null
      int lineCount = 0
      long charCount = 0
      def lines = []
      long character = matcher[0][1] as long
      reader.eachLine { String line ->
        if(!lines)
        {
          lineCount++
          if((charCount + line.size()) >= character)
          {
            if(previousLine != null)
            {
              lines << "${lineCount - 1}: ${previousLine}"
            }

            lines << "${lineCount}: ${line}"
            lines << "${lineCount}: ${' ' * (character - charCount)}^"
          }
          else
          {
            previousLine = line
          }
          charCount += line.size() + 1
        }
      }
      return lines.join("\n")
    }
    else
      return null
  }
}