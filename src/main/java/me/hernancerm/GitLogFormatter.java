package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

public class GitLogFormatter {

    public String format(GitCommit commit) {
        boolean isMergeCommit = commit.getAbbreviatedParentHashes().length > 1;
        boolean authorDiffersFromCommitter = !commit.getAuthorName().equals(
                commit.getCommitterName());
        return ansi().render(
                        (isMergeCommit ? "@|bold,yellow " : "@|italic,yellow ")
                                + commit.getAbbreviatedHash()
                                + "|@ "
                        + "@|italic,green "
                                + commit.getAuthorDate()
                                + "|@ "
                        + (authorDiffersFromCommitter ? "@|bold,cyan " : "@|italic,cyan ")
                                + commit.getAuthorName()
                                + "|@ "
                        + commit.getRefNamesColored()
                        + " " + commit.getSubjectLine()
                        + " " + buildHyperlink("http://example.com", "This is a link"))
                .toString();
    }

    private String buildHyperlink(String url, String title) {
        return "\033]8;;" + url + "\007" + title + "\033]8;;\007";
    }
}
