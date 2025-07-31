package me.hernancerm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

class ShellCommandParserTest {

    @Test
    void parse_booleanOptions() throws IOException {
        assertEquals(
                List.of("less", "--incsearch", "-iSR", "+INPUT_LINE_NUMBER"),
                ShellCommandParser.parse("less --incsearch -iSR +INPUT_LINE_NUMBER"));
    }

    @Test
    void parse_valueOptions() throws IOException {
        assertEquals(
                List.of("less", "-b", "2", "--jump-target=20"),
                ShellCommandParser.parse("less -b 2 --jump-target=20"));
    }

    @Test
    void parse_valueOptionWithSpaces() throws IOException {
        assertEquals(
                List.of("less", "-b", "2", "--log-file=/Users/john/my log file.log", "--pattern=a b c"),
                ShellCommandParser.parse("less -b 2 --log-file='/Users/john/my log file.log' --pattern='a b c'"));
    }
}