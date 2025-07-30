package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
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
                        + " " + hyperlinkSubjectLine(commit.getRemoteOriginUrl(), commit.getSubjectLine()))
                .toString();
    }

    private String hyperlinkSubjectLine(String originRemote, String subjectLine) {
        String hyperlinkedSubjectLine = "";

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

                String nonHyperlinkedJiraKeyRegex = "([A-Z]+-\\d+)(?!.*\007)";
                Pattern jiraKeyPattern = Pattern.compile(nonHyperlinkedJiraKeyRegex);
                Matcher jiraKeyMatcher = jiraKeyPattern.matcher(subjectLine);

                Deque<String> hyperlinks = new ArrayDeque<>();
                while (jiraKeyMatcher.find()) {
                    hyperlinks.offerLast(buildHyperlink(
                            "https://" + user + ".atlassian.net/browse/" + jiraKeyMatcher.group(1),
                            jiraKeyMatcher.group(1)));
                }

                if (!hyperlinks.isEmpty()) {
                    hyperlinkedSubjectLine = subjectLine;
                    int totalHyperlinks = hyperlinks.size();
                    for (int i = 0; i < totalHyperlinks; i++) {
                        jiraKeyMatcher = jiraKeyPattern.matcher(hyperlinkedSubjectLine);
                        if (jiraKeyMatcher.find()) {
                            StringBuilder stringBuilder = new StringBuilder(hyperlinkedSubjectLine);
                            stringBuilder.replace(jiraKeyMatcher.start(), jiraKeyMatcher.end(),
                                    Objects.requireNonNull(hyperlinks.pollFirst()));
                            hyperlinkedSubjectLine = stringBuilder.toString();
                        }
                    }
                } else {
                    hyperlinkedSubjectLine = subjectLine;
                }
            }
        }

        if (hyperlinkedSubjectLine.isEmpty()) {
            hyperlinkedSubjectLine = subjectLine;
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
