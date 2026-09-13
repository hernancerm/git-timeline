package me.hernancerm;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsiUtils {

    // Jira issue key. E.g.: ABC-123
    // The lookahead keeps version-like text, e.g. `xyz-8.2`, from being linked.
    private static final Pattern JIRA_ISSUE_KEY = Pattern.compile("([A-Z]+-\\d+)(?!.*[.]\\d)");

    // Issue or pull request number. E.g.: #123
    private static final Pattern ISSUE_NUMBER = Pattern.compile("#(\\d+)");

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

        // Jira URL:    https://<owner>.atlassian.net/browse/<jira-key>

        if (!enabled) {
            return line;
        }

        return hyperlinkEveryMatch(JIRA_ISSUE_KEY, line,
                key -> "https://" + bitbucketOwner + ".atlassian.net/browse/" + key,
                key -> key);
    }

    public static String hyperlinkBitbucketPrNumbers(
            String bitbucketOwner,
            String bitbucketRepository,
            String line
    ) {

        // Bitbucket URL:  https://bitbucket.org/<owner>/<repo>/pull-requests/<pr-number>

        if (!enabled) {
            return line;
        }

        return hyperlinkEveryMatch(ISSUE_NUMBER, line,
                number -> "https://bitbucket.org/" + bitbucketOwner + "/" + bitbucketRepository
                        + "/pull-requests/" + number,
                number -> "#" + number);
    }

    public static String hyperlinkGitHubIssuesAndPrNumbers(
            String gitHubOwner,
            String gitHubRepository,
            String line
    ) {

        // How this method links both issues and PR numbers?:
        // A GitHub issue URL redirects to a PR if the id matches a PR instead of an issue.

        // GitHub URL:  https://github.com/<owner>/<repo>/issues/<issue-number>

        if (!enabled) {
            return line;
        }

        return hyperlinkEveryMatch(ISSUE_NUMBER, line,
                number -> "https://github.com/" + gitHubOwner + "/" + gitHubRepository
                        + "/issues/" + number,
                number -> "#" + number);
    }

    public static String hyperlinkToGitHubCommit(
            String gitHubOwner,
            String gitHubRepository,
            String fullHash,
            String line) {

        if (!enabled) {
            return line;
        }

        return buildHyperlink(
                "https://github.com/" + gitHubOwner + "/" + gitHubRepository
                        + "/commit/" + fullHash,
                line);
    }

    public static String hyperlinkToBitbucketCommit(
            String bitbucketOwner,
            String bitbucketRepository,
            String fullHash,
            String line) {

        if (!enabled) {
            return line;
        }

        return buildHyperlink(
                "https://bitbucket.org/" + bitbucketOwner + "/" + bitbucketRepository
                        + "/commits/" + fullHash,
                line);
    }

    // Replaces capture group 1 of every match with a hyperlink, in a single pass.
    // appendReplacement moves past what it just wrote, so a hyperlink is never rescanned. The
    // rescanning version needed a `(?!.*\007)` lookahead to avoid linking its own output, which
    // also made it skip a match whenever an earlier pass had left a hyperlink further right.
    private static String hyperlinkEveryMatch(
            Pattern pattern,
            String line,
            Function<String, String> toUrl,
            Function<String, String> toTitle
    ) {
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            return line;
        }

        StringBuilder output = new StringBuilder(line.length());
        do {
            String id = matcher.group(1);
            matcher.appendReplacement(output,
                    Matcher.quoteReplacement(buildHyperlink(toUrl.apply(id), toTitle.apply(id))));
        } while (matcher.find());
        matcher.appendTail(output);

        return output.toString();
    }
}
