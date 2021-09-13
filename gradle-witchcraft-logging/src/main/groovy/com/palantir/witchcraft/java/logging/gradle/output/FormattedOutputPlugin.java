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

package com.palantir.witchcraft.java.logging.gradle.output;

import com.palantir.witchcraft.java.logging.format.LogFormatter;
import com.palantir.witchcraft.java.logging.format.LogParser;
import java.io.PrintStream;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.testing.AbstractTestTask;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestOutputEvent.Destination;

public abstract class FormattedOutputPlugin implements Plugin<Project> {

    private static final LogParser<String> PARSER =
            new LogParser<>(LogFormatter.INSTANCE.combineWith(TestLogFilter.INSTANCE, (formatted, include) -> {
                if (!include) {
                    return "";
                }
                return formatted + System.lineSeparator();
            }));

    @Override
    public final void apply(Project project) {
        project.getTasks()
                .withType(AbstractTestTask.class)
                .configureEach(task -> task.addTestOutputListener((descriptor, event) -> {
                    PrintStream target = event.getDestination() == Destination.StdErr ? System.err : System.out;
                    String message = event.getMessage();
                    String parsedMessage = PARSER.tryParse(message).orElse(message);
                    if (!parsedMessage.isEmpty()) {
                        target.printf("[%s] %s", testDescriptor(descriptor), parsedMessage);
                    }
                }));
    }

    private static String testDescriptor(TestDescriptor input) {
        String className = input.getClassName();
        if (className == null) {
            return input.getDisplayName();
        }
        int lastIndex = className.lastIndexOf('.');
        if (lastIndex == className.length() - 1) {
            return input.getDisplayName();
        }
        String abbreviated = lastIndex < 0 ? className : className.substring(lastIndex + 1);
        return abbreviated + '.' + input.getDisplayName();
    }
}
