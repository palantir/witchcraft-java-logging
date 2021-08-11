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

package com.palantir.witchcraft.java.logging.format;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.witchcraft.api.logging.LogLevel;
import com.palantir.witchcraft.api.logging.ServiceLogV1;
import org.junit.jupiter.api.Test;

class ServiceLogFormatterTest {
    @Test
    void formats_logline_correctly_1() {
        String formatted = ServiceLogFormatter.format(ServiceLogV1.builder()
                .type("service.1")
                .level(LogLevel.INFO)
                .time(TestData.XMAS_2019)
                .message("message {}")
                .origin("com.origin")
                .thread("thread-1")
                .params("param1", "value1")
                .unsafeParams("unsafeParam2", "value2")
                .stacktrace("java.lang.Exception: stacktrace")
                .build());

        assertThat(formatted)
                .isEqualTo("INFO  [2019-12-25T01:02:03Z] [thread-1] com.origin: message {} (param1: value1, "
                        + "unsafeParam2: value2)\njava.lang.Exception: stacktrace");
    }

    @Test
    void formats_logline_correctly_2() {
        String formatted = ServiceLogFormatter.format(ServiceLogV1.builder()
                .type("service.1")
                .level(LogLevel.ERROR)
                .time(TestData.XMAS_2019)
                .message("message {}")
                .params("param", "value")
                .unsafeParams("0", "inlined")
                .build());

        assertThat(formatted).isEqualTo("ERROR [2019-12-25T01:02:03Z] <nil>: message inlined (param: value)");
    }

    @Test
    void interpolates_stack_trace_parameters() {
        String formatted = ServiceLogFormatter.format(ServiceLogV1.builder()
                .type("service.1")
                .level(LogLevel.INFO)
                .time(TestData.XMAS_2019)
                .message("message")
                .origin("com.origin")
                .thread("thread-1")
                .unsafeParams("throwable0_message", "exception message")
                .stacktrace("java.lang.Exception: {throwable0_message}")
                .build());

        assertThat(formatted)
                .isEqualTo("INFO  [2019-12-25T01:02:03Z] [thread-1] com.origin: message\n"
                        + "java.lang.Exception: exception message");
    }
}
