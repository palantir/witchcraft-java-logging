/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.witchcraft.java.logging.gradle;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.SubProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class WitchcraftLoggingPluginTest {

    @Test
    void applies_to_root_project_and_subprojects(GradleInvoker gradle, RootProject rootProject, SubProject subProject) {
        rootProject.buildGradle().plugins().add("com.palantir.witchcraft-logging");

        rootProject.buildGradle().append("""
            println "root:witchcraft-logging=" + pluginManager.hasPlugin('com.palantir.witchcraft-logging')
            println "root:idea-configuration=" + pluginManager.hasPlugin('com.palantir.idea-configuration')
            println "root:witchcraft-logging-testreport=" + pluginManager.hasPlugin('com.palantir.witchcraft-logging-testreport')
            """);

        subProject.buildGradle().append("""
            println "sub:witchcraft-logging=" + pluginManager.hasPlugin('com.palantir.witchcraft-logging')
            println "sub:idea-configuration=" + pluginManager.hasPlugin('com.palantir.idea-configuration')
            println "sub:witchcraft-logging-testreport=" + pluginManager.hasPlugin('com.palantir.witchcraft-logging-testreport')
            """);

        gradle.withArgs()
                .buildsSuccessfully()
                .assertThat()
                .output()
                .contains("root:witchcraft-logging=true")
                .contains("root:idea-configuration=true")
                .contains("root:witchcraft-logging-testreport=true")
                .contains("sub:witchcraft-logging=false")
                .contains("sub:idea-configuration=false")
                .contains("sub:witchcraft-logging-testreport=true");
    }
}
