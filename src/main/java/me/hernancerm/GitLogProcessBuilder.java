package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiMode;
import org.fusesource.jansi.AnsiPrintStream;

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
        Process process = new ProcessBuilder(getGitLogCommand(args)).start();

        // Stdout.
        try (
                AnsiPrintStream ansiPrintStream = AnsiConsole.out();
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String line;
            GitCommit commit = new GitCommit();
            setColorMode(ansiPrintStream, args);
            Pattern pattern = Pattern.compile(REGEX_TAG);
            while ((line = bufferedReader.readLine()) != null) {
                int startIndex;
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    startIndex = matcher.start();
                    do {
                        populateCommitAttribute(matcher.group(1), matcher.group(2), commit);
                    } while (matcher.find());
                    // Substring is needed to account for the prefixes of the git-log option `--graph`.
                    // Example prefixes in this case: `* <commit>`, `| * <commit>`.
                    ansiPrintStream.println(ansi().render(
                            line.substring(0, startIndex) + commitFormatter.apply(commit)));
                    commit.reset();
                } else {
                    // "Intermediate" line (no commit data) in git-log `--graph`. These are lines with
                    // just connectors, like `|\` or `|\|`.
                    ansiPrintStream.println(ansi().render(line));
                }
            }
        }

        // Stderr.
        try (
                AnsiPrintStream ansiPrintStream = AnsiConsole.err();
                var inputStreamReader = new InputStreamReader(process.getErrorStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                ansiPrintStream.println(line);
            }
        }

        process.waitFor(500, TimeUnit.MILLISECONDS);
        return process.exitValue();
    }

    private void setColorMode(AnsiPrintStream ansiPrintStream, String[] args) {
        for (String arg : args) {
            switch (arg) {
                case "--color=always":
                    ansiPrintStream.setMode(AnsiMode.Force);
                    break;
                case "--color=never":
                    ansiPrintStream.setMode(AnsiMode.Strip);
                    break;
                case "--color=auto":
                    ansiPrintStream.setMode(AnsiMode.Default);
                    break;
            }
        }
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
                        "--date=format:%d/%b/%Y",
                        "--pretty=" + prettyFormat),
                Arrays.stream(args)).toList();
    }
}
