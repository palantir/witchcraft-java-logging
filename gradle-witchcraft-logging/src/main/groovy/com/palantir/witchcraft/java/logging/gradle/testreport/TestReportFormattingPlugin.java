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

package com.palantir.witchcraft.java.logging.gradle.testreport;

import java.io.File;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.flow.FlowAction;
import org.gradle.api.flow.FlowParameters;
import org.gradle.api.flow.FlowScope;
import org.gradle.api.provider.Property;
import org.gradle.api.reporting.DirectoryReport;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.testing.AbstractTestTask;

/**
 * Plugin that formats witchcraft structured logging output in HTML test reports.
 * Post-processes generated HTML files to replace JSON log lines with formatted versions.
 */
public abstract class TestReportFormattingPlugin implements Plugin<Project> {

    @Inject
    protected abstract FlowScope getFlowScope();

    @Override
    public final void apply(Project project) {
        project.getTasks().withType(AbstractTestTask.class).configureEach(task -> {
            getFlowScope().always(FormatReportAction.class, spec -> spec.getParameters()
                    .getReportDir()
                    .set(task.getReports().getHtml()));
        });
    }

    public static final class FormatReportAction implements FlowAction<FormatReportAction.Parameters> {
        interface Parameters extends FlowParameters {
            @Input
            Property<DirectoryReport> getReportDir();
        }

        @Override
        public void execute(Parameters parameters) {
            File reportDirectory = parameters
                    .getReportDir()
                    .get()
                    .getOutputLocation()
                    .getAsFile()
                    .getOrNull();

            if (reportDirectory == null || !reportDirectory.exists()) {
                return;
            }

            new HtmlReportPostProcessor().processReportDirectory(reportDirectory);
        }
    }
}
