package me.hernancerm;

import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "git-timeline",
        version = "git-timeline 0.1",
        mixinStandardHelpOptions = true,
        description = "A small wrapper for git-log which improves readability.")
public class GitTimeline implements Callable<Integer> {

    private final List<String> args;
    private final GitLogProcessBuilder gitLogProcessBuilder;

    public GitTimeline(
            List<String> args,
            GitLogProcessBuilder gitLogProcessBuilder
    ) {
        this.args = args;
        this.gitLogProcessBuilder = gitLogProcessBuilder;
    }

    @Override
    public Integer call() throws Exception {
        return gitLogProcessBuilder.start(args, this::prettyPrint);
    }

    // TODO: Colored output when expected (colored on terminal, not colored on non-TTY target).
    private void prettyPrint(Commit commit) {
        System.out.println(commit);
    }
}
