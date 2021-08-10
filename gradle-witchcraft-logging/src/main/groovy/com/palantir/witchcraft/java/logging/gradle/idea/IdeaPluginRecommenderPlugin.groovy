/*
 * (c) Copyright 2021 Palantir Technologies Inc. All rights reserved.
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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.plugins.ide.idea.model.IdeaModel

class IdeaPluginRecommenderPlugin implements Plugin<Project> {

    static WITCHCRAFT_LOGGING_PLUGIN_MINIMUM_VERSION = '1.6.0'

    @Override
    void apply(Project project) {
        if (Objects.equals(project, project.getRootProject())) {
            project.getPluginManager().withPlugin("idea", unused -> {
                // Support the .ipr file when the idea plugin is applied
                IdeaModel ideaRootModel = project.extensions.findByType(IdeaModel)
                ideaRootModel.project.ipr.withXml {XmlProvider provider ->
                    Node node = provider.asNode()
                    recommendWitchcraftLoggingPlugin(node)
                }
            })
            if (Boolean.getBoolean("idea.active")) {
                XmlUtils.createOrUpdateXmlFile(
                        project.file(".idea/externalDependencies.xml"),
                        IdeaPluginRecommenderPlugin.&recommendWitchcraftLoggingPlugin)
            }
        }
    }

    private static void recommendWitchcraftLoggingPlugin(Node rootNode) {
        def externalDependencies = matchOrCreateChild(rootNode, 'component', [name: 'ExternalDependencies'])
        matchOrCreateChild(
                externalDependencies,
                'plugin',
                [id: 'com.palantir.witchcraft.api.logging.idea'],
                ['min-version': WITCHCRAFT_LOGGING_PLUGIN_MINIMUM_VERSION])
    }

    private static Node matchOrCreateChild(
            Node base,
            String name,
            Map attributes = [:],
            Map defaults = [:],
            @DelegatesTo(value = Node, strategy = Closure.DELEGATE_FIRST) Closure ifCreated = {}) {
        def child = base[name].find { it.attributes().entrySet().containsAll(attributes.entrySet()) }
        if (child) {
            return child
        }

        def created = base.appendNode(name, attributes + defaults)
        ifCreated.delegate = created
        ifCreated(created)
        return created
    }
}
