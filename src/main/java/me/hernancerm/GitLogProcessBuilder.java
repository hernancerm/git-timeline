package me.hernancerm;

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

    private static final String NAMESPACE = "me/hernancerm/git-timeline/";

    private static final String ABBREVIATED_HASH = "ABBREVIATED_HASH";
    private static final String ABBREVIATED_PARENT_HASHES = "ABBREVIATED_PARENT_HASHES";
    private static final String AUTHOR_NAME = "AUTHOR_NAME";
    private static final String AUTHOR_DATE = "AUTHOR_DATE";
    private static final String COMMITTER_NAME = "COMMITTER_NAME";
    private static final String SUBJECT_LINE = "SUBJECT_LINE";
    private static final String REF_NAMES_COLORED = "REF_NAMES";

    private static final String KEY_VALUE_REGEX =
            "\\[me/hernancerm/git-timeline/([A-Z_]+)](.*?)\\[me/hernancerm/git-timeline/#]";
    private static final String COMMIT_BEGIN_END_REGEX =
            "\\[me/hernancerm/git-timeline/<].*\\[me/hernancerm/git-timeline/>]";

    public int start(String[] args, Function<Commit, String> formatter) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(getGitLogCommand(args)).start();

        try (
                var inputStreamReader = new InputStreamReader(process.getErrorStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            bufferedReader.lines().forEach(System.err::println);
        }

        try (
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            Commit commit = new Commit();
            Pattern pattern = Pattern.compile(KEY_VALUE_REGEX);
            bufferedReader.lines().forEach(line -> {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    populateCommitAttribute(matcher.group(1), matcher.group(2), commit);
                }
                System.out.println(line.replaceFirst(COMMIT_BEGIN_END_REGEX, formatter.apply(commit)));
                commit.reset();
            });
        }

        process.waitFor(1000, TimeUnit.MILLISECONDS);
        return process.exitValue();
    }

    private void populateCommitAttribute(
            String serializedAttributeName,
            String attributeValue,
            Commit commit
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
        return Stream.concat(Stream.of(
                "git",
                "log",
                "--date=format:%d/%b/%Y",
                "--color=always",
                "--pretty=[" + NAMESPACE + "<]"
                        + "[" + NAMESPACE + ABBREVIATED_HASH + "]%h[" + NAMESPACE + "#]"
                        + "[" + NAMESPACE + ABBREVIATED_PARENT_HASHES + "]%p[" + NAMESPACE + "#]"
                        + "[" + NAMESPACE + AUTHOR_NAME + "]%an[" + NAMESPACE + "#]"
                        + "[" + NAMESPACE + AUTHOR_DATE + "]%ad[" + NAMESPACE + "#]"
                        + "[" + NAMESPACE + SUBJECT_LINE +"]%s[" + NAMESPACE + "#]"
                        + "[" + NAMESPACE + REF_NAMES_COLORED + "]%C(auto)%d[" + NAMESPACE + "#]"
                        + "[" + NAMESPACE + ">]"),
                Arrays.stream(args)).toList();
    }
}
