package me.hernancerm;

import static me.hernancerm.GitRemote.Platform.BITBUCKET_ORG;
import static me.hernancerm.GitRemote.Platform.GITHUB_COM;
import static org.fusesource.jansi.Ansi.ansi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitLogFormatter {

    public String format(GitCommit c) {
        boolean isMergeCommit = c.getAbbreviatedParentHashes().length > 1;
        boolean authorDiffersFromCommitter = !c.getAuthorName().equals(c.getCommitterName());
        return ansi().render(
                        (isMergeCommit ? "@|bold,yellow " : "@|italic,yellow ")
                                + c.getAbbreviatedHash()
                                + "|@ "
                        + "@|italic,green "
                                + c.getAuthorDate()
                                + "|@ "
                        + (authorDiffersFromCommitter ? "@|bold,cyan " : "@|italic,cyan ")
                                + c.getAuthorName()
                                + "|@ "
                        + ((c.getRemote() != null && BITBUCKET_ORG.equals(c.getRemote().getPlatform())
                                ? hyperlinkSubjectLineJiraIssues(c.getRemote().getOwnerName(), c.getRefNamesColored())
                                : c.getRefNamesColored()))
                        + " "
                        + (c.getRemote() != null
                                ? hyperlinkSubjectLine(c.getRemote(), c.getSubjectLine())
                                : c.getSubjectLine()
                        )).toString();
    }

    private String hyperlinkSubjectLine(GitRemote gitRemote, String subjectLine) {
        String output = subjectLine;

        if (BITBUCKET_ORG.equals(gitRemote.getPlatform())) {
            output = hyperlinkSubjectLineJiraIssues(
                    gitRemote.getOwnerName(), output);
            output = hyperlinkSubjectLineBitbucketPrNumbers(
                    gitRemote.getOwnerName(), gitRemote.getRepositoryName(), output);
        } else if (GITHUB_COM.equals(gitRemote.getPlatform())) {
            output = hyperlinkSubjectLineGitHubIssuesAndPrNumbers(
                    gitRemote.getOwnerName(), gitRemote.getRepositoryName(), output);
        }

        return output;
    }

    private String hyperlinkSubjectLineJiraIssues(String bitbucketOwner, String subjectLine) {

        //   Jira URL:               https://<owner>.atlassian.net/browse/<jira-key>
        //   <jira-key>:             Regex: [A-Z]+-\d+    E.g.: ABC-123

        String output = subjectLine;
        String nonHyperlinkedJiraIssueKeyRegex = "([A-Z]+-\\d+)(?!.*\007)";
        Pattern pattern = Pattern.compile(nonHyperlinkedJiraIssueKeyRegex);
        Matcher matcher = pattern.matcher(output);

        while (matcher.find()) {
            StringBuilder stringBuilder = new StringBuilder(output);
            String hyperlink = AnsiUtils.buildHyperlink(String.format(
                    "https://%s.atlassian.net/browse/%s",
                    bitbucketOwner, matcher.group(1)),
                    matcher.group(1));
            stringBuilder.replace(matcher.start(), matcher.end(), hyperlink);
            output = stringBuilder.toString();
            matcher = pattern.matcher(output);
        }

        return output;
    }

    private String hyperlinkSubjectLineBitbucketPrNumbers(
            String bitbucketOwner,
            String bitbucketRepository,
            String subjectLine
    ) {

        // Bitbucket URL:  https://bitbucket.org/<owner>/<repo>/pull-requests/<pr-number>
        // <pr-number>:    Regex: #\d+    E.g.: #123

        String output = subjectLine;
        String nonHyperlinkedBitbucketPrNumberRegex = "#(\\d+)(?!.*\\007)";
        Pattern pattern = Pattern.compile(nonHyperlinkedBitbucketPrNumberRegex);
        Matcher matcher = pattern.matcher(output);

        while (matcher.find()) {
            StringBuilder stringBuilder = new StringBuilder(output);
            String hyperlink = AnsiUtils.buildHyperlink(String.format(
                    "https://bitbucket.org/%s/%s/pull-requests/%s",
                    bitbucketOwner, bitbucketRepository, matcher.group(1)),
                    "#" + matcher.group(1));
            stringBuilder.replace(matcher.start(), matcher.end(), hyperlink);
            output = stringBuilder.toString();
            matcher = pattern.matcher(output);
        }

        return output;
    }

    private String hyperlinkSubjectLineGitHubIssuesAndPrNumbers(
            String gitHubOwner,
            String gitHubRepository,
            String subjectLine
    ) {

        // How this method links both issues and PR numbers?:
        // A GitHub issue URL redirects to a PR if the id matches a PR instead of an issue.

        // GitHub URL:      https://github.com/<owner>/<repo>/issues/<issue-number>
        // <issue-number>:  Regex: #\d+    E.g.: #123

        String output = subjectLine;
        String nonHyperlinkedGitHubIssueNumberRegex = "#(\\d+)(?!.*\\007)";
        Pattern pattern = Pattern.compile(nonHyperlinkedGitHubIssueNumberRegex);
        Matcher matcher = pattern.matcher(output);

        while (matcher.find()) {
            StringBuilder stringBuilder = new StringBuilder(output);
            String hyperlink = AnsiUtils.buildHyperlink(String.format(
                            "https://github.com/%s/%s/issues/%s",
                            gitHubOwner, gitHubRepository, matcher.group(1)),
                    "#" + matcher.group(1));
            stringBuilder.replace(matcher.start(), matcher.end(), hyperlink);
            output = stringBuilder.toString();
            matcher = pattern.matcher(output);
        }

        return output;
    }
}
