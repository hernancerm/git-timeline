package me.hernancerm;

import static me.hernancerm.CommitLineParser.splitCommitLine;
import static me.hernancerm.CommitLineParser.toCommit;
import static org.jline.jansi.Ansi.ansi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class GitLogProcessBuilder {

    public int start(GitLogArgs args, BiFunction<GitCommit, GitRemote, String> commitFormatter)
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

        process.waitFor(500, TimeUnit.MILLISECONDS);
        return process.exitValue();
    }

    private OutputSink openSink(GitLogArgs args, GitQuery corePagerLookup) throws IOException {
        if (!args.isPagerEnabled()) {
            return new StdoutSink();
        }
        return PagerSink.open(corePagerLookup);
    }

    private List<String> getGitLogCommand(GitLogArgs args) {
        return Stream.concat(Stream.of(
                        "git",
                        "log",
                        args.isColorEnabled() ? "--color=always" : "--color=never",
                        "--date=format:%b-%d-%Y",
                        "--pretty=format:" + CommitLineParser.PRETTY_FORMAT),
                Arrays.stream(args.unparsedArgs())
                        .map(CommitLineParser::stripDelimiter)).toList();
    }
}
