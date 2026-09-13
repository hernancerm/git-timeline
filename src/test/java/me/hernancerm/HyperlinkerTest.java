package me.hernancerm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

class HyperlinkerTest {

    private static final String BEL = "\007";

    private static final UnaryOperator<String> ISSUE_URL =
            number -> "https://github.com/o/r/issues/" + number;
    private static final UnaryOperator<String> PR_URL =
            number -> "https://bitbucket.org/o/r/pull-requests/" + number;
    private static final UnaryOperator<String> JIRA_URL =
            issueKey -> "https://o.atlassian.net/browse/" + issueKey;
    private static final UnaryOperator<String> ACME_JIRA_URL =
            issueKey -> "https://acme.atlassian.net/browse/" + issueKey;

    private static final Hyperlinker HYPERLINKER = new Hyperlinker(true);

    private static String link(String url, String title) {
        return "\033]8;;" + url + BEL + title + "\033]8;;" + BEL;
    }

    @Test
    void hyperlinkIssueNumbers_givenOneNumber_thenHyperlinkIt() {
        String output = HYPERLINKER.linkIssueNumbers("Merge #382 in", ISSUE_URL);

        assertEquals("Merge " + link("https://github.com/o/r/issues/382", "#382") + " in", output);
    }

    @Test
    void hyperlinkIssueNumbers_givenManyNumbers_thenHyperlinkEveryOne() {
        String output = HYPERLINKER.linkIssueNumbers("#1 and #2 and #3", ISSUE_URL);

        assertEquals(
                link("https://github.com/o/r/issues/1", "#1") + " and "
                        + link("https://github.com/o/r/issues/2", "#2") + " and "
                        + link("https://github.com/o/r/issues/3", "#3"),
                output);
    }

    @Test
    void hyperlinkIssueNumbers_givenNoNumber_thenReturnLineUnchanged() {
        String line = "docs: clean up README.md";

        assertSame(line, HYPERLINKER.linkIssueNumbers(line, ISSUE_URL));
    }

    @Test
    void hyperlinkIssueNumbers_givenJiraHyperlinkFurtherRight_thenStillHyperlinkTheNumber() {
        // Regression: the rescanning version rejected a match that had a BEL anywhere after it,
        // so the Jira pass running first left this number unlinked.
        String jiraLinked = HYPERLINKER.linkJiraIssues("Merge #42 for ABC-123", JIRA_URL);

        String output = HYPERLINKER.linkIssueNumbers(jiraLinked, PR_URL);

        assertTrue(output.contains("bitbucket.org/o/r/pull-requests/42"), output);
        assertTrue(output.contains("o.atlassian.net/browse/ABC-123"), output);
    }

    @Test
    void hyperlinkJiraIssues_givenIssueKey_thenHyperlinkIt() {
        String output = HYPERLINKER.linkJiraIssues("Fix ABC-123 now", ACME_JIRA_URL);

        assertEquals(
                "Fix " + link("https://acme.atlassian.net/browse/ABC-123", "ABC-123") + " now",
                output);
    }

    @Test
    void hyperlinkJiraIssues_givenVersionLikeTextFurtherRight_thenReturnLineUnchanged() {
        // The `(?!.*[.]\d)` lookahead, kept from the rescanning version.
        String line = "Merge branch ABC-123 from xyz-8.2";

        assertSame(line, HYPERLINKER.linkJiraIssues(line, ACME_JIRA_URL));
    }

    @Test
    void hyperlinkJiraIssues_givenNoUrl_thenLeaveTheKeyAsPlainText() {
        // How a platform with no Jira alongside it declines to link issue keys.
        String output = HYPERLINKER.linkJiraIssues("Fix ABC-123 now", issueKey -> null);

        assertEquals("Fix ABC-123 now", output);
    }

    @Test
    void hyperlink_givenDisabled_thenReturnLineUnchanged() {
        Hyperlinker disabled = new Hyperlinker(false);

        assertEquals("Merge #382", disabled.linkIssueNumbers("Merge #382", ISSUE_URL));
        assertEquals("Fix ABC-123", disabled.linkJiraIssues("Fix ABC-123", ACME_JIRA_URL));
        assertEquals("3bb28d0",
                disabled.link("https://github.com/o/r/commit/full", "3bb28d0"));
    }
}
