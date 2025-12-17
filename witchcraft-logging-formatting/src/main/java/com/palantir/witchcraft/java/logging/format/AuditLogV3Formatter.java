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

import com.palantir.witchcraft.api.logging.AuditLogV3;
import java.time.format.DateTimeFormatter;

final class AuditLogV3Formatter {
    private AuditLogV3Formatter() {}

    static String format(AuditLogV3 audit) {
        return Formatting.withStringBuilder(buffer -> {
            buffer.append('[');
            DateTimeFormatter.ISO_INSTANT.formatTo(audit.getTime(), buffer);
            buffer.append("] AUDIT.3: ").append(audit.getName());
            buffer.append(" (result: ").append(audit.getResult()).append(')');

            // Categories
            if (!audit.getCategories().isEmpty()) {
                buffer.append(" (categories: ").append(audit.getCategories()).append(')');
            }

            if (!audit.getRequestFields().isEmpty()) {
                buffer.append("\n  requestFields: ")
                        .append(Formatting.prettyPrintJson(audit.getRequestFields())
                                .replace("\n", "\n  "));
            }

            if (!audit.getResultFields().isEmpty()) {
                buffer.append("\n  resultFields: ")
                        .append(Formatting.prettyPrintJson(audit.getResultFields())
                                .replace("\n", "\n  "));
            }
        });
    }
}
