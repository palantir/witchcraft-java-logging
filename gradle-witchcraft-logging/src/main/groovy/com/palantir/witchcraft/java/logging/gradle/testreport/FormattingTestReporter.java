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

import com.palantir.witchcraft.java.logging.format.LogFormatter;
import com.palantir.witchcraft.java.logging.format.LogParser;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.function.BiConsumer;
import org.gradle.api.Action;
import org.gradle.api.internal.tasks.testing.junit.result.TestClassResult;
import org.gradle.api.internal.tasks.testing.junit.result.TestResultsProvider;
import org.gradle.api.internal.tasks.testing.report.HtmlTestReport;
import org.gradle.api.internal.tasks.testing.report.TestReporter;
import org.gradle.api.tasks.testing.TestOutputEvent;
// CHECKSTYLE:ON

final class FormattingTestReporter implements TestReporter {

    private final Object delegate;

    FormattingTestReporter(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public void generateReport(TestResultsProvider testResultsProvider, File file) {
        if (delegate instanceof TestReporter) {
            ((TestReporter) delegate).generateReport(new FormattingTestResultsProvider(testResultsProvider), file);
        } else if (delegate instanceof HtmlTestReport) {
            ((HtmlTestReport) delegate).generateReport(new FormattingTestResultsProvider(testResultsProvider), file);
        } else {
            throw new IllegalArgumentException("Unknown delegate class: " + delegate.getClass());
        }
    }

    private static final class FormattingTestResultsProvider implements TestResultsProvider {

        private static final LogParser<Writable> PARSER = new LogParser<>(TestLogFilter.INSTANCE.combineWith(
                LogFormatter.INSTANCE,
                (include, formatted) -> include
                        ? writer -> {
                            writer.write(formatted);
                            writer.write('\n');
                        }
                        : Writable.NOP));
        private static final BiConsumer<String, Writer> LINE_PROCESSOR = (line, outputWriter) -> {
            try {
                PARSER.tryParse(line)
                        .orElseGet(() -> out -> {
                            try {
                                out.write(line);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        })
                        .write(outputWriter);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        private final TestResultsProvider delegate;

        FormattingTestResultsProvider(TestResultsProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public void writeAllOutput(long classId, TestOutputEvent.Destination destination, Writer writer) {
            if (destination == TestOutputEvent.Destination.StdErr
                    || destination == TestOutputEvent.Destination.StdOut) {
                delegate.writeAllOutput(classId, destination, new LineProcessingWriter(writer, LINE_PROCESSOR));
            } else {
                delegate.writeAllOutput(classId, destination, writer);
            }
        }

        private static class LineProcessingWriter extends Writer {
            private final Writer delegate;
            private final BiConsumer<String, Writer> lineProcessor;
            private final StringBuilder lineBuffer = new StringBuilder();

            LineProcessingWriter(Writer delegate, BiConsumer<String, Writer> lineProcessor) {
                this.delegate = delegate;
                this.lineProcessor = lineProcessor;
            }

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                for (int i = off; i < off + len; i++) {
                    char ch = cbuf[i];
                    if (ch == '\n') {
                        processLine();
                    } else {
                        lineBuffer.append(ch);
                    }
                }
            }

            @Override
            public void flush() throws IOException {
                delegate.flush();
            }

            @Override
            public void close() throws IOException {
                if (!lineBuffer.isEmpty()) {
                    processLine();
                }
                delegate.close();
            }

            private void processLine() throws IOException {
                String line = lineBuffer.toString();
                lineProcessor.accept(line, delegate);
                delegate.write('\n');
                lineBuffer.setLength(0);
            }
        }

        @Override
        public void writeNonTestOutput(long classId, TestOutputEvent.Destination destination, Writer writer) {
            delegate.writeNonTestOutput(classId, destination, writer);
        }

        @Override
        public void writeTestOutput(long classId, long testId, TestOutputEvent.Destination destination, Writer writer) {
            delegate.writeTestOutput(classId, testId, destination, writer);
        }

        @Override
        public void visitClasses(Action<? super TestClassResult> visitor) {
            delegate.visitClasses(visitor);
        }

        @Override
        public boolean hasOutput(long classId, TestOutputEvent.Destination destination) {
            return delegate.hasOutput(classId, destination);
        }

        @Override
        public boolean hasOutput(long classId, long testId, TestOutputEvent.Destination destination) {
            return delegate.hasOutput(classId, testId, destination);
        }

        @Override
        public boolean isHasResults() {
            return delegate.isHasResults();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
