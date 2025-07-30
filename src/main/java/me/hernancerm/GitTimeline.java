package me.hernancerm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.fusesource.jansi.Ansi;

import lombok.Data;

//@Command(name = "git-timeline",
//        version = "git-timeline 0.1",
//        mixinStandardHelpOptions = true,
//        description = "A small wrapper for git-log which improves readability.")
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
        return gitLogProcessBuilder.start(parseArgs(args), gitLogFormatter::format);
    }

    private GitLogArgs parseArgs(String[] args) {
        List<String> unparsedArgs = new ArrayList<>();
        GitLogArgs output = new GitLogArgs();
        for (String arg : args) {
            switch (arg) {
                case "--color=always":
                    unparsedArgs.add(arg);
                    Ansi.setEnabled(true);
                    break;
                case "--color=never":
                    unparsedArgs.add(arg);
                    Ansi.setEnabled(false);
                    break;
                case "--no-pager":
                case "-P":
                    // Consistent with: https://git-scm.com/docs/git
                    output.setPagerEnabled(false);
                    break;
                case "--paginate":
                case "-p":
                    // Consistent with: https://git-scm.com/docs/git
                    output.setPagerEnabled(true);
                    break;
                default:
                    unparsedArgs.add(arg);
                    break;
            }
        }
        output.setUnparsedArgs(unparsedArgs.toArray(new String[0]));
        return output;
    }
}
