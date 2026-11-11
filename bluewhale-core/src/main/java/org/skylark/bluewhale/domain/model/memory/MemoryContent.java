package org.skylark.bluewhale.domain.model.memory;

import java.util.Map;

public class MemoryContent {
    private final String text;
    private final Map<String, Object> structuredData;

    public MemoryContent(String text, Map<String, Object> structuredData) {
        this.text = text;
        this.structuredData = structuredData;
    }

    public static MemoryContent ofText(String text) {
        return new MemoryContent(text, null);
    }

    public static MemoryContent ofStructured(String text, Map<String, Object> data) {
        return new MemoryContent(text, data);
    }

    public boolean hasStructuredData() {
        return structuredData != null && !structuredData.isEmpty();
    }

    public String getText() {
        return text;
    }

    public Map<String, Object> getStructuredData() {
        return structuredData;
    }
}
