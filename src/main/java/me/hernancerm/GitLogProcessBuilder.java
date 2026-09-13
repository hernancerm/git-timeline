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

        // Launch the git lookups before anything reads them. ProcessBuilder.start() does not
        // block, so these run alongside each other and alongside git-log instead of one after
        // the other. Each is skipped when its value cannot be used.
        Process gitRemoteProcess = args.isColorEnabled() ? startGitRemoteProcess() : null;
        Process gitCorePagerProcess =
                args.isPagerEnabled() && isGitPagerEnvUnset() ? startGitCorePagerProcess() : null;

        ProcessBuilder processBuilder = new ProcessBuilder(getGitLogCommand(args));
        // Print stderr to the tty.
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = processBuilder.start();

        // Closing the sink is what starts the pager's interactive mode, so it has to happen
        // after the read loop is done.
        try (OutputSink sink = openSink(args, gitCorePagerProcess)) {
            // Null when color is off, which leaves every hyperlink out of the output anyway.
            GitRemote gitRemote = GitRemote.parse(
                    readFirstLine(gitRemoteProcess, "remote url for: origin"));

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

    private OutputSink openSink(GitLogArgs args, Process gitCorePagerProcess) throws IOException {
        if (!args.isPagerEnabled()) {
            return new StdoutSink();
        }
        return PagerSink.start(getPagerCommand(gitCorePagerProcess));
    }

    // Documentation for precedence of pager command source:
    // https://git-scm.com/docs/git-var#Documentation/git-var.txt-GITPAGER
    private List<String> getPagerCommand(Process gitCorePagerProcess) {

        String gitPagerCommand = System.getenv("GIT_PAGER");
        if (gitPagerCommand != null && !gitPagerCommand.isEmpty()) {
            return ShellCommandParser.parse(gitPagerCommand);
        }

        String gitCorePagerCommand =
                readFirstLine(gitCorePagerProcess, "git config value 'core.pager'");
        if (gitCorePagerCommand != null && !gitCorePagerCommand.isEmpty()) {
            return ShellCommandParser.parse(gitCorePagerCommand);
        }

        String pagerCommand = System.getenv("PAGER");
        if (pagerCommand != null && !pagerCommand.isEmpty()) {
            return ShellCommandParser.parse(pagerCommand);
        }

        return List.of("less", "-RXFM");
    }

    // GIT_PAGER wins over core.pager, so asking git for core.pager is only worth a subprocess
    // when GIT_PAGER is unset.
    private static boolean isGitPagerEnvUnset() {
        String gitPagerCommand = System.getenv("GIT_PAGER");
        return gitPagerCommand == null || gitPagerCommand.isEmpty();
    }

    private static Process startGitCorePagerProcess() {

        // In the case of 'delta', the pager configuration is retrieved from the
        // file `.gitconfig` at user root from the section `[delta]`. No need to
        // read default 'delta' opts here.

        try {
            return new ProcessBuilder("git", "config", "get", "core.pager").start();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error starting git process to get value 'core.pager'",
                    e);
        }
    }

    private static Process startGitRemoteProcess() {
        try {
            return new ProcessBuilder("git", "remote", "get-url", "origin").start();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error starting git process to get remote url for: origin",
                    e);
        }
    }

    // Returns null when the process was never started, i.e. when its value is not needed.
    private static String readFirstLine(Process process, String what) {
        if (process == null) {
            return null;
        }

        try (
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            return bufferedReader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + what, e);
        }
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
