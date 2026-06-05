package id.naturalsmp.naturalSchool.ui.util;

import java.util.ArrayList;
import java.util.List;

public final class DialogFormatter {

    private DialogFormatter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Aligns a list of lines to the left by right-padding them to the maximum line length
     * and wrapping each line with a uniform font MiniMessage tag.
     *
     * @param rawLines the raw lines to format
     * @return the list of aligned and font-wrapped lines
     */
    public static List<String> alignLeft(List<String> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return new ArrayList<>();
        }

        int maxLength = 0;
        for (String line : rawLines) {
            if (line != null && line.length() > maxLength) {
                maxLength = line.length();
            }
        }

        List<String> formattedLines = new ArrayList<>(rawLines.size());
        for (String line : rawLines) {
            if (line == null) {
                line = "";
            }
            int diff = maxLength - line.length();
            StringBuilder sb = new StringBuilder(line);
            for (int i = 0; i < diff; i++) {
                sb.append(" ");
            }
            formattedLines.add(sb.toString());
        }

        return formattedLines;
    }
}
