package me.hernancerm;

import static me.hernancerm.GitRemote.Platform.BITBUCKET_ORG;
import static me.hernancerm.GitRemote.Platform.GITHUB_COM;
import static org.fusesource.jansi.Ansi.ansi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitLogFormatter {

    public String format(GitCommit c) {
        boolean isMergeCommit = c.getAbbreviatedParentHashes().length > 1;
        boolean authorDiffersFromCommitter = !c.getAuthorName().equals(c.getCommitterName());
        return ansi().render(
                        (isMergeCommit ? "@|bold,yellow " : "@|italic,yellow ")
                                + c.getAbbreviatedHash()
                                + (c.getArgs().isGraphEnabled() ? " " : (isMergeCommit ? " M" : "  "))
                                + "|@ "
                        + "@|italic,green "
                                + c.getAuthorDate()
                                + "|@  "
                        + (authorDiffersFromCommitter ? "@|bold,cyan " : "@|italic,cyan ")
                                + c.getAuthorName()
                                + "|@ "
                        + ((c.getRemote() != null && BITBUCKET_ORG.equals(c.getRemote().getPlatform())
                                ? AnsiUtils.hyperlinkJiraIssues(c.getRemote().getOwnerName(), c.getRefNamesColored())
                                : c.getRefNamesColored()))
                        + " "
                        + (c.getRemote() != null
                                ? hyperlinkSubjectLine(c.getRemote(), c.getSubjectLine())
                                : c.getSubjectLine()
                        )).toString();
    }

    private String hyperlinkSubjectLine(GitRemote r, String subjectLine) {
        String output = subjectLine;

        if (BITBUCKET_ORG.equals(r.getPlatform())) {
            output = AnsiUtils.hyperlinkJiraIssues(
                    r.getOwnerName(), output);
            output = AnsiUtils.hyperlinkBitbucketPrNumbers(
                    r.getOwnerName(), r.getRepositoryName(), output);
        } else if (GITHUB_COM.equals(r.getPlatform())) {
            output = AnsiUtils.hyperlinkGitHubIssuesAndPrNumbers(
                    r.getOwnerName(), r.getRepositoryName(), output);
        }

        return output;
    }
}
