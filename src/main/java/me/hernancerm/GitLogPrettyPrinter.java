package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

public class GitLogPrettyPrinter {

    // TODO: Colored output when expected (colored on terminal, not colored on non-TTY target).
    public void print(Commit commit) {
        System.out.print(ansi().render("@|yellow " + commit.getAbbreviatedHash() + "|@ "));
        System.out.print(ansi().render("@|green " + commit.getAuthorName() + "|@ "));
        System.out.print(ansi().render("@|cyan " + commit.getAuthorDate() + "|@ "));
        System.out.print(ansi().render(commit.getRefNamesColored()));
        System.out.print(ansi().render(commit.getSubjectLine()));
        System.out.println();
    }
}
