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

    public int start(GitLogArgs args, Function<GitCommit, String> commitFormatter)
            throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(getGitLogCommand(args.unparsedArgs()));
        // Print stderr to the tty.
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = processBuilder.start();

        Process pagerProcess;
        PrintWriter pagerWriter;
        if (args.isPagerEnabled()) {
            ProcessBuilder pagerProcessBuilder = new ProcessBuilder(getPagerCommand());
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
            GitRemote gitRemote = getGitRemote();
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
    private List<String> getPagerCommand() {

        String gitPagerCommand = System.getenv("GIT_PAGER");
        if (gitPagerCommand != null && !gitPagerCommand.isEmpty()) {
            return ShellCommandParser.parse(gitPagerCommand);
        }

        String gitCorePagerCommand = getGitCorePagerCommand();
        if (gitCorePagerCommand != null && !gitCorePagerCommand.isEmpty()) {
            return ShellCommandParser.parse(gitCorePagerCommand);
        }

        String pagerCommand = System.getenv("PAGER");
        if (pagerCommand != null && !pagerCommand.isEmpty()) {
            return ShellCommandParser.parse(pagerCommand);
        }

        return List.of("less", "-RXFM");
    }

    private String getGitCorePagerCommand() {
        Process process;

        // In the case of 'delta', the pager configuration is retrieved from the
        // file `.gitconfig` at user root from the section `[delta]`. No need to
        // read default 'delta' opts here.

        try {
            process = new ProcessBuilder("git", "config", "get", "core.pager").start();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error starting git process to get value 'core.pager'",
                    e);
        }

        try (
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            return bufferedReader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error reading git config value 'core.pager'",
                    e);
        }
    }

    private GitRemote getGitRemote() {
        GitRemote gitRemote;
        Process process;

        try {
            process = new ProcessBuilder("git", "remote", "get-url", "origin").start();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error starting git process to get remote url for: origin",
                    e);
        }

        try (
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            Matcher matcher;
            String originUrl = bufferedReader.readLine();

            if (originUrl == null || originUrl.isEmpty()) {
                // No Git remote url.
                return null;
            }

            if (originUrl.matches("^https.*$")) {
                // HTTPS.
                // The regex syntax `(?:X)` where `X` is a pattern defines a non-capturing
                // regex group: https://www.baeldung.com/java-regex-non-capturing-groups
                Pattern pattern = Pattern.compile("https://(?:.*?@)?(.*?)/(.*?)/(.*?)[.]git");
                matcher = pattern.matcher(originUrl);
                if (!matcher.find()) {
                    throw new IllegalStateException(
                            "Error matching the remote HTTPS url to extract its parts: "
                                    + originUrl);
                }
            } else if (originUrl.matches("^git@.*$")) {
                // SSH.
                Pattern pattern = Pattern.compile("git@(.*?):(.*?)/(.*?)[.]git");
                matcher = pattern.matcher(originUrl);
                if (!matcher.find()) {
                    throw new IllegalStateException(
                            "Error matching the remote SSH url to extract its parts: "
                                    + originUrl);
                }
            } else {
                // Unsupported Git remote protocol.
                return null;
            }

            gitRemote = new GitRemote(
                    GitRemote.Platform.from(matcher.group(1)),
                    matcher.group(3),
                    matcher.group(2));

        } catch (IOException e) {
            throw new RuntimeException(
                    "Error reading remote url for: origin",
                    e);
        }

        return gitRemote;
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

    private List<String> getGitLogCommand(String[] args) {
        return Stream.concat(Stream.of(
                        "git",
                        "log",
                        "--color=always",
                        "--date=format:%b-%d-%Y",
                        "--pretty=format:" + PRETTY_FORMAT),
                // Drop the delimiter's control byte from pass-through args. Without this a user
                // supplied format, e.g. `--date=format:`, could inject a field boundary.
                Arrays.stream(args).map(arg -> arg.replace("\u001F", ""))).toList();
    }
}
