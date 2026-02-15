package me.hernancerm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Override
    public Integer call() throws Exception {
        return gitLogProcessBuilder.start(parseArgs(args), gitLogFormatter::format);
    }

    private GitLogArgs parseArgs(String[] args) {
        List<String> unparsedArgs = new ArrayList<>();
        var isGraphEnabled = false;
        var isPagerEnabled = true;
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
                    // Consistent with: https://git-scm.com/docs/git-log
                    unparsedArgs.add(arg);
                    setAnsiEnabled(true);
                    break;
                case "--color=never":
                case "--no-color":
                    // Consistent with: https://git-scm.com/docs/git-log
                    unparsedArgs.add(arg);
                    setAnsiEnabled(false);
                    break;
                case "--no-pager":
                    // Consistent with: https://git-scm.com/docs/git
                    isPagerEnabled = false;
                    break;
                case "--graph":
                    unparsedArgs.add(arg);
                    isGraphEnabled = true;
                    break;
                case "--sync-zsh-completions":
                    handleSyncZshCompletions();
                    break;
                default:
                    unparsedArgs.add(arg);
                    break;
            }
        }
        return new GitLogArgs(
                unparsedArgs.toArray(new String[0]),
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
                        Disable paging.

                    --sync-zsh-completions
                        Update Zsh completion file. This is necessary when the installed
                        Git version changes, e.g., after a Git upgrade. Use an env var:
                          BREW=1    When git-timeline was installed with Homebrew.""");
        System.exit(0);
    }

    private void handleVersionOption() {
        System.out.println(NAME + " " + VERSION);
        System.exit(0);
    }

    private void handleSyncZshCompletions() {
        var base_url = "https://raw.githubusercontent.com/hernancerm/git-timeline";
        var path = "/refs/heads/main/completions/install-completions.zsh";
        var url = base_url + path;
        try {
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("ERROR: Failed to download install-completions.zsh"
                        + " (HTTP " + response.statusCode() + ")");
                System.err.println("  URL: " + url);
                System.exit(1);
            }
            var tempFile = Files.createTempFile("install-completions-", ".zsh");
            try {
                Files.writeString(tempFile, response.body());
                var process = new ProcessBuilder("zsh", tempFile.toString())
                        .inheritIO()
                        .start();
                System.exit(process.waitFor());
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }
}
