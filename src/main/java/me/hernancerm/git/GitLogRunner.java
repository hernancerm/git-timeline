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

    // Dropped when the user picks the format, since the date is part of it.
    private static final String DATE_FORMAT = "--date=format:%b-%d-%Y";

    // How long git-log gets to exit on its own.
    private static final int EXIT_TIMEOUT_MILLIS = 500;

    // What a shell reports for a SIGTERM kill (128 + 15), the signal destroy() sends.
    private static final int SIGTERM_EXIT_CODE = 143;

    public int run(GitLogArgs args, BiFunction<GitCommit, GitRemote, String> commitFormatter)
            throws IOException, InterruptedException {

        // Starting a query does not block, so these run alongside each other and git-log.
        GitQuery remoteLookup =
                args.isColorEnabled() ? GitRemote.startLookup() : GitQuery.skipped();
        GitQuery corePagerLookup =
                args.isPagerEnabled() ? PagerSink.startCorePagerLookup() : GitQuery.skipped();

        ProcessBuilder processBuilder = new ProcessBuilder(getGitLogCommand(args));
        // Print stderr to the tty.
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = processBuilder.start();

        // Closing the sink starts the pager's interactive mode, so it waits for the read loop.
        try (OutputSink sink = openSink(args, corePagerLookup)) {
            // Null when color is off, which drops every hyperlink.
            GitRemote gitRemote = GitRemote.parse(remoteLookup.firstLine());

            try (
                    var inputStreamReader = new InputStreamReader(process.getInputStream());
                    var bufferedReader = new BufferedReader(inputStreamReader)
            ) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {

                    // Quitting the pager on a big repo would otherwise leave a delay.
                    if (!sink.isOpen()) {
                        process.destroy();
                        break;
                    }

                    String[] parts = splitCommitLine(line);
                    if (parts != null) {
                        // parts[0] is the `--graph` prefix, e.g. `* `, `| * `.
                        sink.println(ansi().render(parts[0]
                                + commitFormatter.apply(toCommit(parts), gitRemote)).toString());
                    } else {
                        // A `--graph` connector line like `|\`, or anything else git-log
                        // emits that is not a commit line. Passed through untouched.
                        sink.println(ansi().render(line).toString());
                    }
                }
            }
        }

        return waitForExitCode(process);
    }

    // exitValue() throws while the process runs, so the wait has to succeed first. Only a
    // destroy() leaves git-log running this long, which is a SIGTERM either way.
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

        // A user supplied format brings its own date. Forcing ours left `--pretty=medium`
        // showing a date git-log would never print.
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
