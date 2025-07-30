package me.hernancerm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.fusesource.jansi.Ansi;

public class GitTimeline implements Callable<Integer> {

    private static final String NAME = "git-timeline";
    private static final String VERSION = "0.1";

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
                case "--help":
                case "-h":
                    handleHelpOption();
                    break;
                case "--version":
                case "-v":
                    handleVersionOption();
                    break;
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

    private void handleHelpOption() {
        System.out.println(NAME + " " + VERSION);
        System.out.println("""
                A small wrapper for git-log which improves readability.
                    -h, --help
                        Display this help message. Since git-timeline is a wrapper around
                        git-log, all options supported by git-log are also supported by
                        git-timeline. See: git log --help
                    -v, --version
                        Display the version of git-timeline.""");
        System.exit(0);
    }

    private void handleVersionOption() {
        System.out.println(NAME + " " + VERSION);
        System.exit(0);
    }
}
