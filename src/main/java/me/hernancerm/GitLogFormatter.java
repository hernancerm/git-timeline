package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitLogFormatter {

    public String format(GitCommit commit) {
        boolean isMergeCommit = commit.getAbbreviatedParentHashes().length > 1;
        boolean authorDiffersFromCommitter = !commit.getAuthorName().equals(
                commit.getCommitterName());
        return ansi().render(
                        (isMergeCommit ? "@|bold,yellow " : "@|italic,yellow ")
                                + commit.getAbbreviatedHash()
                                + "|@ "
                        + "@|italic,green "
                                + commit.getAuthorDate()
                                + "|@ "
                        + (authorDiffersFromCommitter ? "@|bold,cyan " : "@|italic,cyan ")
                                + commit.getAuthorName()
                                + "|@ "
                        + commit.getRefNamesColored()
                        + " " + hyperlinkSubjectLine(getRemoteOriginUrl(), commit.getSubjectLine()))
                .toString();
    }

    private String getRemoteOriginUrl() {
        Process process;

        try {
            process = new ProcessBuilder("git", "remote", "get-url", "origin").start();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error starting git process to get remote url for: origin",
                    e);
        }

        try (
            var inputStreamReader = new InputStreamReader(process.getInputStream());
            var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            return bufferedReader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error reading remote url for: origin",
                    e);
        }
    }

    private String hyperlinkSubjectLine(String originRemote, String subjectLine) {
        String hyperlinkedSubjectLine = subjectLine;

        Pattern remoteUrlPattern = Pattern.compile("@(.*?):(.*?)/");
        Matcher remoteUrlMatcher = remoteUrlPattern.matcher(originRemote);

        if (remoteUrlMatcher.find()) {
            String platform = remoteUrlMatcher.group(1);
            String user = remoteUrlMatcher.group(2);

            if (platform.equals("bitbucket.org")) {

                // Hyperlink Jira keys.
                //
                //   SSH remote origin URL:  git@bitbucket.org:<user>/<repo>.git
                //   Jira key URL:           https://<user>.atlassian.net/browse/ABC-123

                String jiraKeyRegex = "[A-Z]+-\\d+";
                Pattern jiraKeyPattern = Pattern.compile(jiraKeyRegex);
                Matcher jiraKeyMatcher = jiraKeyPattern.matcher(subjectLine);

                Deque<String> hyperlinks = new ArrayDeque<>();
                while (jiraKeyMatcher.find()) {
                    hyperlinks.offerLast(
                            buildHyperlink(
                                    "https://" + user + ".atlassian.net/browse/" + jiraKeyMatcher.group(0),
                                    jiraKeyMatcher.group(0)));
                }

                jiraKeyMatcher.reset();
                while (jiraKeyMatcher.find() && !hyperlinks.isEmpty()) {
                    hyperlinkedSubjectLine = subjectLine.replaceFirst(jiraKeyRegex, hyperlinks.pollFirst());
                }
            }
        }

        return hyperlinkedSubjectLine;
    }

    private String buildHyperlink(String url, String title) {
        // https://unix.stackexchange.com/a/437585
        // To get the octal escape sequences for '\e', '\a', etc., do this:
        // 1. $ echo -n '\e' > _.txt
        // 2. $ nvim _.txt
        // 3. ga
        return "\033]8;;" + url + "\007" + title + "\033]8;;\007";
    }
}
