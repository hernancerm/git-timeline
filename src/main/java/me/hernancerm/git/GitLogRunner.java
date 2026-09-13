package me.hernancerm.git;

import me.hernancerm.output.OutputSink;
import me.hernancerm.output.PagerSink;
import me.hernancerm.output.StdoutSink;
import me.hernancerm.shell.GitQuery;

import static me.hernancerm.git.CommitLineParser.splitCommitLine;
import static me.hernancerm.git.CommitLineParser.toCommit;
import static org.jline.jansi.Ansi.ansi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/** Runs git-log and writes every line of it, formatted, to the sink. */
public class GitLogRunner {

    // Dropped when the user picks the format, since the date belongs to the format.
    private static final String DATE_FORMAT = "--date=format:%b-%d-%Y";

    // How long git-log gets to exit once its output has run out or it has been destroyed.
    private static final int EXIT_TIMEOUT_MILLIS = 500;

    // What a shell reports for a process killed by SIGTERM (128 + 15), which is the signal
    // destroy() sends.
    private static final int SIGTERM_EXIT_CODE = 143;

    public int run(GitLogArgs args, BiFunction<GitCommit, GitRemote, String> commitFormatter)
            throws IOException, InterruptedException {

        // Launch the git lookups before anything reads them. Starting a query does not block,
        // so these run alongside each other and alongside git-log instead of one after the
        // other. Each is skipped when its value cannot be used.
        GitQuery remoteLookup =
                args.isColorEnabled() ? GitRemote.startLookup() : GitQuery.skipped();
        GitQuery corePagerLookup =
                args.isPagerEnabled() ? PagerSink.startCorePagerLookup() : GitQuery.skipped();

        ProcessBuilder processBuilder = new ProcessBuilder(getGitLogCommand(args));
        // Print stderr to the tty.
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = processBuilder.start();

        // Closing the sink is what starts the pager's interactive mode, so it has to happen
        // after the read loop is done.
        try (OutputSink sink = openSink(args, corePagerLookup)) {
            // Null when color is off, which leaves every hyperlink out of the output anyway.
            GitRemote gitRemote = GitRemote.parse(remoteLookup.firstLine());

            try (
                    var inputStreamReader = new InputStreamReader(process.getInputStream());
                    var bufferedReader = new BufferedReader(inputStreamReader)
            ) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {

                    // Fixes delay after user quits pager (e.g., press 'q' in less) on big repos.
                    if (!sink.isOpen()) {
                        process.destroy();
                        break;
                    }

                    String[] parts = splitCommitLine(line);
                    if (parts != null) {
                        // parts[0] holds the prefixes of the git-log option `--graph`.
                        // Example prefixes in this case: `* <commit>`, `| * <commit>`.
                        sink.println(ansi().render(parts[0]
                                + commitFormatter.apply(toCommit(parts), gitRemote)).toString());
                    } else {
                        // "Intermediate" line (no commit data) in git-log `--graph`. These are lines with
                        // just connectors, like `|\` or `|\|`. Anything else git-log emits that does not
                        // match the commit line format also lands here and is passed through untouched.
                        sink.println(ansi().render(line).toString());
                    }
                }
            }
        }

        return waitForExitCode(process);
    }

    // exitValue() throws while the process is still running, so the wait has to succeed
    // before asking. Only the destroy above can leave git-log running this long, and it
    // reports SIGTERM whether it obeys in time or has to be killed outright.
    static int waitForExitCode(Process process) throws InterruptedException {
        if (process.waitFor(EXIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            return process.exitValue();
        }

        process.destroyForcibly();
        return SIGTERM_EXIT_CODE;
    }

    private OutputSink openSink(GitLogArgs args, GitQuery corePagerLookup) throws IOException {
        if (!args.isPagerEnabled()) {
            return new StdoutSink();
        }
        return PagerSink.open(corePagerLookup);
    }

    static List<String> getGitLogCommand(GitLogArgs args) {
        List<String> command = new ArrayList<>(List.of(
                "git",
                "log",
                args.isColorEnabled() ? "--color=always" : "--color=never",
                "--pretty=format:" + CommitLineParser.PRETTY_FORMAT));

        // The date format is part of the line format, so a user supplied format has to take
        // the date with it. Forcing ours left `--pretty=medium` showing a date git-log would
        // never print there.
        if (!replacesTheFormat(args.unparsedArgs())) {
            command.add(DATE_FORMAT);
        }

        Arrays.stream(args.unparsedArgs())
                .map(CommitLineParser::stripDelimiter)
                .forEach(command::add);

        return command;
    }

    private static boolean replacesTheFormat(String[] unparsedArgs) {
        for (String arg : unparsedArgs) {
            // Everything past `--` is a path, so a file named like an option is not one.
            if (arg.equals("--")) {
                return false;
            }
            if (arg.equals("--pretty") || arg.startsWith("--pretty=")
                    || arg.equals("--format") || arg.startsWith("--format=")) {
                return true;
            }
        }
        return false;
    }
}
