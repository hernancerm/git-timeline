package me.hernancerm;

import static me.hernancerm.GitRemote.Platform.BITBUCKET_ORG;
import static me.hernancerm.GitRemote.Platform.GITHUB_COM;
import static org.jline.jansi.Ansi.ansi;

import java.util.Objects;

public class GitLogFormatter {

    // The remote is null when there is none or color is off, which leaves every hyperlink out.
    public String format(GitCommit c, GitRemote r) {
        boolean isMergeCommit = c.abbreviatedParentHashes().length > 1;
        boolean authorDiffersFromCommitter = !c.authorName().equals(c.committerName());
        return ansi().render(
                        (isMergeCommit ? "@|bold,yellow " : "@|yellow ")
                                + (r != null
                                        ? hyperlinkToCommit(r, c.fullHash(), c.abbreviatedHash())
                                        : c.abbreviatedHash())
                                + (isMergeCommit ? "*" : " ")
                                + "|@ "
                        + "@|green "
                                + c.authorDate()
                                + "|@  "
                        + (authorDiffersFromCommitter ? "@|bold,cyan " : "@|cyan ")
                                + c.authorName()
                                + (authorDiffersFromCommitter ? "*" : " ")
                                + "|@"
                        + ((r != null && BITBUCKET_ORG.equals(r.platform())
                                ? AnsiUtils.hyperlinkJiraIssues(r.ownerName(), c.refNamesColored())
                                : c.refNamesColored()))
                        + " "
                        + (r != null
                                ? hyperlinkSubjectLine(r, c.subjectLine())
                                : c.subjectLine()
                        )).toString();
    }

    private String hyperlinkToCommit(GitRemote r, String fullHash, String line) {
        Objects.requireNonNull(r, "Cannot hyperlink line when the remote is null");
        Objects.requireNonNull(fullHash, "Cannot hyperlink line when the full hash is null");
        Objects.requireNonNull(line, "Cannot hyperlink line when the line is null");

        String output = line;

        if (BITBUCKET_ORG.equals(r.platform())) {
            output = AnsiUtils.hyperlinkToBitbucketCommit(
                    r.ownerName(), r.repositoryName(), fullHash, line);
        } else if (GITHUB_COM.equals(r.platform())) {
            output = AnsiUtils.hyperlinkToGitHubCommit(
                    r.ownerName(), r.repositoryName(), fullHash, line);
        }

        return output;
    }

    private String hyperlinkSubjectLine(GitRemote r, String subjectLine) {
        Objects.requireNonNull(r, "Cannot hyperlink subject line when the remote is null");
        Objects.requireNonNull(r, "Cannot hyperlink subject line when the subject line is null");

        String output = subjectLine;

        if (BITBUCKET_ORG.equals(r.platform())) {
            output = AnsiUtils.hyperlinkJiraIssues(
                    r.ownerName(), output);
            output = AnsiUtils.hyperlinkBitbucketPrNumbers(
                    r.ownerName(), r.repositoryName(), output);
        } else if (GITHUB_COM.equals(r.platform())) {
            output = AnsiUtils.hyperlinkGitHubIssuesAndPrNumbers(
                    r.ownerName(), r.repositoryName(), output);
        }

        return output;
    }
}
