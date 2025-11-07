package com.example.event_app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Utility helpers for loading CSV-based test data from resources. */
public final class TestDataLoader {

    private TestDataLoader() {}

    public static Map<String, String> loadRecord(String resourcePath, String key) throws IOException {
        Map<String, Map<String, String>> all = loadAllRecords(resourcePath);
        return all.getOrDefault(key, Collections.emptyMap());
    }

    public static Map<String, Map<String, String>> loadAllRecords(String resourcePath) throws IOException {
        InputStream stream = TestDataLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IOException("Missing test resource: " + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return Collections.emptyMap();
            }

            String[] headers = headerLine.split(",");
            Map<String, Map<String, String>> results = new LinkedHashMap<>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] values = line.split(",", -1);
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i].trim(), stripQuotes(values[i].trim()));
                }
                if (!row.isEmpty()) {
                    String key = stripQuotes(values[0].trim());
                    results.put(key, row);
                }
            }
            return results;
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
