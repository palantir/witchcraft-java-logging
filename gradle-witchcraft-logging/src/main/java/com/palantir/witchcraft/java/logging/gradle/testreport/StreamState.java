/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
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

import com.palantir.witchcraft.java.logging.format.LogFormatter;
import com.palantir.witchcraft.java.logging.format.LogParser;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;

/// A state machine that processes Gradle HTML test report lines, filtering and
/// formatting log output within `<pre>` blocks that follow standard
/// output/error headers.
///
/// ```
///  ┌───────────┐  <h2>   ┌──────────────┐ <pre> ┌────────┐
///  │  DEFAULT  │────────▶│ AWAITING_PRE │──────▶│ IN_PRE │
///  └─────┬─────┘         └──────────────┘       └───┬────┘
///    ↺ any line            ↺ any line            ↺ any line
///    passthrough            passthrough           formatAndWrite
///        ▲                                          │
///        └──────── </pre> ── formatAndWrite ────────┘
/// ```
final class StreamState {
    private static final LogParser<Optional<String>> PARSER = new LogParser<>(TestLogFilter.INSTANCE.combineWith(
            LogFormatter.INSTANCE, (include, formatted) -> include ? Optional.of(formatted) : Optional.empty()));

    private final BufferedWriter writer;
    private State state = Default.INSTANCE;

    StreamState(BufferedWriter writer) {
        this.writer = writer;
    }

    void processLine(String line) {
        try {
            StepResult result = state.step(this, line);
            state = result.next();
            if (result.remainder().isPresent()) {
                result = state.step(this, result.remainder().get());
                state = result.next();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record StepResult(Optional<String> remainder, State next) {}

    private sealed interface State {
        StepResult step(StreamState ctx, String input) throws IOException;
    }

    private static final class Default implements State {
        static final State INSTANCE = new Default();

        private static final Pattern OUTPUT_HEADER_PATTERN =
                Pattern.compile("<h2>standard (?:output|error)</h2>", Pattern.CASE_INSENSITIVE);

        @Override
        public StepResult step(StreamState ctx, String input) throws IOException {
            ctx.passthrough(input);
            if (OUTPUT_HEADER_PATTERN.matcher(input).find()) {
                return new StepResult(Optional.empty(), AwaitingPre.INSTANCE);
            }
            return new StepResult(Optional.empty(), this);
        }
    }

    private static final class AwaitingPre implements State {
        static final State INSTANCE = new AwaitingPre();

        private static final Pattern PRE_OPEN_PATTERN = Pattern.compile("<pre[^>]*>");

        @Override
        public StepResult step(StreamState ctx, String input) throws IOException {
            Matcher matcher = PRE_OPEN_PATTERN.matcher(input);
            if (!matcher.find()) {
                ctx.passthrough(input);
                return new StepResult(Optional.empty(), this);
            }

            ctx.writer.write(input, 0, matcher.end());
            ctx.writer.newLine();

            String remainder = input.substring(matcher.end());
            return new StepResult(remainder.isEmpty() ? Optional.empty() : Optional.of(remainder), InPre.INSTANCE);
        }
    }

    private static final class InPre implements State {
        static final State INSTANCE = new InPre();

        @Override
        public StepResult step(StreamState ctx, String input) throws IOException {
            int closeIdx = input.indexOf("</pre>");
            if (closeIdx < 0) {
                ctx.formatAndWrite(input);
                return new StepResult(Optional.empty(), this);
            }

            if (closeIdx > 0) {
                ctx.formatAndWrite(input.substring(0, closeIdx));
            }
            ctx.writer.write(input, closeIdx, input.length() - closeIdx);
            ctx.writer.newLine();
            return new StepResult(Optional.empty(), Default.INSTANCE);
        }
    }

    private void passthrough(String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }

    private void formatAndWrite(String rawLine) throws IOException {
        Optional<String> formatted = formatLine(rawLine);
        if (formatted.isPresent()) {
            writer.write(formatted.get());
            writer.newLine();
        }
    }

    private static Optional<String> formatLine(String line) {
        return PARSER.tryParse(StringEscapeUtils.unescapeHtml4(line))
                .map(opt -> opt.map(StringEscapeUtils::escapeHtml4))
                .orElseGet(() -> Optional.of(line));
    }
}
