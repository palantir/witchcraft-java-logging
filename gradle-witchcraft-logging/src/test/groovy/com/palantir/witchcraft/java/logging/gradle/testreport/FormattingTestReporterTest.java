/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
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

import com.palantir.witchcraft.java.logging.gradle.testreport.FormattingTestReporter.LineProcessingWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormattingTestReporterTest {
    @Test
    void testWriteLines() throws Exception {
        StringWriter delegate = new StringWriter();
        List<String> lines = new ArrayList<>();
        try (LineProcessingWriter lineWriter = new LineProcessingWriter(delegate, (line, writer) -> {
            assertThat(writer).isSameAs(delegate);
            assertThat(line).isNotNull();
            lines.add(line);
            delegate.write(line);
        })) {
            assertThat(delegate.toString()).isEmpty();
            lineWriter.write("Hello ");
            assertThat(delegate.toString()).isEmpty();
            lineWriter.write("world".toCharArray());
            assertThat(delegate.toString()).isEmpty();
            lineWriter.write("!\nThis is");
            assertThat(delegate.toString()).isEqualTo("Hello world!\n");
            lineWriter.write("");
            assertThat(delegate.toString()).isEqualTo("Hello world!\n");
            lineWriter.write(" a test\n of the\nemer".toCharArray());
            assertThat(delegate.toString()).isEqualTo("Hello world!\nThis is a test\n of the\n");
            lineWriter.write(new char[0]);
            assertThat(delegate.toString()).isEqualTo("Hello world!\nThis is a test\n of the\n");
            lineWriter.append("gency broadcasting system.");
            assertThat(delegate.toString()).isEqualTo("Hello world!\nThis is a test\n of the\n");
            lineWriter.flush();
            assertThat(delegate.toString()).isEqualTo("Hello world!\nThis is a test\n of the\n");
        }
        assertThat(delegate.toString())
                .isEqualTo("Hello world!\nThis is a test\n of the\nemergency broadcasting system.");
        assertThat(lines)
                .hasSize(4)
                .containsExactly("Hello world!\n", "This is a test\n", " of the\n", "emergency broadcasting system.");
    }
}
