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
spec = [
  name: 'gradle-plugins',
  group: 'org.pongasoft',
  version: '2.2.5',

  versions: [
    jdk: '1.7'
  ],

  artifacts: ["a1", "a2"],

  // information about the build framework itself
  build: [
    type: "gradle", // version 1.7
    commands: [
      "snapshot": "./gradlew <xxx>",
      "release": "./gradlew -Prelease=true <xxx>"
    ]
  ],
]

spec.scmUrl = "git@github.com:pongasoft/${spec.name}.git"
spec.scm = 'git'

/**
 * External dependencies
 */
spec.external = [
  httpBuilder: "org.codehaus.groovy.modules.http-builder:http-builder:0.6"
]

// information about the bintray distribution
spec.bintray = [
  apiBaseUrl: 'https://bintray.com/api/v1',
  username: 'yan',
  pkgOrganization: 'pongasoft',
  repositories: [
    binaries: [
      pkgRepository: 'binaries',
      pkgName: spec.name
    ],
//    distributions: [
//      pkgRepository: spec.name,
//      pkgName: 'releases'
//    ],
  ]
]
