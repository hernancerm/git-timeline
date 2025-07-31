package me.hernancerm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ShellCommandParserTest {

    @Test
    void parse_emptyCommand() {
        assertEquals(
                List.of(),
                ShellCommandParser.parse(""));
    }

    @Test
    void parse_bareCommand() {
        assertEquals(
                List.of("less"),
                ShellCommandParser.parse("less"));
    }

    @Test
    void parse_booleanOptions() {
        assertEquals(
                List.of("less", "--incsearch", "-iSR", "+INPUT_LINE_NUMBER"),
                ShellCommandParser.parse("less --incsearch -iSR +INPUT_LINE_NUMBER"));
    }

    @Test
    void parse_valueOptions() {
        assertEquals(
                List.of("less", "-b", "2", "--jump-target=20"),
                ShellCommandParser.parse("less -b 2 --jump-target=20"));
    }

    @Test
    void parse_valueOptionWithSpaces_singleQuotes() {
        assertEquals(
                List.of("less", "-b", "2", "--log-file=/Users/john/my log file.log", "--pattern=a b c"),
                ShellCommandParser.parse("less -b 2 --log-file='/Users/john/my log file.log' --pattern='a b c'"));
    }

    @Test
    void parse_valueOptionWithSpaces_doubleQuotes() {
        assertEquals(
                List.of("less", "-b", "2", "--log-file=/Users/john/my log file.log", "--pattern=a b c"),
                ShellCommandParser.parse("less -b 2 --log-file=\"/Users/john/my log file.log\" --pattern=\"a b c\""));
    }

    @Test
    void parse_valueOptionWithSpaces_mixedSingleAndDoubleQuotes() {
        assertEquals(
                List.of("less", "-b", "2", "--log-file=/Users/john/my log file.log", "--pattern=a b c"),
                ShellCommandParser.parse("less -b 2 --log-file='/Users/john/my log file.log' --pattern=\"a b c\""));
    }
}
