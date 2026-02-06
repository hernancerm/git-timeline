package me.hernancerm;

import static me.hernancerm.GitRemote.Platform.BITBUCKET_ORG;
import static me.hernancerm.GitRemote.Platform.GITHUB_COM;
import static org.jline.jansi.Ansi.ansi;

import java.util.Objects;

public class GitLogFormatter {

    public String format(GitCommit c) {
        boolean isMergeCommit = c.getAbbreviatedParentHashes().length > 1;
        boolean authorDiffersFromCommitter = !c.getAuthorName().equals(c.getCommitterName());
        return ansi().render(
                        (isMergeCommit ? "@|bold,yellow " : "@|yellow ")
                                + (c.getRemote() != null
                                        ? hyperlinkToCommit(c.getRemote(), c.getFullHash(), c.getAbbreviatedHash())
                                        : c.getAbbreviatedHash())
                                + (isMergeCommit ? "*" : " ")
                                + "|@ "
                        + "@|green "
                                + c.getAuthorDate()
                                + "|@  "
                        + (authorDiffersFromCommitter ? "@|bold,cyan " : "@|cyan ")
                                + c.getAuthorName()
                                + (authorDiffersFromCommitter ? "*" : " ")
                                + "|@"
                        + ((c.getRemote() != null && BITBUCKET_ORG.equals(c.getRemote().platform())
                                ? AnsiUtils.hyperlinkJiraIssues(c.getRemote().ownerName(), c.getRefNamesColored())
                                : c.getRefNamesColored()))
                        + " "
                        + (c.getRemote() != null
                                ? hyperlinkSubjectLine(c.getRemote(), c.getSubjectLine())
                                : c.getSubjectLine()
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
