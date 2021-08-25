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

package com.palantir.witchcraft.java.logging.idea;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.palantir.witchcraft.api.logging.LogLevel;
import java.util.Map;

/** {@link ConsoleViewContentType} definitions used by this plugin. */
final class WitchcraftConsoleViewContentTypes {

    static final ConsoleViewContentType DEFAULT_SERVICE_TYPE =
            createViewType("WITCHCRAFT_DEFAULT", ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    static final Map<LogLevel, ConsoleViewContentType> SERVICE_TYPES = Map.of(
            LogLevel.FATAL,
            createViewType("WITCHCRAFT_SERVICE_FATAL", ConsoleViewContentType.ERROR_OUTPUT_KEY),
            LogLevel.ERROR,
            createViewType("WITCHCRAFT_SERVICE_ERROR", ConsoleViewContentType.ERROR_OUTPUT_KEY),
            LogLevel.WARN,
            createViewType("WITCHCRAFT_SERVICE_WARN", ConsoleViewContentType.LOG_WARNING_OUTPUT_KEY),
            LogLevel.INFO,
            createViewType("WITCHCRAFT_SERVICE_INFO", ConsoleViewContentType.LOG_INFO_OUTPUT_KEY),
            LogLevel.DEBUG,
            createViewType("WITCHCRAFT_SERVICE_DEBUG", ConsoleViewContentType.LOG_DEBUG_OUTPUT_KEY),
            LogLevel.TRACE,
            createViewType("WITCHCRAFT_SERVICE_TRACE", ConsoleViewContentType.LOG_VERBOSE_OUTPUT_KEY));
    static final ConsoleViewContentType EVENT_TYPE =
            createViewType("WITCHCRAFT_EVENT", ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    static final ConsoleViewContentType METRIC_TYPE =
            createViewType("WITCHCRAFT_METRIC", ConsoleViewContentType.LOG_EXPIRED_ENTRY);
    static final ConsoleViewContentType REQUEST_TYPE =
            createViewType("WITCHCRAFT_REQUEST", ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    static final ConsoleViewContentType TRACE_TYPE =
            createViewType("WITCHCRAFT_TRACE", ConsoleViewContentType.LOG_EXPIRED_ENTRY);

    private WitchcraftConsoleViewContentTypes() {}

    private static ConsoleViewContentType createViewType(String name, TextAttributesKey fallback) {
        return new ConsoleViewContentType(name, TextAttributesKey.createTextAttributesKey(name, fallback));
    }
}
