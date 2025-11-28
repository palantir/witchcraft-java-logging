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

import com.palantir.conjure.java.serialization.ObjectMappers;
import com.palantir.witchcraft.api.logging.AuditLogV3;
import com.palantir.witchcraft.api.logging.AuditProducer;
import com.palantir.witchcraft.api.logging.AuditResult;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditLogV3FormatterTest {
    private static final UUID TEST_EVENT_ID = UUID.fromString("12345678-1234-1234-1234-123456789abc");

    @Test
    void formats_with_request_and_result_fields() {
        String formatted = AuditLogV3Formatter.format(AuditLogV3.builder()
                .type("audit.3")
                .product("my-product")
                .productVersion("1.0.0")
                .producerType(AuditProducer.SERVER)
                .eventId(TEST_EVENT_ID)
                .time(TestData.XMAS_2019)
                .name("PUT_FILE")
                .result(AuditResult.SUCCESS)
                .requestFields("fileId", "file123")
                .requestFields("path", "/data/file.txt")
                .resultFields("bytes", 1024)
                .resultFields("status", "uploaded")
                .build());

        assertThat(formatted)
                .startsWith("[2019-12-25T01:02:03Z] AUDIT: PUT_FILE (result: SUCCESS)")
                .contains("requestFields:")
                .contains("\"fileId\" : \"file123\"")
                .contains("\"path\" : \"/data/file.txt\"")
                .contains("resultFields:")
                .contains("\"bytes\" : 1024")
                .contains("\"status\" : \"uploaded\"");
    }

    @Test
    void formats_with_categories() {
        String formatted = AuditLogV3Formatter.format(AuditLogV3.builder()
                .type("audit.3")
                .product("my-product")
                .productVersion("2.0.0")
                .producerType(AuditProducer.CLIENT)
                .eventId(TEST_EVENT_ID)
                .time(TestData.XMAS_2019)
                .name("DATA_ACCESS")
                .result(AuditResult.SUCCESS)
                .categories(List.of("DATASET_ACCESS", "FILE_READ"))
                .build());

        assertThat(formatted)
                .startsWith("[2019-12-25T01:02:03Z] AUDIT: DATA_ACCESS (result: SUCCESS)")
                .contains("categories: [DATASET_ACCESS, FILE_READ]");
    }

    @Test
    void formats_with_empty_fields() {
        String formatted = AuditLogV3Formatter.format(AuditLogV3.builder()
                .type("audit.3")
                .product("my-product")
                .productVersion("1.0.0")
                .producerType(AuditProducer.SERVER)
                .eventId(TEST_EVENT_ID)
                .time(TestData.XMAS_2019)
                .name("MINIMAL_ACTION")
                .result(AuditResult.SUCCESS)
                .build());

        assertThat(formatted)
                .startsWith("[2019-12-25T01:02:03Z] AUDIT: MINIMAL_ACTION (result: SUCCESS)")
                .doesNotContain("requestFields:")
                .doesNotContain("resultFields:");
    }

    @Test
    void formats_with_complex_nested_fields() {
        String formatted = AuditLogV3Formatter.format(AuditLogV3.builder()
                .type("audit.3")
                .product("my-product")
                .productVersion("1.0.0")
                .producerType(AuditProducer.SERVER)
                .eventId(TEST_EVENT_ID)
                .time(TestData.XMAS_2019)
                .name("COMPLEX_ACTION")
                .result(AuditResult.SUCCESS)
                .requestFields("metadata", Map.of("key1", "value1", "key2", 42))
                .resultFields("response", Map.of("success", true, "code", 200))
                .build());

        assertThat(formatted)
                .startsWith("[2019-12-25T01:02:03Z] AUDIT: COMPLEX_ACTION (result: SUCCESS)")
                .contains("requestFields:")
                .contains("resultFields:");
    }

    @Test
    void formats_example_audit_log() throws Exception {
        String exampleRawAuditLog = new String(
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("example_audit_v3_log.json")
                        .readAllBytes(),
                StandardCharsets.UTF_8);
        AuditLogV3 exampleAuditLog =
                ObjectMappers.newClientObjectMapper().readValue(exampleRawAuditLog, AuditLogV3.class);
        String formatted = AuditLogV3Formatter.format(exampleAuditLog);
        assertThat(formatted).isEqualToIgnoringWhitespace("""
            [2025-12-11T18:04:29.202693778Z] AUDIT: SOME_SERVICE_GET_RESOURCE_BY_PATH (result: SUCCESS) (categories: [dataSearch])
                      requestFields: {
                        "dataSearchQuery" : "/namespace-2656ede8-bb13-4b05-a99e-5f96d00ee084-67dff0/57ceda98-46c7-4bba-8590-f5fe4018f81e/",
                        "dataSearchContext" : [ ]
                      }
                      resultFields: {
                        "dataSearchResults" : [ {
                          "id" : {
                            "type" : "rid",
                            "rid" : "ri.main.folder.2b65ce71-dc9b-49d9-9c63-4cf4ab4c8e6f"
                          },
                          "context" : [ ]
                        } ]
                      }
            """);
    }
}
