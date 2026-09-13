package me.hernancerm;

import static org.jline.jansi.Ansi.ansi;

public class GitLogFormatter {

    // The remote is null when there is none or color is off, which leaves every hyperlink out.
    public String format(GitCommit c, GitRemote r) {
        boolean isMergeCommit = c.abbreviatedParentHashes().length > 1;
        boolean authorDiffersFromCommitter = !c.authorName().equals(c.committerName());
        return ansi().render(
                        (isMergeCommit ? "@|bold,yellow " : "@|yellow ")
                                + hyperlinkToCommit(r, c)
                                + (isMergeCommit ? "*" : " ")
                                + "|@ "
                        + "@|green "
                                + c.authorDate()
                                + "|@  "
                        + (authorDiffersFromCommitter ? "@|bold,cyan " : "@|cyan ")
                                + c.authorName()
                                + (authorDiffersFromCommitter ? "*" : " ")
                                + "|@"
                        + hyperlinkRefNames(r, c.refNamesColored())
                        + " "
                        + hyperlinkSubjectLine(r, c.subjectLine())).toString();
    }

    private String hyperlinkToCommit(GitRemote r, GitCommit c) {
        if (r == null) {
            return c.abbreviatedHash();
        }
        return AnsiUtils.buildHyperlink(r.commitUrl(c.fullHash()), c.abbreviatedHash());
    }

    // A ref name can hold a Jira issue key, e.g. a branch named `ABC-123`.
    private String hyperlinkRefNames(GitRemote r, String refNamesColored) {
        if (r == null) {
            return refNamesColored;
        }
        return AnsiUtils.hyperlinkJiraIssues(refNamesColored, r::jiraIssueUrl);
    }

    private String hyperlinkSubjectLine(GitRemote r, String subjectLine) {
        if (r == null) {
            return subjectLine;
        }
        // A platform with no Jira alongside it yields no url, which leaves the key as plain text.
        String output = AnsiUtils.hyperlinkJiraIssues(subjectLine, r::jiraIssueUrl);
        return AnsiUtils.hyperlinkIssueNumbers(output, r::issueUrl);
    }
}
