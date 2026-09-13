package me.hernancerm;

import static org.junit.jupiter.api.Assertions.*;

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
}
