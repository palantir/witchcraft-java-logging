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

import com.palantir.witchcraft.api.logging.DiagnosticLogV1;
import com.palantir.witchcraft.api.logging.MetricLogV1;
import com.palantir.witchcraft.api.logging.TraceLogV1;
import com.palantir.witchcraft.java.logging.format.LogVisitor;
import java.util.Optional;

/**
 * Filtering {@link LogVisitor} implementation with reasonable defaults for tests.
 */
enum TestLogFilter implements LogVisitor<Boolean> {
    INSTANCE;

    private static final Optional<Boolean> ALLOW = Optional.of(Boolean.TRUE);
    private static final Optional<Boolean> BLOCK = Optional.of(Boolean.FALSE);

    @Override
    public Optional<Boolean> metricV1(MetricLogV1 _metricLogV1) {
        return BLOCK;
    }

    @Override
    public Optional<Boolean> traceV1(TraceLogV1 _traceLogV1) {
        return BLOCK;
    }

    @Override
    public Optional<Boolean> diagnosticV1(DiagnosticLogV1 _diagnosticLogV1) {
        return BLOCK;
    }

    @Override
    public Optional<Boolean> defaultValue() {
        return ALLOW;
    }
}
