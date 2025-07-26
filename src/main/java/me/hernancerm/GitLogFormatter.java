package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

public class GitLogFormatter {

    public String format(GitCommit commit) {
        return ansi().render("@|yellow " + commit.getAbbreviatedHash() + "|@"
                        + " @|green " + commit.getAuthorName() + "|@"
                        + " @|cyan " + commit.getAuthorDate() + "|@"
                        + commit.getRefNamesColored()
                        + " " + commit.getSubjectLine())
                .toString();
    }
}
