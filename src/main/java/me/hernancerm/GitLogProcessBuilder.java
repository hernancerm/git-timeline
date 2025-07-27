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
import org.fusesource.jansi.AnsiPrintStream;

public class GitLogProcessBuilder {

    // Items.
    private static final String ITEM_ABBREVIATED_HASH = "ABBREVIATED_HASH";
    private static final String ITEM_ABBREVIATED_PARENT_HASHES = "ABBREVIATED_PARENT_HASHES";
    private static final String ITEM_AUTHOR_NAME = "AUTHOR_NAME";
    private static final String ITEM_AUTHOR_DATE = "AUTHOR_DATE";
    private static final String ITEM_COMMITTER_NAME = "COMMITTER_NAME";
    private static final String ITEM_SUBJECT_LINE = "SUBJECT_LINE";
    private static final String ITEM_REF_NAMES_COLORED = "REF_NAMES_COLORED";

    // Capture groups: 1:Item, 2:Value.
    private static final String REGEX_TAG =
            "<hernancerm[.]git-timeline[.]([A-Z_]+)>(.*?)</hernancerm[.]git-timeline[.]\\1>";

    public int start(String[] args, Function<GitCommit, String> formatter) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(getGitLogCommand(args)).start();

//        try (
//                var inputStreamReader = new InputStreamReader(process.getErrorStream());
//                var bufferedReader = new BufferedReader(inputStreamReader)
//        ) {
//            bufferedReader.lines().forEach(System.err::println);
//        }

        try (
                AnsiPrintStream ansiPrintStream = AnsiConsole.out();
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String line;
            GitCommit commit = new GitCommit();
            Pattern pattern = Pattern.compile(REGEX_TAG);
            while ((line = bufferedReader.readLine()) != null) {
                int startIndex;
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    startIndex = matcher.start();
                } else {
                    throw new IllegalStateException(
                            "No tag matched. At least one tag must be matched.");
                }
                do {
                    populateCommitAttribute(matcher.group(1), matcher.group(2), commit);
                } while (matcher.find());
                // Substring is needed to account for the git-log option `--graph`.
                ansiPrintStream.println(ansi().render(line.substring(0, startIndex) + formatter.apply(commit)));
                commit.reset();
            }
        }

        process.waitFor(500, TimeUnit.MILLISECONDS);
        return process.exitValue();
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
                "<hernancerm.git-timeline.FULL_HASH>",
                    "%H",
                "</hernancerm.git-timeline.FULL_HASH>",
                "<hernancerm.git-timeline.ABBREVIATED_HASH>",
                    "%h",
                "</hernancerm.git-timeline.ABBREVIATED_HASH>",
                "<hernancerm.git-timeline.ABBREVIATED_PARENT_HASHES>",
                    "%p",
                "</hernancerm.git-timeline.ABBREVIATED_PARENT_HASHES>",
                "<hernancerm.git-timeline.AUTHOR_NAME>",
                    "%an",
                "</hernancerm.git-timeline.AUTHOR_NAME>",
                "<hernancerm.git-timeline.COMMITTER_NAME>",
                    "%cn",
                "</hernancerm.git-timeline.COMMITTER_NAME>",
                "<hernancerm.git-timeline.AUTHOR_DATE>",
                    "%ad",
                "</hernancerm.git-timeline.AUTHOR_DATE>",
                "<hernancerm.git-timeline.SUBJECT_LINE>",
                    "%s",
                "</hernancerm.git-timeline.SUBJECT_LINE>",
                "<hernancerm.git-timeline.REF_NAMES_COLORED>",
                    "%C(auto)%d",
                "</hernancerm.git-timeline.REF_NAMES_COLORED>");

        return Stream.concat(Stream.of(
                        "git",
                        "log",
                        "--color=always",
                        "--date=format:%d/%b/%Y",
                        "--pretty=" + prettyFormat),
                Arrays.stream(args)).toList();
    }
}
