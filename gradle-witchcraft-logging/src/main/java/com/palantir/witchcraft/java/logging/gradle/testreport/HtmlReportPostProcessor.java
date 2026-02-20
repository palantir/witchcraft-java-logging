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

import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import com.palantir.witchcraft.java.logging.format.LogFormatter;
import com.palantir.witchcraft.java.logging.format.LogParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Post-processes HTML test report files to apply witchcraft log formatting.
 * Uses line-by-line streaming to handle arbitrarily large files without loading them into memory.
 */
final class HtmlReportPostProcessor {

    private static final LogParser<Optional<String>> PARSER = new LogParser<>(TestLogFilter.INSTANCE.combineWith(
            LogFormatter.INSTANCE, (include, formatted) -> include ? Optional.of(formatted) : Optional.empty()));

    private static final Pattern HEADER_PATTERN =
            Pattern.compile("<h2>standard (?:output|error)</h2>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRE_OPEN_PATTERN = Pattern.compile("<pre[^>]*>");
    private static final String PRE_CLOSE_TAG = "</pre>";

    void processReportDirectory(File reportDir) {
        Optional.ofNullable(reportDir).filter(File::isDirectory).ifPresent(this::walkAndProcessHtmlFiles);
    }

    private void walkAndProcessHtmlFiles(File reportDir) {
        try (Stream<Path> paths = Files.walk(reportDir.toPath())) {
            paths.filter(path -> path.toString().endsWith(".html")).forEach(this::processHtmlFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void processHtmlFile(Path htmlFile) {
        Path tempFile = htmlFile.resolveSibling(htmlFile.getFileName() + ".tmp");

        try (BufferedReader reader = Files.newBufferedReader(htmlFile, StandardCharsets.UTF_8);
                BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            boolean changed = CharStreams.readLines(reader, new FormattingLineProcessor(writer));
            replaceIfChanged(changed, tempFile, htmlFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void replaceIfChanged(boolean changed, Path tempFile, Path original) throws IOException {
        if (changed) {
            Files.move(tempFile, original, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.deleteIfExists(tempFile);
        }
    }

    private static Optional<String> formatLine(String line) {
        return PARSER.tryParse(StringEscapeUtils.unescapeHtml4(line))
                .map(opt -> opt.map(formatted -> StringEscapeUtils.escapeHtml4(formatted) + "\n"))
                .orElseGet(() -> Optional.of(line));
    }

    /**
     * Stateful {@link LineProcessor} that writes transformed lines to a {@link Writer}.
     * State transitions between handlers are expressed as function references.
     */
    private static final class FormattingLineProcessor implements LineProcessor<Boolean> {
        private final Writer writer;
        private UnaryOperator<String> handler = this::passthrough;
        private boolean needsSeparator;
        private boolean changed;

        FormattingLineProcessor(Writer writer) {
            this.writer = writer;
        }

        @Override
        public boolean processLine(String line) throws IOException {
            writer.write(handler.apply(line));
            return true;
        }

        @Override
        public Boolean getResult() {
            return changed;
        }

        private String passthrough(String line) {
            if (HEADER_PATTERN.matcher(line).find()) {
                handler = this::awaitingPre;
            }
            return line + "\n";
        }

        private String awaitingPre(String line) {
            Matcher m = PRE_OPEN_PATTERN.matcher(line);
            if (!m.find()) {
                return line + "\n";
            }
            needsSeparator = false;
            handler = this::preContent;
            return line.substring(0, m.end()) + preContent(line.substring(m.end()));
        }

        private String preContent(String line) {
            int close = line.indexOf(PRE_CLOSE_TAG);
            if (close >= 0) {
                handler = this::passthrough;
                return formatContent(line.substring(0, close)) + line.substring(close) + "\n";
            }
            return formatContent(line);
        }

        private String formatContent(String content) {
            if (content.isEmpty()) {
                return "";
            }

            Optional<String> result = formatLine(content);
            changed |= !result.equals(Optional.of(content));

            if (result.isEmpty()) {
                return "";
            }

            String prefix = needsSeparator ? "\n" : "";
            needsSeparator = true;
            return prefix + result.get();
        }
    }
}
