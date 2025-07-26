package me.hernancerm;

import java.util.concurrent.Callable;

import org.fusesource.jansi.AnsiConsole;

import picocli.CommandLine.Command;

@Command(name = "git-timeline",
        version = "git-timeline 0.1",
        mixinStandardHelpOptions = true,
        description = "A small wrapper for git-log which improves readability.")
public class GitTimeline implements Callable<Integer> {

    private final String[] args;
    private final GitLogProcessBuilder gitLogProcessBuilder;
    private final GitLogFormatter gitLogPrettyPrinter;

    public GitTimeline(
            String[] args,
            GitLogProcessBuilder gitLogProcessBuilder,
            GitLogFormatter gitLogPrettyPrinter
    ) {
        this.args = args;
        this.gitLogProcessBuilder = gitLogProcessBuilder;
        this.gitLogPrettyPrinter = gitLogPrettyPrinter;
    }

    @Override
    public Integer call() throws Exception {
        AnsiConsole.systemInstall();
        int exitCode = gitLogProcessBuilder.start(args, gitLogPrettyPrinter::format);
        AnsiConsole.systemUninstall();
        return exitCode;
    }
}
