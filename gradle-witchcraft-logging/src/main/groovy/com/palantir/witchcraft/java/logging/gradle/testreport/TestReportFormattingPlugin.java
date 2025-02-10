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

// CHECKSTYLE:OFF

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.tasks.testing.report.HtmlTestReport;
import org.gradle.api.internal.tasks.testing.report.TestReporter;
import org.gradle.api.tasks.testing.AbstractTestTask;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.BuildOperationRunner;
import org.gradle.util.GradleVersion;
// CHECKSTYLE:ON

/**
 * In its current form, this plugin may generously be described as "a workaround".
 * I've filed <a href="https://github.com/gradle/gradle/issues/17966">gradle#17966</a>
 * upstream to find a better solution.
 * We may be able to consume the xml test report and generate our own html based on that
 * if the current approach becomes troublesome, that would allow us to color individual
 * lines much like our intellij plugin.
 */
public abstract class TestReportFormattingPlugin implements Plugin<Project> {

    @Override
    @SuppressWarnings("Slf4jLogsafeArgs")
    public final void apply(Project project) {
        project.getTasks().withType(AbstractTestTask.class).configureEach(task -> {
            try {
                Method method = AbstractTestTask.class.getDeclaredMethod("setTestReporter", TestReporter.class);
                method.setAccessible(true);

                TestReporter formattingReporter = getFormattingReporter();

                method.invoke(task, formattingReporter);
            } catch (ReflectiveOperationException e) {
                project.getLogger()
                        .error(
                                "Failed to update task '{}' TestReporter to format structured logging output",
                                task.getName(),
                                e);
            }
        });
    }

    /**
     * The internal gradle test report classes changed in 8.11.  DefaultTestReport was renamed to HtmlTestReport and
     * also stopped extending the TestReporter interface.  So the delegate for the formatting reporter varies either
     * an HtmlTestReport to be compatible with gradle 8.11+ or DefaultTestReport for lower versions.
     */
    private TestReporter getFormattingReporter() {
        boolean greaterThan8Point11 = GradleVersion.current().compareTo(GradleVersion.version("8.11")) >= 0;
        if (greaterThan8Point11) {
            return new FormattingTestReporter(
                    new HtmlTestReport(getBuildOperationRunner(), getBuildOperationExecutor()));
        } else {
            return new FormattingTestReporter(createDefaultTestReport());
        }
    }

    /**
     * The constructor for DefaultTestReport changed in gradle 8.8.  Dynamically invoke based on runtime version.
     */
    private TestReporter createDefaultTestReport() {
        boolean greaterThan8Point8 = GradleVersion.current().compareTo(GradleVersion.version("8.8")) >= 0;

        try {
            Class<TestReporter> defaultTestReporterClass = (Class<TestReporter>)
                    Class.forName("org.gradle.api.internal.tasks.testing.report.DefaultTestReport");
            if (greaterThan8Point8) {
                return defaultTestReporterClass
                        .getDeclaredConstructor(BuildOperationRunner.class, BuildOperationExecutor.class)
                        .newInstance(getBuildOperationRunner(), getBuildOperationExecutor());
            } else {
                return defaultTestReporterClass
                        .getDeclaredConstructor(BuildOperationExecutor.class)
                        .newInstance(getBuildOperationExecutor());
            }
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | ClassNotFoundException
                | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject
    @SuppressWarnings("DesignForExtension")
    protected abstract BuildOperationExecutor getBuildOperationExecutor();

    @Inject
    @SuppressWarnings("DesignForExtension")
    protected abstract BuildOperationRunner getBuildOperationRunner();
}
