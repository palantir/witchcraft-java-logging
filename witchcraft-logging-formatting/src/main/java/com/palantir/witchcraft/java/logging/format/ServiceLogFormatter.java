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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.palantir.witchcraft.api.logging.ServiceLogV1;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.helpers.MessageFormatter;

final class ServiceLogFormatter {
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{(\\S+?)}");

    private ServiceLogFormatter() {}

    static String format(ServiceLogV1 service) {
        return Formatting.withStringBuilder(buffer -> {
            buffer.append(service.getLevel());
            while (buffer.length() < 6) {
                buffer.append(' ');
            }
            Map<String, Object> mergedParameters = new LinkedHashMap<>(
                    service.getParams().size() + service.getUnsafeParams().size());
            mergedParameters.putAll(service.getParams());
            mergedParameters.putAll(service.getUnsafeParams());
            buffer.append('[');
            DateTimeFormatter.ISO_INSTANT.formatTo(service.getTime(), buffer);
            buffer.append("] ");
            Optional<String> maybeThread = service.getThread();
            if (maybeThread.isPresent()) {
                buffer.append('[').append(maybeThread.get()).append("] ");
            }
            buffer.append(service.getOrigin().orElse("<nil>"))
                    .append(": ")
                    .append(getMessage(service.getMessage(), mergedParameters));
            Optional<String> maybeStackTrace = getStackTrace(service, mergedParameters);
            if (!mergedParameters.isEmpty()) {
                buffer.append(" (");
                Formatting.formatParamsTo(mergedParameters, buffer);
                // Reset trailing separator
                buffer.setLength(buffer.length() - 2);
                buffer.append(')');
            }
            maybeStackTrace.ifPresent(stackTrace -> buffer.append('\n').append(stackTrace));
        });
    }

    private static Optional<String> getStackTrace(
            ServiceLogV1 service, /* mutable */ Map<String, Object> mergedParameters) {
        return service.getStacktrace()
                .map(input -> Strings.emptyToNull(Formatting.NEWLINE_MATCHER.trimFrom(input)))
                .map(input -> interpolateParameters(input, mergedParameters::remove));
    }

    private static String getMessage(String formatString, /* mutable */ Map<String, Object> mergedParameters) {
        // If placeholders are found, attempt slf4j-style interpolation
        int placeholders = countPlaceholders(formatString);
        if (placeholders > 0) {
            Object[] parameters = new Object[placeholders];
            for (int i = 0; i < placeholders; i++) {
                // Use the placeholder string by default to avoid modifying non-existent parameters.
                // This can occur if only some parameters are wrapped with log-safe args.
                parameters[i] = MoreObjects.firstNonNull(mergedParameters.remove("" + i), "{}");
            }
            // Use the slf4j provided utility directly
            return MessageFormatter.arrayFormat(formatString, parameters).getMessage();
        }
        return formatString;
    }

    private static int countPlaceholders(String formatString) {
        int count = 0;
        for (int i = 1; i < formatString.length(); i++) {
            if (formatString.charAt(i - 1) == '{' && formatString.charAt(i) == '}') {
                count++;
            }
        }
        return count;
    }

    private static String interpolateParameters(String original, Function<String, Object> lookup) {
        Matcher matcher = PARAMETER_PATTERN.matcher(original);
        String current = original;
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = lookup.apply(name);
            if (value != null) {
                current = current.replace("{" + name + "}", Formatting.safeString(value));
            }
        }
        return current;
    }
}
