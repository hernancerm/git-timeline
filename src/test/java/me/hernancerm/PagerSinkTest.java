package me.hernancerm;

import static me.hernancerm.PagerSink.resolveCommand;
import static me.hernancerm.PagerSink.startCorePagerLookup;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

class PagerSinkTest {

    private static final String NO_CORE_PAGER = null;

    /** An environment holding the given variables and nothing else. */
    private static UnaryOperator<String> env(String... namesAndValues) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            values.put(namesAndValues[i], namesAndValues[i + 1]);
        }
        return values::get;
    }

    @Test
    void resolveCommand_givenEverySource_thenGitPagerEnvWins() {
        List<String> command = resolveCommand("delta",
                env("GIT_PAGER", "less -X", "PAGER", "more"));

        assertEquals(List.of("less", "-X"), command);
    }

    @Test
    void resolveCommand_givenNoGitPagerEnv_thenCorePagerWins() {
        List<String> command = resolveCommand("delta", env("PAGER", "more"));

        assertEquals(List.of("delta"), command);
    }

    @Test
    void resolveCommand_givenOnlyPagerEnv_thenUseIt() {
        List<String> command = resolveCommand(NO_CORE_PAGER, env("PAGER", "more"));

        assertEquals(List.of("more"), command);
    }

    @Test
    void resolveCommand_givenNoSource_thenFallBackToLess() {
        List<String> command = resolveCommand(NO_CORE_PAGER, env());

        assertEquals(List.of("less", "-RXFM"), command);
    }

    @Test
    void resolveCommand_givenEmptyValues_thenSkipThem() {
        // An exported but empty variable names no pager, so it must not win over the next source.
        List<String> command = resolveCommand("", env("GIT_PAGER", "", "PAGER", "more"));

        assertEquals(List.of("more"), command);
    }

    @Test
    void resolveCommand_givenQuotedArgument_thenKeepItWhole() {
        List<String> command = resolveCommand(NO_CORE_PAGER,
                env("GIT_PAGER", "less '-p one two'"));

        assertEquals(List.of("less", "-p one two"), command);
    }

    @Test
    void startCorePagerLookup_givenGitPagerEnv_thenSkipTheSubprocess() {
        // core.pager cannot win, so the subprocess is not worth starting.
        assertNull(startCorePagerLookup(env("GIT_PAGER", "delta")).firstLine());
    }
}
