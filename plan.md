# Plan: Streaming support for large HTML test reports

## Problem

`HtmlReportPostProcessor` loads entire HTML files into memory with `Files.readString()` (line 61) and processes them with a regex over the full content. For multi-GiB test reports (tests that produce enormous stdout), this causes OOM or extreme memory pressure in the Gradle daemon.

The chain of in-memory operations:
1. `Files.readString(htmlFile)` — full file as a single `String`
2. `OUTPUT_SECTION_PATTERN.matcher(content)` — regex with `DOTALL` scans the entire string
3. `StringBuilder result` in `processHtmlContent` — builds a second copy of the file
4. `formatPreContent` collects all lines with `Collectors.joining("\n")` — third copy of the `<pre>` section

Peak memory is roughly **3x the file size** for a single file.

## Approach: line-by-line streaming with a state machine

Replace the regex-over-whole-file approach with a streaming state machine that reads and writes one line at a time. This bounds memory to O(single line) regardless of file size.

### State machine

Gradle's HTML report structure for stdout/stderr sections follows this pattern:
```html
<h2>Standard output</h2>
<span class="...">
<pre>...log lines...</pre>
```

States:
- **PASSTHROUGH** — default; copy lines verbatim to output
- **SAW_HEADER** — just saw an `<h2>standard output</h2>` or `<h2>standard error</h2>` line; looking for `<pre`
- **IN_PRE** — inside a `<pre>` block within a stdout/stderr section; process each line through the log parser

Transitions:
- PASSTHROUGH + line matches `<h2>standard (output|error)</h2>` → SAW_HEADER, write line
- SAW_HEADER + line contains `<pre` → IN_PRE, write everything up to and including the `<pre...>` tag, begin processing content
- IN_PRE + line contains `</pre>` → PASSTHROUGH, process content before `</pre>`, write `</pre>` and anything after
- IN_PRE + normal line → format through `LogParser`/`TestLogFilter`, write result

### File I/O

- Read with `BufferedReader` (8KB default buffer)
- Write to a temp file in the same directory with `BufferedWriter`
- Track whether any content was actually changed
- If changed: atomically replace original via `Files.move(..., REPLACE_EXISTING)`
- If unchanged: delete temp file (avoids unnecessary writes and preserves timestamps)

## Implementation steps

### Step 1: Add unit tests for `HtmlReportPostProcessor`

Create `HtmlReportPostProcessorTest.java` with direct unit tests (not Gradle integration tests) to get fast feedback:

- **`processes_small_report_correctly`** — synthetic HTML with known log lines, assert formatted output matches expected (validates parity with current behavior)
- **`preserves_non_log_content`** — HTML with no witchcraft logs passes through unchanged
- **`filters_metric_and_trace_logs`** — verify TestLogFilter is applied
- **`handles_multiple_pre_sections`** — stdout and stderr sections in one file
- **`handles_pre_tag_on_same_line_as_header`** — edge case where `<h2>` and `<pre>` are on one line

These tests pin current behavior before the refactor.

### Step 2: Add large-file test

- **`streams_large_report_without_loading_into_memory`** — generate a synthetic HTML file with a large `<pre>` section (e.g. 100K repeated log lines; ~50MB). Process it and assert:
  1. Output is correct (spot-check first/last lines and filtered lines)
  2. Processing completes without error

  This test won't be multi-GiB (that would be too slow for CI), but it validates the streaming path works correctly on non-trivial sizes. The real multi-GiB guarantee comes from the architecture (line-by-line streaming), not from a brute-force test.

### Step 3: Refactor `HtmlReportPostProcessor` to streaming

Replace `processHtmlFile` internals:

```java
private void processHtmlFile(Path htmlFile) {
    Path tempFile = htmlFile.resolveSibling(htmlFile.getFileName() + ".tmp");
    boolean changed = false;
    try (BufferedReader reader = Files.newBufferedReader(htmlFile, StandardCharsets.UTF_8);
         BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {

        State state = State.PASSTHROUGH;
        String line;
        while ((line = reader.readLine()) != null) {
            // state machine logic; write processed lines to writer
            // set changed=true when a line is modified
        }
    }
    if (changed) {
        Files.move(tempFile, htmlFile, StandardCopyOption.REPLACE_EXISTING);
    } else {
        Files.deleteIfExists(tempFile);
    }
}
```

Remove `processHtmlContent` and `formatPreContent` (the stream-processing replaces them). Keep `formatLine` as-is since it handles a single line.

### Step 4: Verify all tests pass

- Run existing integration test `TestReportFormattingPluginIntegrationTest` to ensure behavioral parity
- Run new unit tests
- Run the large-file test

### Step 5: Clean up

- Remove the `Writable` interface if it's still unused after the refactor (it already looks unused)
- Remove any dead `processHtmlContent`/`formatPreContent` methods

## Risks and edge cases

- **`<pre>` tag and content on the same line**: Gradle's report generator may put the opening `<pre>` tag and initial content on the same line. The state machine must handle this by splitting at the tag boundary.
- **`</pre>` mid-line**: Similarly, `</pre>` may appear after content on the same line. Must process content before the closing tag.
- **Multi-line HTML attributes on `<pre>` tags**: Unlikely in Gradle reports but worth handling defensively — treat any line containing `<pre` followed by `>` as entering IN_PRE.
- **No trailing newline**: `BufferedReader.readLine()` strips newlines; must re-add `\n` when writing (except possibly the last line). Or use a flag to track.
- **Behavioral parity**: The regex approach with `DOTALL` matches across lines within a `<pre>` block. The line-by-line approach inherently handles this since witchcraft log lines are single-line JSON — they never span multiple lines in practice.
- **Empty `<pre>` sections**: Handle gracefully (no lines to process).

## Files to modify

- `gradle-witchcraft-logging/src/main/java/.../testreport/HtmlReportPostProcessor.java` — rewrite to streaming
- `gradle-witchcraft-logging/src/test/java/.../testreport/HtmlReportPostProcessorTest.java` — new unit test file
- `gradle-witchcraft-logging/src/main/java/.../testreport/Writable.java` — delete if confirmed unused
