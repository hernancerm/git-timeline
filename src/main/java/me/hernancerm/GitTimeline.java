package me.hernancerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import picocli.CommandLine;
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

    public static void main(String[] args) throws IOException {
        CommandLine commandLine = new CommandLine(new GitTimeline(args));
        commandLine.setUnmatchedArgumentsAllowed(true);
        System.exit(commandLine.execute(args));
    }

    @Override
    public Integer call() throws Exception {

        List<String> command = Stream.concat(Stream.of(
                        "git",
                        "log",
                        "--date=format:%d/%b/%Y",
                        "--pretty=HASH=%H%nAUTHOR=%an%nDATE=%ad%nMESSAGE=%s%nEND"),
                Arrays.stream(args)).toList();
        Process process = new ProcessBuilder(command).start();
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
        }

        return 0;
    }

//    private Map<String, String> deserialize(String input) {
//    }
}