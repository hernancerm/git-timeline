package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

public class GitLogPrettyPrinter {

    public void print(Commit commit) {
        System.out.println(ansi().render(
                "@|yellow " + commit.getAbbreviatedHash() + "|@"
                        + " @|green " + commit.getAuthorName() + "|@"
                        + " @|cyan " + commit.getAuthorDate() + "|@"
                        + commit.getRefNamesColored()
                        + " " + commit.getSubjectLine()));
    }
}
