/*
 * Copyright (c) 2013 Yan Pujante
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
spec = [
  name: 'gradle-plugins',
  group: 'org.pongasoft',
  version: '1.0.0',

  versions: [
    groovy: '1.7.5'
  ],

  artifacts: ["a1", "a2"],

  // information about the build framework itself
  build: [
    type: "gradle",
    version: "0.9-rc-2",
    commands: [
      "snapshot": "gradle -Psnapshot=true release",
      "release": "gradle -Prelease=true release"
    ]
  ]
]

spec.scmUrl = "git@github.com:linkedin/${spec.name}.git"
spec.build.uri = "http://dist.codehaus.org/gradle/gradle-${spec.build.version}-all.zip"

/**
 * External dependencies
 */
spec.external = [
  json: 'org.json:json:20090211',
  groovy: "org.codehaus.groovy:groovy:${spec.versions.groovy}"
]
