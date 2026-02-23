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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Post-processes HTML test report files to apply witchcraft log formatting.
 * Streams line-by-line to support arbitrarily large files in constant memory.
 */
final class HtmlReportPostProcessor {

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
        Path tmp = htmlFile.resolveSibling(htmlFile.getFileName() + ".tmp");
        try (BufferedReader reader = Files.newBufferedReader(htmlFile, StandardCharsets.UTF_8);
                BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            StreamState state = new StreamState(writer);
            reader.lines().forEach(state::processLine);
            Files.move(tmp, htmlFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
