package me.hernancerm;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class ShellCommandParser {

    private ShellCommandParser() {
    }

    public static List<String> parse(String command) throws IOException {
        List<String> result = new ArrayList<>();

        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(command));

        tokenizer.resetSyntax();
        // Almost everything.
        tokenizer.wordChars('!', '~');
        // Spaces are delimiters.
        tokenizer.whitespaceChars(0, ' ');
//        // Quotation chars.
//        tokenizer.quoteChar('"');
//        tokenizer.quoteChar('\'');

        while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
            if (tokenizer.ttype == StreamTokenizer.TT_WORD || tokenizer.ttype == '"' || tokenizer.ttype == '\'') {
                result.add(tokenizer.sval);
            }
        }

        return result;
    }
}

        /*
import org.apache.commons.text.StringTokenizer;

import java.util.List;
import java.util.stream.Collectors;

public class CommandParser {
    public static void main(String[] args) {
        String command = "myprogram --opt='hello world' -xF";

        StringTokenizer tokenizer = new StringTokenizer(command, StringTokenizer.SEPARATORS);
        List<String> parts = tokenizer.getTokenList().stream()
                .map(String::trim)
                .collect(Collectors.toList());

        System.out.println(parts);
    }
}
         */
