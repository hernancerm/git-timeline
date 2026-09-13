package me.hernancerm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class GitLogRunnerTest {

    @Test
    void waitForExitCode_givenProcessThatExits_thenReturnItsCode() throws Exception {
        Process process = new ProcessBuilder("sh", "-c", "exit 3").start();

        assertEquals(3, GitLogRunner.waitForExitCode(process));
    }

    @Test
    void waitForExitCode_givenProcessThatOutlivesTheWait_thenReportSigterm() throws Exception {
        // Regression: exitValue() was asked while the process was still running, which throws.
        // Quitting the pager on a big repo is what gets git-log here.
        Process process = new ProcessBuilder("sleep", "30").start();

        assertEquals(143, GitLogRunner.waitForExitCode(process));
        assertTrue(process.waitFor(5, TimeUnit.SECONDS), "Expected the process to be killed");
    }

    private static final String DATE_FORMAT = "--date=format:%b-%d-%Y";

    private static List<String> commandFor(String... unparsedArgs) {
        return GitLogRunner.getGitLogCommand(new GitLogArgs(unparsedArgs, false, false));
    }

    @Test
    void getGitLogCommand_givenNoFormatOption_thenSetTheDateFormat() {
        assertTrue(commandFor("-n", "5").contains(DATE_FORMAT));
    }

    @Test
    void getGitLogCommand_givenAFormatOption_thenLeaveTheDateToGitLog() {
        // Regression: the forced date leaked into every format that prints one, so
        // `--pretty=medium` showed a date git-log would never print there.
        String[][] formatOptions = {
                {"--pretty=medium"},
                {"--pretty", "medium"},
                {"--format=%h %ad"},
                {"--format", "%h %ad"},
        };

        for (String[] args : formatOptions) {
            assertFalse(commandFor(args).contains(DATE_FORMAT), String.join(" ", args));
        }
    }

    @Test
    void getGitLogCommand_givenAPathThatLooksLikeAFormatOption_thenSetTheDateFormat() {
        assertTrue(commandFor("--", "--pretty=medium").contains(DATE_FORMAT));
    }

    @Test
    void getGitLogCommand_givenAnyArgs_thenPassThemAfterItsOwn() {
        List<String> command = commandFor("--pretty=medium", "-n", "5");

        assertEquals(List.of("--pretty=medium", "-n", "5"),
                command.subList(command.size() - 3, command.size()));
    }
}
