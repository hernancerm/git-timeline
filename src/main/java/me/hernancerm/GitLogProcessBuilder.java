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

public class GitLogProcessBuilder {

    private static final String ABBREVIATED_HASH = "ABBREVIATED_HASH";
    private static final String ABBREVIATED_PARENT_HASHES = "ABBREVIATED_PARENT_HASHES";
    private static final String AUTHOR_NAME = "AUTHOR_NAME";
    private static final String AUTHOR_DATE = "AUTHOR_DATE";
    private static final String COMMITTER_NAME = "COMMITTER_NAME";
    private static final String SUBJECT_LINE = "SUBJECT_LINE";
    private static final String REF_NAMES_COLORED = "REF_NAMES_COLORED";

    private static final String COMMIT_BOUNDS_REGEX =
            "<git-timeline/commit-begin>.*<git-timeline/commit-end>";
    private static final String KEY_VALUE_REGEX =
            "<git-timeline/item-begin/([A-Z_]+)>(.*?)<git-timeline/item-end>";

    public int start(String[] args, Function<GitCommit, String> formatter) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(getGitLogCommand(args)).start();

//        try (
//                var inputStreamReader = new InputStreamReader(process.getErrorStream());
//                var bufferedReader = new BufferedReader(inputStreamReader)
//        ) {
//            bufferedReader.lines().forEach(System.err::println);
//        }

        try (
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String line;
            GitCommit commit = new GitCommit();
            Pattern keyValuePattern = Pattern.compile(KEY_VALUE_REGEX);
            Pattern commitBoundsPattern = Pattern.compile(COMMIT_BOUNDS_REGEX);
            while ((line = bufferedReader.readLine()) != null) {
                Matcher keyValuematcher = keyValuePattern.matcher(line);
                while (keyValuematcher.find()) {
                    populateCommitAttribute(keyValuematcher.group(1), keyValuematcher.group(2), commit);
                }
                Matcher commitBoundsMatcher = commitBoundsPattern.matcher(line);
                if (commitBoundsMatcher.find()) {
                    int matchIndex = commitBoundsMatcher.start();
                    System.out.println(ansi().render(line.substring(0, matchIndex) + formatter.apply(commit)));
                }
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
            case ABBREVIATED_HASH:
                commit.setAbbreviatedHash(attributeValue);
                break;
            case ABBREVIATED_PARENT_HASHES:
                commit.setAbbreviatedParentHashes(attributeValue.split("\\s"));
                break;
            case AUTHOR_NAME:
                commit.setAuthorName(attributeValue);
                break;
            case AUTHOR_DATE:
                commit.setAuthorDate(attributeValue);
                break;
            case COMMITTER_NAME:
                commit.setCommitterName(attributeValue);
                break;
            case SUBJECT_LINE:
                commit.setSubjectLine(attributeValue);
                break;
            case REF_NAMES_COLORED:
                commit.setRefNamesColored(attributeValue);
                break;
        }
    }

    private List<String> getGitLogCommand(String[] args) {

        // XML-inspired format with these goals: Easy to parse, fast to parse and reasonably resistant to unsanitized
        // input. Since this is parsed using regex, unsanitized input can only break the format if the exact tags are
        // in the input, which is unlikely under normal use, but easy to cause if intentional.
        final String prettyFormat = "<git-timeline/commit-begin>"
                + "<git-timeline/item-begin/FULL_HASH>%H<git-timeline/item-end>"
                + "<git-timeline/item-begin/ABBREVIATED_HASH>%h<git-timeline/item-end>"
                + "<git-timeline/item-begin/ABBREVIATED_PARENT_HASHES>%p<git-timeline/item-end>"
                + "<git-timeline/item-begin/AUTHOR_NAME>%an<git-timeline/item-end>"
                + "<git-timeline/item-begin/COMMITTER_NAME>%cn<git-timeline/item-end>"
                + "<git-timeline/item-begin/AUTHOR_DATE>%ad<git-timeline/item-end>"
                + "<git-timeline/item-begin/SUBJECT_LINE>%s<git-timeline/item-end>"
                + "<git-timeline/item-begin/REF_NAMES_COLORED>%C(auto)%d<git-timeline/item-end>"
                + "<git-timeline/commit-end>";

        return Stream.concat(Stream.of(
                        "git",
                        "log",
                        "--color=always",
                        "--date=format:%d/%b/%Y",
                        "--pretty=" + prettyFormat),
                Arrays.stream(args)).toList();
    }
}
