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

    // System.console() is also null when stdin is redirected, so `git timeline < /dev/null`
    // loses color where git would keep it. Use `--color=always` for that case.
    @Override
    public Integer call() throws Exception {
        return gitLogProcessBuilder.start(
                parseArgs(args, System.console() != null), gitLogFormatter::format);
    }

    GitLogArgs parseArgs(String[] args, boolean isTerminal) {
        List<String> unparsedArgs = new ArrayList<>();
        var isGraphEnabled = false;
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
                case "--color":
                case "--color=always":
                    // Consistent with: https://git-scm.com/docs/git-log
                    isColorEnabled = true;
                    break;
                case "--color=auto":
                    isColorEnabled = isTerminal;
                    break;
                case "--color=never":
                case "--no-color":
                    // Consistent with: https://git-scm.com/docs/git-log
                    isColorEnabled = false;
                    break;
                case "--no-pager":
                    // Consistent with: https://git-scm.com/docs/git
                    isPagerEnabled = false;
                    break;
                case "--graph":
                    unparsedArgs.add(arg);
                    isGraphEnabled = true;
                    break;
                default:
                    unparsedArgs.add(arg);
                    break;
            }
        }

        setAnsiEnabled(isColorEnabled);
        return new GitLogArgs(
                unparsedArgs.toArray(new String[0]),
                isColorEnabled,
                isPagerEnabled,
                isGraphEnabled);
    }

    private void setAnsiEnabled(boolean enabled) {
        Ansi.setEnabled(enabled);
        AnsiUtils.setEnabled(enabled);
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

                    -P, --no-pager
                        Disable paging.""");
        System.exit(0);
    }

    private void handleVersionOption() {
        System.out.println(NAME + " " + VERSION);
        System.exit(0);
    }

}
