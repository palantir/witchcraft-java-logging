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

/** Executable class which prints several types of log lines for local verification. */
public final class Demonstration {

    public static void main(String[] _args) {
        System.out.println(EVENT_JSON);
        System.out.println(REQUEST_JSON);
        System.out.println(METRIC_JSON);
        System.out.println(TRACE_JSON);
        System.out.println(service("TRACE"));
        System.out.println(service("DEBUG"));
        System.out.println(service("INFO"));
        System.out.println(service("WARN"));
        System.out.println(service("ERROR"));
        System.out.println(service("FATAL"));
    }

    private static String service(String level) {
        return String.format(
                "{\"type\":\"service.1\",\"level\":\"%s\","
                        + "\"time\":\"2021-08-17T14:09:48.664971Z\",\"origin\":\"com.palantir.Demonstration\","
                        + "\"thread\":\"main\",\"message\":\"Hello, World!\"}",
                level);
    }

    private static final String EVENT_JSON = "{\"type\":\"event.2\",\"time\":\"2021-08-17T14:09:48.664971Z\","
            + "\"eventName\":\"com.palantir.witchcraft.jvm.crash\","
            + "\"values\":{\"numJvmErrorLogs\":\"1\"},\"unsafeParams\":{},\"tags\":{}}";

    private static final String REQUEST_JSON = "{\"type\":\"request.2\",\"time\":\"2021-08-17T14:09:48.664971Z\","
            + "\"method\":\"GET\",\"protocol\":\"HTTP/1.1\",\"path\":\"/api/sleep/{millis}\","
            + "\"params\":{\"host\":\"localhost:8443\",\"connection\":\"Keep-Alive\","
            + "\"accept-encoding\":\"gzip\",\"user-agent\":\"okhttp/3.13.1\"},"
            + "\"status\":503,\"requestSize\":0,\"responseSize\":78,\"duration\":1935,"
            + "\"traceId\":\"ba3200b6eb01999a\",\"unsafeParams\":{\"path\":\"/api/sleep/10\","
            + "\"millis\":\"10\"}}";

    private static final String METRIC_JSON = "{\"type\": \"metric.1\","
            + "\"time\":\"2021-08-17T14:09:48.664971Z\","
            + "\"metricName\":\"jvm.heap\",\"metricType\":\"gauge\",\"values\":{\"size\":66274352},"
            + "\"tags\":{\"collection\":\"Metaspace\",\"collector\":\"PS Scavenge\","
            + "\"when\":\"after\"},\"unsafeParams\":{}}";

    private static final String TRACE_JSON = "{\"type\":\"trace.1\",\"time\":\"2021-08-17T14:09:48.664971Z\","
            + "\"unsafeParams\":{},\"span\":{\"traceId\":\"2250486695021e19\",\"id\":\"c11b9a31555b7035\","
            + "\"name\":\"config-reload\",\"timestamp\":1558716040949000,\"duration\":618,"
            + "\"annotations\":[{\"timestamp\":1558716040949000,\"value\":\"lc\","
            + "\"endpoint\":{\"serviceName\":\"my-service\",\"ipv4\":\"127.0.0.1\"}}]}}";

    private Demonstration() {}
}
