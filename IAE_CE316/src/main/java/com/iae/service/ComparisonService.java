package com.iae.service;

public class ComparisonService {

    public boolean compare(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.replace("\r\n", "\n")
                   .replace("\r", "\n")
                   .trim();
    }

    public String generateDiff(String actual, String expected) {
        String[] actualLines = normalize(actual).split("\n", -1);
        String[] expectedLines = normalize(expected).split("\n", -1);

        StringBuilder diff = new StringBuilder();
        int maxLines = Math.max(actualLines.length, expectedLines.length);

        for (int i = 0; i < maxLines; i++) {
            String actualLine = i < actualLines.length ? actualLines[i] : "<missing>";
            String expectedLine = i < expectedLines.length ? expectedLines[i] : "<missing>";

            if (!actualLine.equals(expectedLine)) {
                diff.append("Line ").append(i + 1).append(":\n")
                    .append("  expected: '").append(expectedLine).append("'\n")
                    .append("  actual:   '").append(actualLine).append("'\n");
            }
        }

        if (diff.length() == 0) return "Outputs match.";
        return diff.toString();
    }
}
