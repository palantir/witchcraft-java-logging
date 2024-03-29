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

import com.palantir.witchcraft.api.logging.RequestLogV2;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RequestLogFormatter {
    private RequestLogFormatter() {}

    private static final Pattern REQUEST_PARAMETER_PATTERN = Pattern.compile("\\{(\\S+?)}");
    // Common implementations often store the raw unsafe path in 'unsafeParams.path'.
    // We can use that path when present for a better debugging experience, especially
    // in cases tests fail due to unmatched paths where we'd otherwise see '/*'
    // or '(unmatched path)'
    private static final String UNSAFE_PATH_PARAMETER = "path";

    static String format(RequestLogV2 request) {
        return Formatting.withStringBuilder(buffer -> {
            buffer.append('[');
            DateTimeFormatter.ISO_INSTANT.formatTo(request.getTime(), buffer);
            buffer.append("] \"");

            Map<String, Object> mergedParameters = new LinkedHashMap<>(
                    request.getParams().size() + request.getUnsafeParams().size());
            mergedParameters.putAll(request.getParams());
            mergedParameters.putAll(request.getUnsafeParams());

            String path = getPathWithParameters(request, mergedParameters);

            request.getMethod().ifPresent(method -> buffer.append(method).append(' '));
            buffer.append(path)
                    .append(' ')
                    .append(request.getProtocol())
                    .append("\" ")
                    .append(request.getStatus())
                    .append(' ')
                    .append(request.getResponseSize().longValue())
                    .append(" bytes ")
                    .append(request.getDuration().longValue())
                    .append(" μs");

            if (!mergedParameters.isEmpty()) {
                buffer.append(' ');
                Formatting.niceMap(mergedParameters, buffer);
            }
        });
    }

    private static String getPathWithParameters(RequestLogV2 request, Map<String, Object> mergedParameters) {
        // First consume path params to avoid duplicate data on the formatted line
        String path = request.getPath();
        Matcher matcher = REQUEST_PARAMETER_PATTERN.matcher(path);
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = mergedParameters.remove(name);
            if (value != null) {
                path = path.replace("{" + name + "}", Formatting.safeString(value));
            }
        }

        Object maybeUnsafePath = request.getUnsafeParams().get(UNSAFE_PATH_PARAMETER);
        if (maybeUnsafePath instanceof String) {
            String unsafePath = (String) maybeUnsafePath;
            // Validate that this is a reasonable path value based on a leading slash.
            if (unsafePath.startsWith("/")) {
                // Remove the unsafe path attribute if it has been consumed
                mergedParameters.remove(UNSAFE_PATH_PARAMETER, unsafePath);
                return unsafePath;
            }
        }

        return path;
    }
}
