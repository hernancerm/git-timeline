package me.hernancerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GitLogProcessBuilder {

    private static final String HASH = "HASH";
    private static final String AUTHOR_NAME = "AUTHOR_NAME";
    private static final String AUTHOR_DATE = "AUTHOR_DATE";
    private static final String SUBJECT_LINE = "SUBJECT_LINE";

    private static final List<String> GIT_LOG_BASE_COMMAND = List.of(
            "git",
            "log",
            "--date=format:%d/%b/%Y",
            "--pretty="
                    + HASH + "=%H%n"
                    + AUTHOR_NAME + "=%an%n"
                    + AUTHOR_DATE + "=%ad%n"
                    + SUBJECT_LINE +"=%s%n"
                    + "END");

    private static final String KEY_VALUE_REGEX = "^([A-Z_]+)=(.*)$";
    private static final String END_REGEX = "^END$";

    /**
     * Run git-log making it output an easy-to-deserialize format which does not require
     * escape sequences to correctly capture any possible characters. Current constraint:
     * Values which can have a newline character are not handled correctly, e.g. the body.
     * For each commit perform the callback.
     *
     * @param args Optional additional args for git-log.
     * @param callback What to do for each deserialized commit.
     * @return The exit code of git-log process.
     * @throws IOException If an error occurs reading a line from the reader.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    public int start(List<String> args, Consumer<Commit> callback) throws IOException, InterruptedException {
        List<String> command = Stream.concat(GIT_LOG_BASE_COMMAND.stream(), args.stream()).toList();
        Process process = new ProcessBuilder(command).start();

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
                if (line.matches(END_REGEX)) {
                    callback.accept(commit);
                    commit.reset();
                } else {
                    Matcher matcher = pattern.matcher(line);
                    if (!matcher.matches()) {
                        throw new IllegalStateException(String.format(
                                "All lines without the end delimiter '%s' must have a key." +
                                        " Line without key: %s", END_REGEX, line));
                    }
                    populateCommitAttribute(matcher.group(1), matcher.group(2), commit);
                }
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
            case HASH:
                commit.setHash(attributeValue);
                break;
            case AUTHOR_NAME:
                commit.setAuthorName(attributeValue);
                break;
            case AUTHOR_DATE:
                commit.setAuthorDate(attributeValue);
                break;
            case SUBJECT_LINE:
                commit.setSubjectLine(attributeValue);
                break;
        }
    }
}
