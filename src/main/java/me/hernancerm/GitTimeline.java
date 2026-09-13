package me.hernancerm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.jline.jansi.Ansi;

public class GitTimeline implements Callable<Integer> {

    private static final String NAME = "git-timeline";
    private static final String VERSION = "2.2-SNAPSHOT";

    private final String[] args;
    private final GitLogProcessBuilder gitLogProcessBuilder;

    public GitTimeline(String[] args, GitLogProcessBuilder gitLogProcessBuilder) {
        this.args = args;
        this.gitLogProcessBuilder = gitLogProcessBuilder;
    }

    // System.console() is also null when stdin is redirected, so `git timeline < /dev/null`
    // loses color where git would keep it. Use `--color=always` for that case.
    @Override
    public Integer call() throws Exception {
        GitLogArgs gitLogArgs = parseArgs(args, System.console() != null);
        GitLogFormatter formatter =
                new GitLogFormatter(new Hyperlinker(gitLogArgs.isColorEnabled()));
        return gitLogProcessBuilder.start(gitLogArgs, formatter::format);
    }

    GitLogArgs parseArgs(String[] args, boolean isTerminal) {
        List<String> unparsedArgs = new ArrayList<>();
        var isColorEnabled = isTerminal;
        var isPagerEnabled = isTerminal;

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
                // Consistent with: https://git-scm.com/docs/git-log
                case "--color":
                case "--color=always":
                    isColorEnabled = true;
                    break;
                case "--color=auto":
                    isColorEnabled = isTerminal;
                    break;
                case "--color=never":
                case "--no-color":
                    isColorEnabled = false;
                    break;
                // Consistent with: https://git-scm.com/docs/git
                case "--no-pager":
                    isPagerEnabled = false;
                    break;
                default:
                    unparsedArgs.add(arg);
                    break;
            }
        }

        // jline keeps its own global, so this one stays.
        Ansi.setEnabled(isColorEnabled);
        return new GitLogArgs(
                unparsedArgs.toArray(new String[0]),
                isColorEnabled,
                isPagerEnabled);
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
                        Display the version of git-timeline.

                    --no-pager
                        Disable paging.""");
        System.exit(0);
    }

    private void handleVersionOption() {
        System.out.println(NAME + " " + VERSION);
        System.exit(0);
    }

}
