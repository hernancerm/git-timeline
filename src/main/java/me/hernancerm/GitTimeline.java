package me.hernancerm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picocli.CommandLine.Command;

@Command(name = "git-timeline",
        version = "git-timeline 0.1",
        mixinStandardHelpOptions = true,
        description = "A small wrapper for git-log which improves readability.")
public class GitTimeline implements Callable<Integer> {

    private final List<String> args;
    private final GitLogProcessBuilder gitLogProcessBuilder;

    public GitTimeline(List<String> args, GitLogProcessBuilder gitLogProcessBuilder) {
        this.args = args;
        this.gitLogProcessBuilder = gitLogProcessBuilder;
    }

    @Override
    public Integer call() throws Exception {

        Process process = gitLogProcessBuilder.start(args);

        try (
                InputStreamReader inputStreamReader = new InputStreamReader(process.getErrorStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        ) {
            String line = bufferedReader.readLine();
            while (line != null) {
                System.err.println(line);
                line = bufferedReader.readLine();
            }
        }

        // TODO: Break down into another class.
        Pattern pattern = Pattern.compile("^([A-Z_]+)=(.*)$");
        try (
                InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        ) {
            String line = bufferedReader.readLine();
            Map<String, String> result = new HashMap<>();
            while (line != null) {
                if (line.matches("^END$")) {
                    prettyPrint(CommitMapper.mapToCommit(result));
                    result.clear();
                } else {
                    Matcher matcher = pattern.matcher(line);
                    if (!matcher.matches()) {
                        // TODO: Improve error message.
                        throw new IllegalStateException("A match is always expected");
                    }
                    result.put(matcher.group(1), matcher.group(2));
                }
                line = bufferedReader.readLine();
            }
        }

        process.waitFor(1500, TimeUnit.MILLISECONDS);
        return process.exitValue();
    }

    // TODO: Colored output when expected (colored on terminal, not colored on non-TTY target).
    private void prettyPrint(Commit commit) {
        System.out.println(commit);
    }
}
