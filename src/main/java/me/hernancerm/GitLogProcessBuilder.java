package me.hernancerm;

import static org.jline.jansi.Ansi.ansi;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GitLogProcessBuilder {

    // Field delimiter.
    // No field placed before the subject line can contain it.
    // - Ref names (%d) reject every ASCII control char, so they cannot hold the 0x1F.
    // - Ident names (%an, %cn) are cut by git at the first '<', so they cannot hold the '<'.
    // - Hashes (%H, %h, %p) are hex digits and spaces.
    // The subject line (%s) can hold it, so goes last.
    static final String DELIMITER = "\u001F<";

    private static final Pattern DELIMITER_PATTERN = Pattern.compile(Pattern.quote(DELIMITER));

    // The delimiter as spelled in a git-log `--pretty=format:` string. %x1f is the 0x1F byte.
    private static final String DELIMITER_FORMAT = "%x1f<";

    private static final String PRETTY_FORMAT = String.join(DELIMITER_FORMAT,
            // The leading delimiter closes the prefix added by the git-log option `--graph`.
            "",
            "%H", "%h", "%p", "%C(auto)%d", "%cn", "%an", "%ad", "%s");

    // The `--graph` prefix plus the eight fields of PRETTY_FORMAT.
    private static final int PART_COUNT = 9;

    // Git remote url in any form git-clone accepts, including the scp-like `host:owner/repo`.
    // The `.git` suffix is optional: `git remote add origin https://github.com/o/r` is valid.
    // Capture groups: 1:Host, 2:Owner, 3:Repository.
    // https://git-scm.com/docs/git-clone#_git_urls
    private static final Pattern REMOTE_URL = Pattern.compile(
            "^(?:(?:ssh|git|https?|ftps?)://)?(?:[^@/]+@)?([^/:]+)(?::\\d+)?[:/](.+)/([^/]+?)(?:[.]git)?/?$");

    public int start(GitLogArgs args, Function<GitCommit, String> commitFormatter)
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

        Process pagerProcess;
        PrintWriter pagerWriter;
        if (args.isPagerEnabled()) {
            ProcessBuilder pagerProcessBuilder =
                    new ProcessBuilder(getPagerCommand(gitCorePagerProcess));
            pagerProcessBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pagerProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            pagerProcess = pagerProcessBuilder.start();
            pagerWriter = new PrintWriter(new BufferedOutputStream(
                            pagerProcess.getOutputStream()), false);
        } else {
            pagerProcess = null;
            pagerWriter = null;
        }

        // Stdout.
        try (
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String line;
            GitCommit commit = new GitCommit();
            // Null when color is off, which leaves every hyperlink out of the output anyway.
            GitRemote gitRemote = parseRemoteUrl(
                    readFirstLine(gitRemoteProcess, "remote url for: origin"));
            while ((line = bufferedReader.readLine()) != null) {

                // Fixes delay after user quits pager (e.g., press 'q' in less) on big repos.
                if (args.isPagerEnabled()) {
                    if (pagerProcess == null) {
                        throw new IllegalStateException(
                                "The pager process must not be null when the pager is enabled");
                    }
                    if (!pagerProcess.isAlive()) {
                        // Pager has terminated, kill the git-log process.
                        process.destroy();
                        break;
                    }
                }

                String[] parts = splitCommitLine(line);
                if (parts != null) {
                    populateCommit(parts, commit);
                    commit.setRemote(gitRemote);
                    commit.setArgs(args);
                    // parts[0] holds the prefixes of the git-log option `--graph`.
                    // Example prefixes in this case: `* <commit>`, `| * <commit>`.
                    println(args, pagerWriter, ansi().render(
                            parts[0] + commitFormatter.apply(commit)).toString());
                    commit.reset();
                } else {
                    // "Intermediate" line (no commit data) in git-log `--graph`. These are lines with
                    // just connectors, like `|\` or `|\|`. Anything else git-log emits that does not
                    // match PRETTY_FORMAT also lands here and is passed through untouched.
                    println(args, pagerWriter, ansi().render(line).toString());
                }
            }
        }

        if (args.isPagerEnabled()) {
            if (pagerWriter == null) {
                throw new IllegalStateException(
                        "The pager writer must not be null when the pager is enabled");
            }
            // Close the writer to signal EOF to the pager (less). Starts interactive mode.
            pagerWriter.close();
            // Wait for the pager (less) to finish (interactive mode).
            pagerProcess.waitFor();
        }

        process.waitFor(500, TimeUnit.MILLISECONDS);
        return process.exitValue();
    }

    private void println(GitLogArgs args, PrintWriter pagerWriter, String line) {
        if (args.isPagerEnabled()) {
            if (pagerWriter == null) {
                throw new IllegalStateException(
                        "The pager writer must not be null when the pager is enabled");
            }
            pagerWriter.println(line);
            pagerWriter.flush();
        } else {
            System.out.println(line);
        }
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

    // Returns null when there is no remote, the url is not a git url (e.g. a local path) or the
    // host is unsupported. Only the hyperlinks are lost, the rest of the output is unaffected.
    static GitRemote parseRemoteUrl(String originUrl) {
        if (originUrl == null || originUrl.isEmpty()) {
            return null;
        }

        Matcher matcher = REMOTE_URL.matcher(originUrl);
        if (!matcher.matches()) {
            return null;
        }

        GitRemote.Platform platform = GitRemote.Platform.from(matcher.group(1));
        if (platform == null) {
            return null;
        }

        return new GitRemote(platform, matcher.group(3), matcher.group(2));
    }

    static String[] splitCommitLine(String line) {
        String[] parts = DELIMITER_PATTERN.split(line, PART_COUNT);
        return parts.length == PART_COUNT ? parts : null;
    }

    static void populateCommit(String[] parts, GitCommit commit) {
        commit.setFullHash(parts[1]);
        commit.setAbbreviatedHash(parts[2]);
        commit.setAbbreviatedParentHashes(parts[3].split("\\s"));
        commit.setRefNamesColored(parts[4]);
        commit.setCommitterName(parts[5]);
        commit.setAuthorName(parts[6]);
        commit.setAuthorDate(parts[7]);
        commit.setSubjectLine(parts[8]);
    }

    private List<String> getGitLogCommand(GitLogArgs args) {
        return Stream.concat(Stream.of(
                        "git",
                        "log",
                        args.isColorEnabled() ? "--color=always" : "--color=never",
                        "--date=format:%b-%d-%Y",
                        "--pretty=format:" + PRETTY_FORMAT),
                // Drop the delimiter's control byte from pass-through args. Without this a user
                // supplied format, e.g. `--date=format:`, could inject a field boundary.
                Arrays.stream(args.unparsedArgs())
                        .map(arg -> arg.replace("\u001F", ""))).toList();
    }
}
