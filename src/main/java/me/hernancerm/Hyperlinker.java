package me.hernancerm;

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds terminal hyperlinks. Whether they are wanted is settled once, when this is built, so
 * that a run without color needs no flag reachable from anywhere else.
 */
public class Hyperlinker {

    // Jira issue key. E.g.: ABC-123
    // The lookahead keeps version-like text, e.g. `xyz-8.2`, from being linked.
    private static final Pattern JIRA_ISSUE_KEY = Pattern.compile("([A-Z]+-\\d+)(?!.*[.]\\d)");

    // Issue or pull request number. E.g.: #123
    private static final Pattern ISSUE_NUMBER = Pattern.compile("#(\\d+)");

    private final boolean enabled;

    public Hyperlinker(boolean enabled) {
        this.enabled = enabled;
    }

    /** Wraps the title in a hyperlink to the url. */
    public String link(String url, String title) {
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

    /** Hyperlinks every Jira issue key in the line, e.g. `ABC-123`. */
    public String linkJiraIssues(String line, UnaryOperator<String> toUrl) {
        return linkEveryMatch(JIRA_ISSUE_KEY, line, toUrl, issueKey -> issueKey);
    }

    /** Hyperlinks every issue or pull request number in the line, e.g. `#123`. */
    public String linkIssueNumbers(String line, UnaryOperator<String> toUrl) {
        return linkEveryMatch(ISSUE_NUMBER, line, toUrl, number -> "#" + number);
    }

    // Replaces capture group 1 of every match with a hyperlink, in a single pass. A match
    // whose url is null is left as plain text, which is how a platform declines to link one
    // kind of id.
    // appendReplacement moves past what it just wrote, so a hyperlink is never rescanned. The
    // rescanning version needed a `(?!.*\007)` lookahead to avoid linking its own output, which
    // also made it skip a match whenever an earlier pass had left a hyperlink further right.
    private String linkEveryMatch(
            Pattern pattern,
            String line,
            UnaryOperator<String> toUrl,
            UnaryOperator<String> toTitle
    ) {
        if (!enabled) {
            return line;
        }

        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            return line;
        }

        StringBuilder output = new StringBuilder(line.length());
        do {
            String id = matcher.group(1);
            String url = toUrl.apply(id);
            matcher.appendReplacement(output, Matcher.quoteReplacement(url == null
                    ? matcher.group()
                    : link(url, toTitle.apply(id))));
        } while (matcher.find());
        matcher.appendTail(output);

        return output.toString();
    }
}
