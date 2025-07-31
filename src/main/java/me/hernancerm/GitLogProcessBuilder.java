package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

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

    // Items.
    private static final String ABBREVIATED_HASH_ITEM = "abbreviated-hash";
    private static final String ABBREVIATED_PARENT_HASHES_ITEM = "abbreviated-parent-hashes";
    private static final String REF_NAMES_COLORED_ITEM = "ref-names-colored";
    private static final String COMMITTER_NAME_ITEM = "committer-name";
    private static final String AUTHOR_NAME_ITEM = "author-name";
    private static final String AUTHOR_DATE_ITEM = "author-date";
    private static final String SUBJECT_LINE_ITEM = "subject-line";

    // Capture groups: 1:Item, 2:Value.
    private static final String TAG_REGEX =
            "<hernancerm[.]git-timeline[.]([a-z-]+)>(.*?)</hernancerm[.]git-timeline[.]\\1>";

    public int start(GitLogArgs args, Function<GitCommit, String> commitFormatter)
            throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(getGitLogCommand(args.getUnparsedArgs()));
        // Print stderr to the tty.
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = processBuilder.start();

        // TODO: Support ${PAGER} env var.
        //   https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringTokenizer.html
        //   https://docs.oracle.com/javase/8/docs/api/java/io/StreamTokenizer.html

        Process pagerProcess;
        PrintWriter pagerWriter;
        if (args.isPagerEnabled()) {
            ProcessBuilder pagerProcessBuilder = new ProcessBuilder("less", "-RXFM");
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
            Pattern pattern = Pattern.compile(TAG_REGEX);
            GitRemote gitRemote = getGitRemote();
            while ((line = bufferedReader.readLine()) != null) {
                // Check if pager process has terminated (user pressed 'q')
                if (args.isPagerEnabled() && pagerProcess != null && !pagerProcess.isAlive()) {
                    // Pager has terminated, kill the git log process immediately
                    process.destroyForcibly();
                    break;
                }

                int startIndex;
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    startIndex = matcher.start();
                    do {
                        populateCommitAttribute(matcher.group(1), matcher.group(2), commit);
                    } while (matcher.find());
                    commit.setRemote(gitRemote);
                    // Substring is needed to account for the prefixes of the git-log option `--graph`.
                    // Example prefixes in this case: `* <commit>`, `| * <commit>`.
                    if (!println(args, pagerWriter, ansi().render(
                            line.substring(0, startIndex) + commitFormatter.apply(commit)).toString())) {
                        // Failed to write to pager (likely because it was closed), terminate git process
                        process.destroyForcibly();
                        break;
                    }
                    commit.reset();
                } else {
                    // "Intermediate" line (no commit data) in git-log `--graph`. These are lines with
                    // just connectors, like `|\` or `|\|`.
                    if (!println(args, pagerWriter, ansi().render(line).toString())) {
                        // Failed to write to pager (likely because it was closed), terminate git process
                        process.destroyForcibly();
                        break;
                    }
                }
            }
        }

        if (args.isPagerEnabled()) {
            if (pagerWriter == null) {
                throw new IllegalStateException(
                        "The pager writer must not be null when the pager is enabled");
            }
            pagerWriter.flush();
            // Close the writer to signal EOF to the pager (less). Starts interactive mode.
            pagerWriter.close();
            // Wait for the pager (less) to finish (interactive mode).
            pagerProcess.waitFor();
        }

        // Ensure git process is terminated
        if (process.isAlive()) {
            process.destroyForcibly();
        }
        process.waitFor(500, TimeUnit.MILLISECONDS);
        return process.exitValue();
    }

    private boolean println(GitLogArgs args, PrintWriter pagerWriter, String line) {
        if (args.isPagerEnabled()) {
            if (pagerWriter == null) {
                throw new IllegalStateException(
                        "The pager writer must not be null when the pager is enabled");
            }
            try {
                pagerWriter.println(line);
                // Check if the writer encountered an error (e.g., broken pipe)
                return !pagerWriter.checkError();
            } catch (Exception e) {
                // Writing failed, likely because pager was closed
                return false;
            }
        } else {
            System.out.println(line);
            return true;
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

            gitRemote = new GitRemote();
            gitRemote.setPlatform(GitRemote.Platform.toEnum(matcher.group(1)));
            gitRemote.setRepositoryName(matcher.group(3));
            gitRemote.setOwnerName(matcher.group(2));

        } catch (IOException e) {
            throw new RuntimeException(
                    "Error reading remote url for: origin",
                    e);
        }

        return gitRemote;
    }

    private void populateCommitAttribute(
            String serializedAttributeName,
            String attributeValue,
            GitCommit commit
    ) {
        switch (serializedAttributeName) {
            case ABBREVIATED_HASH_ITEM:
                commit.setAbbreviatedHash(attributeValue);
                break;
            case ABBREVIATED_PARENT_HASHES_ITEM:
                commit.setAbbreviatedParentHashes(attributeValue.split("\\s"));
                break;
            case AUTHOR_NAME_ITEM:
                commit.setAuthorName(attributeValue);
                break;
            case AUTHOR_DATE_ITEM:
                commit.setAuthorDate(attributeValue);
                break;
            case COMMITTER_NAME_ITEM:
                commit.setCommitterName(attributeValue);
                break;
            case SUBJECT_LINE_ITEM:
                commit.setSubjectLine(attributeValue);
                break;
            case REF_NAMES_COLORED_ITEM:
                commit.setRefNamesColored(attributeValue);
                break;
        }
    }

    private List<String> getGitLogCommand(String[] args) {

        final String prettyFormat = String.join("",
                "<hernancerm.git-timeline.full-hash>",
                    "%H",
                "</hernancerm.git-timeline.full-hash>",
                "<hernancerm.git-timeline.abbreviated-hash>",
                    "%h",
                "</hernancerm.git-timeline.abbreviated-hash>",
                "<hernancerm.git-timeline.abbreviated-parent-hashes>",
                    "%p",
                "</hernancerm.git-timeline.abbreviated-parent-hashes>",
                "<hernancerm.git-timeline.ref-names-colored>",
                    "%C(auto)%d",
                "</hernancerm.git-timeline.ref-names-colored>",
                "<hernancerm.git-timeline.committer-name>",
                    "%cn",
                "</hernancerm.git-timeline.committer-name>",
                "<hernancerm.git-timeline.author-name>",
                    "%an",
                "</hernancerm.git-timeline.author-name>",
                "<hernancerm.git-timeline.author-date>",
                    "%ad",
                "</hernancerm.git-timeline.author-date>",
                "<hernancerm.git-timeline.subject-line>",
                    "%s",
                "</hernancerm.git-timeline.subject-line>");

        return Stream.concat(Stream.of(
                        "git",
                        "log",
                        "--color=always",
                        "--date=format:%b/%d/%Y",
                        "--pretty=" + prettyFormat),
                Arrays.stream(args)).toList();
    }
}
