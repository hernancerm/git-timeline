package me.hernancerm.render;

import me.hernancerm.git.GitCommit;
import me.hernancerm.git.GitRemote;

import static org.jline.jansi.Ansi.ansi;

public class GitLogFormatter {

    private final Hyperlinker hyperlinker;

    public GitLogFormatter(Hyperlinker hyperlinker) {
        this.hyperlinker = hyperlinker;
    }

    // A null remote leaves every hyperlink out.
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
        return hyperlinker.link(r.commitUrl(c.fullHash()), c.abbreviatedHash());
    }

    // A ref name can hold a Jira issue key, e.g. a branch named `ABC-123`.
    private String hyperlinkRefNames(GitRemote r, String refNamesColored) {
        if (r == null) {
            return refNamesColored;
        }
        return hyperlinker.linkJiraIssues(refNamesColored, r::jiraIssueUrl);
    }

    private String hyperlinkSubjectLine(GitRemote r, String subjectLine) {
        if (r == null) {
            return subjectLine;
        }
        // No Jira url leaves the key as plain text.
        String output = hyperlinker.linkJiraIssues(subjectLine, r::jiraIssueUrl);
        return hyperlinker.linkIssueNumbers(output, r::issueUrl);
    }
}
