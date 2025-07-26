package me.hernancerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GitLogProcessBuilder {

    // Commit items safe to use without sanitization.
    private static final String FULL_HASH = "fullHash";
    private static final String ABBREVIATED_HASH = "abbreviatedHash";
    private static final String ABBREVIATED_PARENT_HASHES = "abbreviatedParentHashes";

    private final GitCommitDao gitCommitDao;

    public GitLogProcessBuilder(GitCommitDao gitCommitDao) {
        this.gitCommitDao = gitCommitDao;
    }

    public int start(String[] args, Consumer<GitCommit> callback)
            throws IOException, InterruptedException, ExecutionException {
        Process process = new ProcessBuilder(getGitLogCommand(args)).start();

//        try (
//                var inputStreamReader = new InputStreamReader(process.getErrorStream());
//                var bufferedReader = new BufferedReader(inputStreamReader)
//        ) {
//            String line;
//            // TODO: For some reason this causes a freeze on big repos. Thread gets stuck here.
//            while ((line = bufferedReader.readLine()) != null) {
//                System.err.println(line);
//            }
//        }

        try (
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                ObjectMapper objectMapper = new ObjectMapper();

                int indexOfSerializedCommit = line.indexOf('{');
                // Substring required due to the option `--graph` of git-log.
                GitCommit commit = objectMapper.readValue(line.substring(indexOfSerializedCommit), GitCommit.class);
                commit.setGitLogFullLine(line);

                String hash = commit.getFullHash();
                String dateFormat = "--date=format:%d/%b/%Y";
                CompletableFuture<String> authorName = CompletableFuture.supplyAsync(
                        () -> gitCommitDao.getItem(hash, "%an"));
                CompletableFuture<String> committerName = CompletableFuture.supplyAsync(
                        () -> gitCommitDao.getItem(hash, "%cn"));
                CompletableFuture<String> authorDate = CompletableFuture.supplyAsync(
                        () -> gitCommitDao.getItem(hash, "%ad", dateFormat));
                CompletableFuture<String> refNamesColored = CompletableFuture.supplyAsync(
                        () -> gitCommitDao.getItem(hash, "%C(auto)%d"));
                CompletableFuture<String> subjectLine = CompletableFuture.supplyAsync(
                        () -> gitCommitDao.getItem(hash, "%s"));
                CompletableFuture.allOf(
                        authorName, committerName, authorDate, refNamesColored, subjectLine)
                        .join();

                commit.setAuthorName(authorName.get());
                commit.setCommitterName(committerName.get());
                commit.setAuthorDate(authorDate.get());
                commit.setSubjectLine(subjectLine.get());
                commit.setRefNamesColored(refNamesColored.get());

                callback.accept(commit);
            }
        }

        process.waitFor(500, TimeUnit.MILLISECONDS);
        return process.exitValue();
    }

    private List<String> getGitLogCommand(String[] args) {
        return Stream.concat(Stream.of(
                "git",
                "log",
                "--pretty={"
                        + "\"" + FULL_HASH + "\":\"%H\","
                        + "\"" + ABBREVIATED_HASH + "\":\"%h\","
                        + "\"" + ABBREVIATED_PARENT_HASHES + "\":\"%p\""
                + "}"),
                Arrays.stream(args)).toList();
    }
}
