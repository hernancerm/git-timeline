package me.hernancerm;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "git-timeline",
        version = "git-timeline 0.1",
        mixinStandardHelpOptions = true,
        description = "A small wrapper for git-log which improves readability.")
public class GitTimeline implements Callable<Integer> {

    private final String[] args;
    private final GitLogProcessBuilder gitLogProcessBuilder;
    private final GitLogFormatter gitLogFormatter;

    public GitTimeline(
            String[] args,
            GitLogProcessBuilder gitLogProcessBuilder,
            GitLogFormatter gitLogFormatter
    ) {
        this.args = args;
        this.gitLogProcessBuilder = gitLogProcessBuilder;
        this.gitLogFormatter = gitLogFormatter;
    }

    @Override
    public Integer call() throws Exception {
        return gitLogProcessBuilder.start(args, gitLogFormatter::format);
    }
}
