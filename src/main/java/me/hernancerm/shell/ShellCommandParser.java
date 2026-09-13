package me.hernancerm.shell;

import java.util.ArrayList;
import java.util.List;

/** Splits a command string into argv, honoring single and double quotes. */
public class ShellCommandParser {

    private ShellCommandParser() {
    }

    public static List<String> parse(String command) {
        List<String> result = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (!inQuotes && (c == '\'' || c == '"')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
                quoteChar = 0;
            } else if (!inQuotes && Character.isWhitespace(c)) {
                if (!currentToken.isEmpty()) {
                    result.add(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c);
            }
        }

        if (!currentToken.isEmpty()) {
            result.add(currentToken.toString());
        }

        return result;
    }
}
