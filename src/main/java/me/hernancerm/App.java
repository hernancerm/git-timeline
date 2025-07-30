package me.hernancerm;

import java.io.IOException;

import picocli.CommandLine;

public class App {

    public static void main(String[] args) throws IOException, InterruptedException {
        CommandLine commandLine = new CommandLine(
                new GitTimeline(
                        args,
                        new GitLogProcessBuilder(),
                        new GitLogFormatter())
        );
        commandLine.setUnmatchedArgumentsAllowed(true);
        System.exit(commandLine.execute(args));
    }
}