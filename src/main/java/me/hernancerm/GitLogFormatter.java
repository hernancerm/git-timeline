package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

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
        String output = subjectLine;

        Pattern remoteUrlPattern = Pattern.compile("@(.*?):(.*?)/(.*?)[.]git");
        Matcher originRemoteMatcher = remoteUrlPattern.matcher(originRemote);

        if (originRemoteMatcher.find()) {
            String platform = originRemoteMatcher.group(1);
            String user = originRemoteMatcher.group(2);
            String repo = originRemoteMatcher.group(3);

            if (platform.equals("bitbucket.org")) {

                // Hyperlink Jira keys.
                //
                //   SSH remote origin URL:  git@bitbucket.org:<user>/<repo>.git
                //   Jira key URL:           https://<user>.atlassian.net/browse/<jira-key>
                //   <jira-key>:             Regex: [A-Z]+-\d+    E.g.: ABC-123

                String nonHyperlinkedJiraKeyRegex = "([A-Z]+-\\d+)(?!.*\007)";
                Pattern jiraKeyPattern = Pattern.compile(nonHyperlinkedJiraKeyRegex);
                Matcher outputMatcher = jiraKeyPattern.matcher(output);

                while (outputMatcher.find()) {
                    StringBuilder stringBuilder = new StringBuilder(output);
                    String hyperlink = buildHyperlink(String.format("https://%s.atlassian.net/browse/%s",
                            user, outputMatcher.group(1)), outputMatcher.group(1));
                    stringBuilder.replace(outputMatcher.start(), outputMatcher.end(), hyperlink);
                    output = stringBuilder.toString();
                    outputMatcher = jiraKeyPattern.matcher(output);
                }

                // Hyperlink Bitbucket PR numbers.
                //
                // Bitbucket URL:  https://bitbucket.org/<user>/<repo>/pull-requests/<pr-number>
                // <pr-number>:    Regex: #\d+    E.g.: #123

                String nonHyperlinkedBbPrNumberRegex = "#(\\d+)(?!.*\\007)";
                Pattern bbPrNumberPattern = Pattern.compile(nonHyperlinkedBbPrNumberRegex);
                outputMatcher = bbPrNumberPattern.matcher(output);

                while (outputMatcher.find()) {
                    StringBuilder stringBuilder = new StringBuilder(output);
                    String hyperlink = buildHyperlink(String.format("https://bitbucket.org/%s/%s/pull-requests/%s",
                            user, repo, outputMatcher.group(1)), "#" + outputMatcher.group(1));
                    stringBuilder.replace(outputMatcher.start(), outputMatcher.end(), hyperlink);
                    output = stringBuilder.toString();
                    outputMatcher = jiraKeyPattern.matcher(output);
                }
            }
        }

        return output;
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
