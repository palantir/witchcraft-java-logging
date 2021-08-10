/*
 * (c) Copyright 2015 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.witchcraft.java.logging.gradle.idea

import nebula.test.IntegrationSpec
import spock.util.environment.RestoreSystemProperties

class IdeaPluginRecommenderPluginIntegrationTest extends IntegrationSpec {

    def 'idea configures the logging plugin for .ipr'() {
        when:
        buildFile << """
            apply plugin: 'idea'
            ${applyPlugin(IdeaPluginRecommenderPlugin)}
            """.stripIndent(true)

        then:
        runTasksSuccessfully('idea')
        fileExists("${moduleName}.ipr")
        def iprFile = new File(projectDir, "${moduleName}.ipr")
        def ipr = new XmlSlurper().parse(iprFile)
        ipr.component.find { it.@name == "ExternalDependencies" }
    }

    @RestoreSystemProperties
    def "configures the logging plugin for IntelliJ import"() {
        buildFile << """
            ${applyPlugin(IdeaPluginRecommenderPlugin)}
            """.stripIndent(true)

        when:
        System.setProperty("idea.active", "true")
        runTasksSuccessfully('tasks')

        then:
        def externalDepsSettingsFile = new File(projectDir, ".idea/externalDependencies.xml")
        def deps = new XmlSlurper().parse(externalDepsSettingsFile)
        deps.component.find { it.@name == "ExternalDependencies" }
    }
}
