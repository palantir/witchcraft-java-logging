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

import static com.palantir.gradle.testing.assertion.GradlePluginTestAssertions.assertThat;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class TestReportFormattingPluginIntegrationTest {
    private static final String SIMPLE_TEST_CLASS = """
        package com.palantir;
        import org.junit.Test;

        public final class SimpleTest {
            private static final String SERVICE_JSON = "{\\"type\\":\\"service.1\\",\\"level\\":\\"ERROR\\","
                + "\\"time\\":\\"2019-05-09T15:32:37.692Z\\",\\"origin\\":\\"ROOT\\","
                + "\\"thread\\":\\"main\\",\\"message\\":\\"test good {}\\","
                + "\\"params\\":{\\"good\\":\\":-)\\"},\\"unsafeParams\\":{},\\"tags\\":{}}";

            private static final String REQUEST_JSON = "{\\"type\\":\\"request.2\\",\\"time\\":\\"2019-05-24T12:40:36.703-04:00\\","
                + "\\"method\\":\\"GET\\",\\"protocol\\":\\"HTTP/1.1\\",\\"path\\":\\"/api/sleep/{millis}\\","
                + "\\"params\\":{\\"host\\":\\"localhost:8443\\",\\"connection\\":\\"Keep-Alive\\","
                + "\\"accept-encoding\\":\\"gzip\\",\\"user-agent\\":\\"okhttp/3.13.1\\"},"
                + "\\"status\\":503,\\"requestSize\\":0,\\"responseSize\\":78,\\"duration\\":1935,"
                + "\\"traceId\\":\\"ba3200b6eb01999a\\",\\"unsafeParams\\":{\\"path\\":\\"/api/sleep/10\\","
                + "\\"millis\\":\\"10\\"}}";

            private static final String METRIC_JSON = "{\\"type\\": \\"metric.1\\","
                + "\\"time\\":\\"2019-05-24T16:40:52.162Z\\","
                + "\\"metricName\\":\\"jvm.heap\\",\\"metricType\\":\\"gauge\\",\\"values\\":{\\"size\\":66274352},"
                + "\\"tags\\":{\\"collection\\":\\"Metaspace\\",\\"collector\\":\\"PS Scavenge\\","
                + "\\"when\\":\\"after\\"},\\"unsafeParams\\":{}}";

            @Test
            public void simpleTest() {
                System.out.println("==Service==");
                System.out.println(SERVICE_JSON);
                System.out.println("==Request==");
                System.out.println(REQUEST_JSON);
                System.out.println("==Metric==");
                System.out.println(METRIC_JSON);
                throw new AssertionError("==Done==");
            }
        }
        """;

    @Test
    void formats_test_report_stdout_and_stderr(GradleInvoker gradle, RootProject rootProject) {
        rootProject
                .buildGradle()
                .plugins()
                .add("java")
                .add("java-library")
                .add("com.palantir.witchcraft-logging-testreport");

        rootProject.buildGradle().append("""
            repositories {
                mavenCentral()
            }

            test {
                reports {
                    junitXml.required = true
                    html.required = true
                }
            }

            dependencies {
                testImplementation 'junit:junit:4.13.2'
            }

            sourceCompatibility = 11
            """);

        rootProject.testSourceSet().java().writeClass(SIMPLE_TEST_CLASS);

        gradle.withArgs("compileTestJava").buildsSuccessfully();

        InvocationResult testResult = gradle.withArgs("test").buildsWithFailure();

        assertThat(testResult).task(":compileTestJava").upToDate();

        rootProject
                .buildDir()
                .file("reports/tests/test/classes/com.palantir.SimpleTest.html")
                .assertThat()
                .content()
                .contains("==Service==")
                .doesNotContain("service.1")
                .contains("ERROR [2019-05-09T15:32:37.692Z] [main] ROOT: test good {} (good: :-))")
                .contains("==Request==")
                .doesNotContain("request.2")
                .contains("GET /api/sleep/10")
                .contains("==Metric==")
                .as("metric logging should be filtered out entirely")
                .doesNotContain("Scavenge")
                .contains("==Done==");
    }
}
