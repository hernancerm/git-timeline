package me.hernancerm;

import java.util.ArrayList;
import java.util.List;

public class ShellCommandParser {

    private ShellCommandParser() {
    }

    // AI generated to pass all unit tests.
    public static List<String> parse(String command) {
        List<String> result = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (!inQuotes && (c == '\'' || c == '"')) {
                // Start of quoted string
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                // End of quoted string
                inQuotes = false;
                quoteChar = 0;
            } else if (!inQuotes && Character.isWhitespace(c)) {
                // Whitespace outside quotes - end current token
                if (!currentToken.isEmpty()) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                // Regular character or whitespace inside quotes
                currentToken.append(c);
            }
        }

        // Add final token if any
        if (!currentToken.isEmpty()) {
            result.add(currentToken.toString());
        }

        return result;
    }
}
