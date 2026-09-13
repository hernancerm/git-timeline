package me.hernancerm.render;

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds terminal hyperlinks. Whether they are wanted is settled once, at construction. */
public class Hyperlinker {

    // Jira issue key, e.g. `ABC-123`. The lookahead rejects version-like text, e.g. `xyz-8.2`.
    private static final Pattern JIRA_ISSUE_KEY = Pattern.compile("([A-Z]+-\\d+)(?!.*[.]\\d)");

    // Issue or pull request number, e.g. `#123`.
    private static final Pattern ISSUE_NUMBER = Pattern.compile("#(\\d+)");

    private final boolean enabled;

    public Hyperlinker(boolean enabled) {
        this.enabled = enabled;
    }

    /** Wraps the title in a hyperlink to the url. */
    public String link(String url, String title) {
        if (enabled) {
            // OSC 8 hyperlink: https://unix.stackexchange.com/a/437585
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

    // Replaces capture group 1 of every match in one pass. A null url leaves the match as
    // plain text. appendReplacement moves past what it wrote, so a hyperlink is never
    // rescanned. The rescanning version needed a `(?!.*\007)` lookahead not to link its own
    // output, which also skipped any match with a hyperlink further right.
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
