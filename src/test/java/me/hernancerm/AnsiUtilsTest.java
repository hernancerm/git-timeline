package me.hernancerm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AnsiUtilsTest {

    private static final String BEL = "\007";

    @AfterEach
    void tearDown() {
        AnsiUtils.setEnabled(true);
    }

    private static String link(String url, String title) {
        return "\033]8;;" + url + BEL + title + "\033]8;;" + BEL;
    }

    @Test
    void hyperlinkGitHubIssuesAndPrNumbers_givenOneNumber_thenHyperlinkIt() {
        String output = AnsiUtils.hyperlinkGitHubIssuesAndPrNumbers("o", "r", "Merge #382 in");

        assertEquals("Merge " + link("https://github.com/o/r/issues/382", "#382") + " in", output);
    }

    @Test
    void hyperlinkGitHubIssuesAndPrNumbers_givenManyNumbers_thenHyperlinkEveryOne() {
        String output = AnsiUtils.hyperlinkGitHubIssuesAndPrNumbers("o", "r", "#1 and #2 and #3");

        assertEquals(
                link("https://github.com/o/r/issues/1", "#1") + " and "
                        + link("https://github.com/o/r/issues/2", "#2") + " and "
                        + link("https://github.com/o/r/issues/3", "#3"),
                output);
    }

    @Test
    void hyperlinkGitHubIssuesAndPrNumbers_givenNoNumber_thenReturnLineUnchanged() {
        String line = "docs: clean up README.md";

        assertSame(line, AnsiUtils.hyperlinkGitHubIssuesAndPrNumbers("o", "r", line));
    }

    @Test
    void hyperlinkBitbucketPrNumbers_givenJiraHyperlinkFurtherRight_thenStillHyperlinkTheNumber() {
        // Regression: the rescanning version rejected a match that had a BEL anywhere after it,
        // so the Jira pass running first left this number unlinked.
        String jiraLinked = AnsiUtils.hyperlinkJiraIssues("o", "Merge #42 for ABC-123");

        String output = AnsiUtils.hyperlinkBitbucketPrNumbers("o", "r", jiraLinked);

        assertTrue(output.contains("bitbucket.org/o/r/pull-requests/42"), output);
        assertTrue(output.contains("o.atlassian.net/browse/ABC-123"), output);
    }

    @Test
    void hyperlinkJiraIssues_givenIssueKey_thenHyperlinkIt() {
        String output = AnsiUtils.hyperlinkJiraIssues("acme", "Fix ABC-123 now");

        assertEquals(
                "Fix " + link("https://acme.atlassian.net/browse/ABC-123", "ABC-123") + " now",
                output);
    }

    @Test
    void hyperlinkJiraIssues_givenVersionLikeTextFurtherRight_thenReturnLineUnchanged() {
        // The `(?!.*[.]\d)` lookahead, kept from the rescanning version.
        String line = "Merge branch ABC-123 from xyz-8.2";

        assertSame(line, AnsiUtils.hyperlinkJiraIssues("acme", line));
    }

    @Test
    void hyperlink_givenAnsiDisabled_thenReturnLineUnchanged() {
        AnsiUtils.setEnabled(false);

        assertEquals("Merge #382", AnsiUtils.hyperlinkGitHubIssuesAndPrNumbers("o", "r", "Merge #382"));
        assertEquals("Fix ABC-123", AnsiUtils.hyperlinkJiraIssues("acme", "Fix ABC-123"));
        assertEquals("3bb28d0", AnsiUtils.hyperlinkToGitHubCommit("o", "r", "full", "3bb28d0"));
        assertEquals("3bb28d0", AnsiUtils.hyperlinkToBitbucketCommit("o", "r", "full", "3bb28d0"));
    }

    @Test
    void hyperlinkToCommit_givenOwnerAndRepository_thenBuildUrl() {
        assertEquals(
                link("https://github.com/o/r/commit/3bb28d0d0d", "3bb28d0"),
                AnsiUtils.hyperlinkToGitHubCommit("o", "r", "3bb28d0d0d", "3bb28d0"));
        assertEquals(
                link("https://bitbucket.org/o/r/commits/3bb28d0d0d", "3bb28d0"),
                AnsiUtils.hyperlinkToBitbucketCommit("o", "r", "3bb28d0d0d", "3bb28d0"));
    }
}
