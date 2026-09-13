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

        // GIT_PAGER wins over core.pager, so the subprocess is only worth it when it is unset.
        if (isSet(env.apply("GIT_PAGER"))) {
            return GitQuery.skipped();
        }

        // 'delta' reads its own opts from the `[delta]` section of `.gitconfig`, so none here.

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

    // Precedence: https://git-scm.com/docs/git-var#Documentation/git-var.txt-GITPAGER
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

    // An unset variable and an empty one both name no pager.
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
        // EOF is what starts the pager's interactive mode.
        writer.close();
        process.waitFor();
    }
}
