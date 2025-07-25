package me.hernancerm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import picocli.CommandLine.Command;

@Command(name = "git-timeline",
        version = "git-timeline 0.1",
        mixinStandardHelpOptions = true,
        description = "A small wrapper for git-log which improves readability.")
public class GitTimeline implements Callable<Integer> {

    private final String[] args;

    public GitTimeline(String[] args) {
        this.args = args;
    }

    @Override
    public Integer call() throws Exception {

        List<String> command = Stream.concat(Stream.of(
                        "git",
                        "log",
                        "--date=format:%d/%b/%Y",
                        "--pretty=HASH=%H%nAUTHOR=%an%nDATE=%ad%nMESSAGE=%s%nEND"),
                Arrays.stream(args)).toList();
        Process gitLogProcess = new ProcessBuilder(command).start();

        try (
                InputStreamReader inputStreamReader = new InputStreamReader(gitLogProcess.getErrorStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        ) {
            String line = bufferedReader.readLine();
            while (line != null) {
                System.err.println(line);
                line = bufferedReader.readLine();
            }
        }

        // TODO: Break down into another class.
        Pattern pattern = Pattern.compile("^([A-Z]+)=(.*)$");
        try (
                InputStreamReader inputStreamReader = new InputStreamReader(gitLogProcess.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        ) {
            String line = bufferedReader.readLine();
            Map<String, String> rawCommit = new HashMap<>();
            while (line != null) {
                if (line.matches("^END$")) {
                    prettyPrint(CommitMapper.mapToCommit(rawCommit));
                    rawCommit.clear();
                } else {
                    Matcher matcher = pattern.matcher(line);
                    if (!matcher.matches()) {
                        // TODO: Improve error message.
                        throw new IllegalStateException("A match is always expected");
                    }
                    rawCommit.put(matcher.group(1), matcher.group(2));
                }
                line = bufferedReader.readLine();
            }
        }

        gitLogProcess.waitFor(1500, TimeUnit.MILLISECONDS);
        return gitLogProcess.exitValue();
    }

    // TODO: Colored output when expected (colored on terminal, not colored on non-TTY target).
    private void prettyPrint(Commit commit) {
        System.out.println(commit);
    }
}
