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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlReportPostProcessorTest {

    private static final String SERVICE_LOG =
            "{&quot;type&quot;:&quot;service.1&quot;,&quot;level&quot;:&quot;ERROR&quot;,"
                    + "&quot;time&quot;:&quot;2019-05-09T15:32:37.692Z&quot;,"
                    + "&quot;origin&quot;:&quot;ROOT&quot;,"
                    + "&quot;thread&quot;:&quot;main&quot;,"
                    + "&quot;message&quot;:&quot;test good {}&quot;,"
                    + "&quot;params&quot;:{&quot;good&quot;:&quot;:-)&quot;},"
                    + "&quot;unsafeParams&quot;:{},&quot;tags&quot;:{}}";

    private static final String METRIC_LOG = "{&quot;type&quot;: &quot;metric.1&quot;,"
            + "&quot;time&quot;:&quot;2019-05-24T16:40:52.162Z&quot;,"
            + "&quot;metricName&quot;:&quot;jvm.heap&quot;,"
            + "&quot;metricType&quot;:&quot;gauge&quot;,"
            + "&quot;values&quot;:{&quot;size&quot;:66274352},"
            + "&quot;tags&quot;:{&quot;collection&quot;:&quot;Metaspace&quot;},"
            + "&quot;unsafeParams&quot;:{}}";

    private static final String TRACE_LOG = "{&quot;type&quot;:&quot;trace.1&quot;,"
            + "&quot;time&quot;:&quot;2019-05-24T16:40:40.95Z&quot;,"
            + "&quot;unsafeParams&quot;:{},"
            + "&quot;span&quot;:{&quot;traceId&quot;:&quot;2250486695021e19&quot;,"
            + "&quot;id&quot;:&quot;c11b9a31555b7035&quot;,"
            + "&quot;name&quot;:&quot;config-reload&quot;,"
            + "&quot;timestamp&quot;:1558716040949000,"
            + "&quot;duration&quot;:618,"
            + "&quot;annotations&quot;:[{&quot;timestamp&quot;:1558716040949000,"
            + "&quot;value&quot;:&quot;lc&quot;,"
            + "&quot;endpoint&quot;:{&quot;serviceName&quot;:&quot;my-service&quot;,"
            + "&quot;ipv4&quot;:&quot;10.193.122.103&quot;}}]}}";

    private final HtmlReportPostProcessor processor = new HtmlReportPostProcessor();

    @TempDir
    Path tempDir;

    @Nested
    class SmallReports {

        @Test
        void processes_service_log_in_stdout_section() throws IOException {
            String html = wrapInHtml(stdoutSection(SERVICE_LOG));
            Path file = writeHtml("test.html", html);

            processor.processReportDirectory(tempDir.toFile());

            String result = Files.readString(file, StandardCharsets.UTF_8);
            assertThat(result)
                    .as("service log should be formatted")
                    .contains("ERROR [2019-05-09T15:32:37.692Z]")
                    .contains("test good {}")
                    .doesNotContain("service.1");
        }

        @Test
        void preserves_non_log_content() throws IOException {
            String html = wrapInHtml(stdoutSection("plain text output\nanother line"));
            Path file = writeHtml("test.html", html);

            processor.processReportDirectory(tempDir.toFile());

            String result = Files.readString(file, StandardCharsets.UTF_8);
            assertThat(result)
                    .as("non-log content should pass through unchanged")
                    .contains("plain text output")
                    .contains("another line");
        }

        @Test
        void filters_metric_logs() throws IOException {
            String html = wrapInHtml(stdoutSection("before\n" + METRIC_LOG + "\nafter"));
            Path file = writeHtml("test.html", html);

            processor.processReportDirectory(tempDir.toFile());

            String result = Files.readString(file, StandardCharsets.UTF_8);
            assertThat(result)
                    .as("metric logs should be filtered out")
                    .contains("before")
                    .contains("after")
                    .doesNotContain("metric.1")
                    .doesNotContain("jvm.heap");
        }

        @Test
        void filters_trace_logs() throws IOException {
            String html = wrapInHtml(stdoutSection("before\n" + TRACE_LOG + "\nafter"));
            Path file = writeHtml("test.html", html);

            processor.processReportDirectory(tempDir.toFile());

            String result = Files.readString(file, StandardCharsets.UTF_8);
            assertThat(result)
                    .as("trace logs should be filtered out")
                    .contains("before")
                    .contains("after")
                    .doesNotContain("trace.1");
        }

        @Test
        void handles_multiple_pre_sections() throws IOException {
            String html = wrapInHtml(
                    stdoutSection(SERVICE_LOG + "\nplain stdout") + stderrSection("stderr line\n" + SERVICE_LOG));
            Path file = writeHtml("test.html", html);

            processor.processReportDirectory(tempDir.toFile());

            String result = Files.readString(file, StandardCharsets.UTF_8);
            assertThat(result)
                    .as("both stdout and stderr sections should be processed")
                    .contains("plain stdout")
                    .contains("stderr line")
                    .doesNotContain("service.1");
        }

        @Test
        void does_not_modify_file_without_log_sections() throws IOException {
            String html = "<html><body><h2>Summary</h2><pre>not a log section</pre></body></html>";
            Path file = writeHtml("test.html", html);

            processor.processReportDirectory(tempDir.toFile());

            assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo(html);
        }

        @Test
        void handles_html_in_subdirectories() throws IOException {
            Path subDir = Files.createDirectories(tempDir.resolve("classes"));
            String html = wrapInHtml(stdoutSection(SERVICE_LOG));
            Path file = Files.writeString(subDir.resolve("test.html"), html, StandardCharsets.UTF_8);

            processor.processReportDirectory(tempDir.toFile());

            String result = Files.readString(file, StandardCharsets.UTF_8);
            assertThat(result)
                    .as("should process HTML files in subdirectories")
                    .contains("ERROR [2019-05-09T15:32:37.692Z]")
                    .doesNotContain("service.1");
        }
    }

    @Nested
    class LargeReports {

        @Test
        void streams_large_report_without_excessive_memory() throws IOException {
            // Write a large HTML file line-by-line to avoid OOM during test setup
            int lineCount = 10_000_000;
            Path file = tempDir.resolve("large-test.html");
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(wrapInHtmlOpen() + stdoutSectionOpen());
                for (int i = 0; i < lineCount; i++) {
                    if (i % 3 == 0) {
                        writer.write(SERVICE_LOG);
                    } else if (i % 3 == 1) {
                        writer.write(METRIC_LOG);
                    } else {
                        writer.write("plain line " + i);
                    }
                    writer.newLine();
                }
                writer.write(stdoutSectionClose() + wrapInHtmlClose());
            }

            long fileSizeMb = Files.size(file) / (1024 * 1024);
            assertThat(fileSizeMb).as("test file should be substantial").isGreaterThan(10);

            processor.processReportDirectory(tempDir.toFile());

            // Assert by streaming the result line-by-line to avoid OOM reading the processed file
            boolean foundFormattedService = false;
            boolean foundPlainLine = false;
            boolean containsRawServiceType = false;
            boolean containsRawMetricType = false;
            boolean containsMetricName = false;
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("ERROR [2019-05-09T15:32:37.692Z]")) {
                        foundFormattedService = true;
                    }
                    if (line.contains("plain line 2")) {
                        foundPlainLine = true;
                    }
                    if (line.contains("service.1")) {
                        containsRawServiceType = true;
                    }
                    if (line.contains("metric.1")) {
                        containsRawMetricType = true;
                    }
                    if (line.contains("jvm.heap")) {
                        containsMetricName = true;
                    }
                }
            }
            assertThat(foundFormattedService)
                    .as("service logs should be formatted")
                    .isTrue();
            assertThat(foundPlainLine).as("plain text should be preserved").isTrue();
            assertThat(containsRawServiceType)
                    .as("raw service.1 type should not appear")
                    .isFalse();
            assertThat(containsRawMetricType)
                    .as("metric logs should be filtered out")
                    .isFalse();
            assertThat(containsMetricName)
                    .as("metric content should be filtered out")
                    .isFalse();
        }
    }

    private Path writeHtml(String filename, String content) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private static String wrapInHtml(String body) {
        return "<html><head><title>Test</title></head><body>" + body + "</body></html>";
    }

    private static String stdoutSection(String content) {
        return stdoutSectionOpen() + content + stdoutSectionClose();
    }

    private static String stderrSection(String content) {
        return "<h2>Standard error</h2>\n<span class=\"code\">\n<pre>" + content + "</pre>\n</span>";
    }

    private static String wrapInHtmlOpen() {
        return "<html><head><title>Test</title></head><body>";
    }

    private static String wrapInHtmlClose() {
        return "</body></html>";
    }

    private static String stdoutSectionOpen() {
        return "<h2>Standard output</h2>\n<span class=\"code\">\n<pre>";
    }

    private static String stdoutSectionClose() {
        return "</pre>\n</span>";
    }
}
