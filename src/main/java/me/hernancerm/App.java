package me.hernancerm;

import java.io.IOException;
import java.util.Arrays;

import picocli.CommandLine;

public class App {

    public static void main(String[] args) throws IOException {
        CommandLine commandLine = new CommandLine(
                new GitTimeline(
                        Arrays.stream(args).toList(),
                        new GitLogProcessBuilder()
                )
        );
        commandLine.setUnmatchedArgumentsAllowed(true);
        System.exit(commandLine.execute(args));
    }
}