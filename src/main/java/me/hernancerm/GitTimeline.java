package me.hernancerm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import picocli.CommandLine.Command;

@Command(name = "git-timeline",
        version = "git-timeline 0.1",
        mixinStandardHelpOptions = true,
        description = "A small wrapper for git-log which improves readability.")
public class GitTimeline implements Callable<Integer> {

    private final List<String> args;
    private final GitLogProcessBuilder gitLogProcessBuilder;
    private final GitLogDeserializer gitLogDeserializer;

    public GitTimeline(
            List<String> args,
            GitLogProcessBuilder gitLogProcessBuilder,
            GitLogDeserializer gitLogDeserializer
    ) {
        this.args = args;
        this.gitLogProcessBuilder = gitLogProcessBuilder;
        this.gitLogDeserializer = gitLogDeserializer;
    }

    @Override
    public Integer call() throws Exception {

        Process process = gitLogProcessBuilder.start(args);

        try (
                InputStreamReader inputStreamReader = new InputStreamReader(process.getErrorStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            String line = bufferedReader.readLine();
            while (line != null) {
                System.err.println(line);
                line = bufferedReader.readLine();
            }
        }

        // TODO: Break down into another class.
        try (
                InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            gitLogDeserializer.process(bufferedReader, this::prettyPrint);
        }

        process.waitFor(1500, TimeUnit.MILLISECONDS);
        return process.exitValue();
    }

    // TODO: Colored output when expected (colored on terminal, not colored on non-TTY target).
    private void prettyPrint(Commit commit) {
        System.out.println(commit);
    }
}
