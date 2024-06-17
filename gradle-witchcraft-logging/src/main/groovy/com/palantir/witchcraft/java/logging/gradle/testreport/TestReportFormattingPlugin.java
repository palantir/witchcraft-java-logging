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
import org.gradle.api.internal.tasks.testing.report.DefaultTestReport;
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
                method.invoke(task, new FormattingTestReporter(createDefaultTestReport()));
            } catch (ReflectiveOperationException e) {
                project.getLogger()
                        .error(
                                "Failed to update task '{}' TestReporter to format structured logging output",
                                task.getName(),
                                e);
            }
        });
    }

    private DefaultTestReport createDefaultTestReport() {
        boolean greaterThan8Point8 = GradleVersion.current().compareTo(GradleVersion.version("8.8")) >= 0;

        try {
            if (greaterThan8Point8) {
                return DefaultTestReport.class
                        .getDeclaredConstructor(BuildOperationRunner.class, BuildOperationExecutor.class)
                        .newInstance(getBuildOperationRunner(), getBuildOperationExecutor());
            } else {
                return DefaultTestReport.class
                        .getDeclaredConstructor(BuildOperationExecutor.class)
                        .newInstance(getBuildOperationExecutor());
            }
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
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
