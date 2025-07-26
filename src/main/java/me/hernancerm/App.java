package me.hernancerm;

import picocli.CommandLine;

public class App {

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(
                new GitTimeline(
                        args,
                        new GitLogProcessBuilder(),
                        new GitLogPrettyPrinter())
        );
        commandLine.setUnmatchedArgumentsAllowed(true);
        System.exit(commandLine.execute(args));
    }
}