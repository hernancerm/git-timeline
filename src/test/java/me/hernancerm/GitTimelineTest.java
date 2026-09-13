package me.hernancerm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitTimelineTest {

    private static final boolean TERMINAL = true;
    private static final boolean NOT_TERMINAL = false;

    private GitTimeline gitTimeline;

    @BeforeEach
    void setUp() {
        gitTimeline = new GitTimeline(new String[0], new GitLogProcessBuilder());
    }

    private GitLogArgs parse(boolean isTerminal, String... args) {
        return gitTimeline.parseArgs(args, isTerminal);
    }

    @Test
    void parseArgs_givenTerminal_thenEnableColorAndPager() {
        GitLogArgs args = parse(TERMINAL);

        assertTrue(args.isColorEnabled());
        assertTrue(args.isPagerEnabled());
    }

    @Test
    void parseArgs_givenNotATerminal_thenDisableColorAndPager() {
        // Piping or redirecting should not need `--no-color`.
        GitLogArgs args = parse(NOT_TERMINAL);

        assertFalse(args.isColorEnabled());
        assertFalse(args.isPagerEnabled());
    }

    @Test
    void parseArgs_givenColorAlways_thenEnableColorEvenWhenNotATerminal() {
        assertTrue(parse(NOT_TERMINAL, "--color=always").isColorEnabled());
        assertTrue(parse(NOT_TERMINAL, "--color").isColorEnabled());
    }

    @Test
    void parseArgs_givenColorNever_thenDisableColorEvenOnATerminal() {
        assertFalse(parse(TERMINAL, "--color=never").isColorEnabled());
        assertFalse(parse(TERMINAL, "--no-color").isColorEnabled());
    }

    @Test
    void parseArgs_givenColorAuto_thenFollowTheTerminal() {
        assertTrue(parse(TERMINAL, "--color=auto").isColorEnabled());
        assertFalse(parse(NOT_TERMINAL, "--color=auto").isColorEnabled());
    }

    @Test
    void parseArgs_givenNoPager_thenDisablePagerOnATerminal() {
        assertFalse(parse(TERMINAL, "--no-pager").isPagerEnabled());
    }

    @Test
    void parseArgs_givenColorOption_thenDoNotPassItOnToGitLog() {
        // The color of git-log is set from isColorEnabled, so forwarding these would be redundant.
        assertArrayEquals(new String[0], parse(TERMINAL, "--color=never").unparsedArgs());
        assertArrayEquals(new String[0], parse(TERMINAL, "--no-color").unparsedArgs());
        assertArrayEquals(new String[0], parse(TERMINAL, "--no-pager").unparsedArgs());
    }

    @Test
    void parseArgs_givenOptionsOfGitLog_thenPassThemOnUntouched() {
        GitLogArgs args = parse(TERMINAL, "-n5", "--graph", "--author=me");

        assertArrayEquals(new String[]{"-n5", "--graph", "--author=me"}, args.unparsedArgs());
    }
}
