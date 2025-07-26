package me.hernancerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GitLogProcessBuilder {

    // Commit items safe to use without sanitization.
    private static final String FULL_HASH = "fullHash";
    private static final String ABBREVIATED_HASH = "abbreviatedHash";
    private static final String ABBREVIATED_PARENT_HASHES = "abbreviatedParentHashes";

//    private static final String AUTHOR_NAME = "AUTHOR_NAME";
//    private static final String AUTHOR_DATE = "AUTHOR_DATE";
//    private static final String COMMITTER_NAME = "COMMITTER_NAME";
//    private static final String SUBJECT_LINE = "SUBJECT_LINE";
//    private static final String REF_NAMES_COLORED = "REF_NAMES";

    public int start(String[] args, Consumer<Commit> callback) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(getGitLogCommand(args)).start();

        try (
                var inputStreamReader = new InputStreamReader(process.getErrorStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.err.println(line);
            }
        }

        try (
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                int indexOfSerializedCommit = line.indexOf('{');
                // Substring required due to the option `--graph` of git-log.
                Commit commit = objectMapper.readValue(line.substring(indexOfSerializedCommit), Commit.class);
                commit.setGitLogFullLine(line);
                callback.accept(commit);
            }
        }

        process.waitFor(1000, TimeUnit.MILLISECONDS);
        return process.exitValue();
    }

    private List<String> getGitLogCommand(String[] args) {
        return Stream.concat(Stream.of(
                "git",
                "log",
                "--date=format:%d/%b/%Y",
                "--color=always",
                "--pretty={"
                        + "\"" + FULL_HASH + "\":\"%H\","
                        + "\"" + ABBREVIATED_HASH + "\":\"%h\","
                        + "\"" + ABBREVIATED_PARENT_HASHES + "\":\"%p\""
                + "}"),
                Arrays.stream(args)).toList();
    }
}
