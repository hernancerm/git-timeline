package me.hernancerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class GitCommitDao {

    public CompletableFuture<String> getItem(String hash, String item) {
        return getItem(hash, item, List.of());
    }

    public CompletableFuture<String> getItem(String hash, String item, String arg) {
        return getItem(hash, item, List.of(arg));
    }

    public CompletableFuture<String> getItem(String hash, String item, List<String> args) {
        return CompletableFuture.supplyAsync(() -> {
            Process process;
            try {
                process = new ProcessBuilder(Stream.concat(
                        Stream.of("git", "log", hash, "--pretty=" + item, "-1", "--color=always"),
                        args.stream()).toList()).start();
            } catch (IOException e) {
                throw new RuntimeException(
                        String.format(
                                "Error starting git-log process to retrieve item '%s' for commit '%s'",
                                item, hash),
                        e);
            }

            try (
                    var inputStreamReader = new InputStreamReader(process.getInputStream());
                    var bufferedReader = new BufferedReader(inputStreamReader)
            ) {
                String line = bufferedReader.readLine();
                return Objects.requireNonNullElse(line, "");
            } catch (IOException e) {
                throw new RuntimeException(
                        String.format(
                                "Error reading git-log stdout to retrieve item '%s' for commit '%s'",
                                item, hash),
                        e);
            }
        });
    }
}
