package me.hernancerm;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "git-timeline", mixinStandardHelpOptions = true, version = "git-timeline 0.1",
        description = "A small wrapper for git-log which improves readability.")
public class GitTimeline implements Callable<Integer> {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new GitTimeline()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        System.out.println("Hello, World!");
        return 0;
    }
}