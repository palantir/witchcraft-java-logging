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

import com.palantir.witchcraft.java.logging.format.LogFormatter;
import com.palantir.witchcraft.java.logging.format.LogParser;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Post-processes HTML test report files to apply witchcraft log formatting.
 */
final class HtmlReportPostProcessor {

    private static final LogParser<Optional<String>> PARSER = new LogParser<>(TestLogFilter.INSTANCE.combineWith(
            LogFormatter.INSTANCE, (include, formatted) -> include ? Optional.of(formatted) : Optional.empty()));

    private static final Pattern PRE_PATTERN = Pattern.compile("(<pre[^>]*>)(.*?)(</pre>)", Pattern.DOTALL);

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
        try {
            String content = Files.readString(htmlFile, StandardCharsets.UTF_8);
            String processed = processHtmlContent(content);

            if (!content.equals(processed)) {
                Files.writeString(htmlFile, processed, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    String processHtmlContent(String html) {
        Matcher matcher = PRE_PATTERN.matcher(html);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String replacement = matcher.group(1) + formatPreContent(matcher.group(2)) + matcher.group(3);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String formatPreContent(String content) {
        return Arrays.stream(content.split("\n"))
                .map(this::formatLine)
                .flatMap(Optional::stream)
                .collect(Collectors.joining("\n"));
    }

    /**
     * @return formatted line, empty if filtered out, or original if not a witchcraft log
     */
    private Optional<String> formatLine(String line) {
        String decoded = htmlDecode(line);

        return PARSER.tryParse(decoded)
                .map(opt -> opt.map(HtmlReportPostProcessor::htmlEncode))
                .orElse(Optional.of(line));
    }

    private static String htmlDecode(String html) {
        return html.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'");
    }

    private static String htmlEncode(String text) {
        return text.chars()
                .mapToObj(c -> switch (c) {
                    case '&' -> "&amp;";
                    case '<' -> "&lt;";
                    case '>' -> "&gt;";
                    case '"' -> "&quot;";
                    case '\'' -> "&#39;";
                    default -> String.valueOf((char) c);
                })
                .collect(Collectors.joining());
    }
}
