package me.hernancerm.output;

import me.hernancerm.shell.GitQuery;
import me.hernancerm.shell.ShellCommandParser;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.UnaryOperator;

public class PagerSink implements OutputSink {

    private static final List<String> DEFAULT_COMMAND = List.of("less", "-RXFM");

    private final Process process;
    private final PrintWriter writer;

    private PagerSink(Process process) {
        this.process = process;
        this.writer = new PrintWriter(
                new BufferedOutputStream(process.getOutputStream()), false);
    }

    /** Starts the lookup of the git config value {@code core.pager}. */
    public static GitQuery startCorePagerLookup() {
        return startCorePagerLookup(System::getenv);
    }

    static GitQuery startCorePagerLookup(UnaryOperator<String> env) {

        // GIT_PAGER wins over core.pager, so asking git for core.pager is only worth a subprocess
        // when GIT_PAGER is unset.
        if (isSet(env.apply("GIT_PAGER"))) {
            return GitQuery.skipped();
        }

        // In the case of 'delta', the pager configuration is retrieved from the
        // file `.gitconfig` at user root from the section `[delta]`. No need to
        // read default 'delta' opts here.

        return GitQuery.start(
                "git config value 'core.pager'", "git", "config", "get", "core.pager");
    }

    /** Starts the pager and returns a sink that writes to it. */
    public static PagerSink open(GitQuery corePagerLookup) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                resolveCommand(corePagerLookup.firstLine(), System::getenv));
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return new PagerSink(processBuilder.start());
    }

    // Documentation for precedence of pager command source:
    // https://git-scm.com/docs/git-var#Documentation/git-var.txt-GITPAGER
    static List<String> resolveCommand(String corePagerCommand, UnaryOperator<String> env) {

        String gitPagerCommand = env.apply("GIT_PAGER");
        if (isSet(gitPagerCommand)) {
            return ShellCommandParser.parse(gitPagerCommand);
        }

        if (isSet(corePagerCommand)) {
            return ShellCommandParser.parse(corePagerCommand);
        }

        String pagerCommand = env.apply("PAGER");
        if (isSet(pagerCommand)) {
            return ShellCommandParser.parse(pagerCommand);
        }

        return DEFAULT_COMMAND;
    }

    // An unset variable and one set to the empty string are both "no pager named here".
    private static boolean isSet(String value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public void println(String line) {
        writer.println(line);
        writer.flush();
    }

    @Override
    public boolean isOpen() {
        return process.isAlive();
    }

    @Override
    public void close() throws InterruptedException {
        // Closing the writer signals EOF to the pager (less), which starts interactive mode.
        writer.close();
        process.waitFor();
    }
}
