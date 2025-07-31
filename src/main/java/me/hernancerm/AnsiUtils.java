package me.hernancerm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsiUtils {

    private static boolean enabled = true;

    private AnsiUtils() {
    }

    public static void setEnabled(boolean enabled) {
        AnsiUtils.enabled = enabled;
    }

    public static String buildHyperlink(String url, String title) {
        if (enabled) {
            // https://unix.stackexchange.com/a/437585
            // To get the octal escape sequences for '\e', '\a', etc., do this:
            // 1. $ echo -n '\e' > _.txt
            // 2. $ nvim _.txt
            // 3. ga
            return "\033]8;;" + url + "\007" + title + "\033]8;;\007";
        } else {
            return title;
        }
    }

    public static String hyperlinkJiraIssues(String bitbucketOwner, String line) {

        //   Jira URL:               https://<owner>.atlassian.net/browse/<jira-key>
        //   <jira-key>:             Regex: [A-Z]+-\d+    E.g.: ABC-123

        if (!enabled) {
            return line;
        }

        String output = line;
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

    public static String hyperlinkBitbucketPrNumbers(
            String bitbucketOwner,
            String bitbucketRepository,
            String line
    ) {

        // Bitbucket URL:  https://bitbucket.org/<owner>/<repo>/pull-requests/<pr-number>
        // <pr-number>:    Regex: #\d+    E.g.: #123

        if (!enabled) {
            return line;
        }

        String output = line;
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

    public static String hyperlinkGitHubIssuesAndPrNumbers(
            String gitHubOwner,
            String gitHubRepository,
            String line
    ) {

        // How this method links both issues and PR numbers?:
        // A GitHub issue URL redirects to a PR if the id matches a PR instead of an issue.

        // GitHub URL:      https://github.com/<owner>/<repo>/issues/<issue-number>
        // <issue-number>:  Regex: #\d+    E.g.: #123

        if (!enabled) {
            return line;
        }

        String output = line;
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
