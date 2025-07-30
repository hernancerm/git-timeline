package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GitLogProcessBuilder {

    // Items.
    private static final String ITEM_ABBREVIATED_HASH = "abbreviated-hash";
    private static final String ITEM_ABBREVIATED_PARENT_HASHES = "abbreviated-parent-hashes";
    private static final String ITEM_AUTHOR_NAME = "author-name";
    private static final String ITEM_AUTHOR_DATE = "author-date";
    private static final String ITEM_COMMITTER_NAME = "committer-name";
    private static final String ITEM_SUBJECT_LINE = "subject-line";
    private static final String ITEM_REF_NAMES_COLORED = "ref-names-colored";

    // Capture groups: 1:Item, 2:Value.
    private static final String REGEX_TAG =
            "<hernancerm[.]git-timeline[.]([a-z-]+)>(.*?)</hernancerm[.]git-timeline[.]\\1>";

    public int start(String[] args, Function<GitCommit, String> commitFormatter)
            throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(getGitLogCommand(args));
        // Print stderr to the tty.
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = processBuilder.start();

        // TODO: Support ${PAGER} env var.
        ProcessBuilder pagerProcessBuilder = new ProcessBuilder("less", "-RXF");
        // Print stderr to the tty.
        pagerProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        // Print stdout to the tty.
        pagerProcessBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process lessProcess = pagerProcessBuilder.start();

        // TODO: Support new option `--no-pager`.
        PrintStream writer = new PrintStream(new BufferedOutputStream(lessProcess.getOutputStream()));

        // Stdout.
        try (
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String line;
            GitCommit commit = new GitCommit();
            Pattern pattern = Pattern.compile(REGEX_TAG);
            GitRemote gitRemote = getGitRemote();
            while ((line = bufferedReader.readLine()) != null) {
                int startIndex;
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    startIndex = matcher.start();
                    do {
                        populateCommitAttribute(matcher.group(1), matcher.group(2), commit);
                    } while (matcher.find());
                    commit.setGitRemote(gitRemote);
                    // Substring is needed to account for the prefixes of the git-log option `--graph`.
                    // Example prefixes in this case: `* <commit>`, `| * <commit>`.
                    writer.println(ansi().render(
                            line.substring(0, startIndex) + commitFormatter.apply(commit)));
                    commit.reset();
                } else {
                    // "Intermediate" line (no commit data) in git-log `--graph`. These are lines with
                    // just connectors, like `|\` or `|\|`.
                    writer.println(ansi().render(line));
                }
            }
        }

        // Close the writer to signal EOF to the pager (less). Starts interactive mode.
        writer.close();
        // Wait indefinitely for the pager (less) to finish (interactive mode).
        lessProcess.waitFor();

        process.waitFor(500, TimeUnit.MILLISECONDS);
        return process.exitValue();
    }

    private GitRemote getGitRemote() {
        GitRemote gitRemote = new GitRemote();
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
                // TODO: Work even when there is no supported remote or no remote at all.
                throw new IllegalStateException(
                        "Unsupported Git remote protocol. Must be one of: HTTPS, SSH");
            }
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
            case ITEM_ABBREVIATED_HASH:
                commit.setAbbreviatedHash(attributeValue);
                break;
            case ITEM_ABBREVIATED_PARENT_HASHES:
                commit.setAbbreviatedParentHashes(attributeValue.split("\\s"));
                break;
            case ITEM_AUTHOR_NAME:
                commit.setAuthorName(attributeValue);
                break;
            case ITEM_AUTHOR_DATE:
                commit.setAuthorDate(attributeValue);
                break;
            case ITEM_COMMITTER_NAME:
                commit.setCommitterName(attributeValue);
                break;
            case ITEM_SUBJECT_LINE:
                commit.setSubjectLine(attributeValue);
                break;
            case ITEM_REF_NAMES_COLORED:
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
                "<hernancerm.git-timeline.author-name>",
                    "%an",
                "</hernancerm.git-timeline.author-name>",
                "<hernancerm.git-timeline.committer-name>",
                    "%cn",
                "</hernancerm.git-timeline.committer-name>",
                "<hernancerm.git-timeline.author-date>",
                    "%ad",
                "</hernancerm.git-timeline.author-date>",
                "<hernancerm.git-timeline.subject-line>",
                    "%s",
                "</hernancerm.git-timeline.subject-line>",
                "<hernancerm.git-timeline.ref-names-colored>",
                    "%C(auto)%d",
                "</hernancerm.git-timeline.ref-names-colored>");

        return Stream.concat(Stream.of(
                        "git",
                        "log",
                        "--color=always",
                        "--date=format:%b/%d/%Y",
                        "--pretty=" + prettyFormat),
                Arrays.stream(args)).toList();
    }
}
