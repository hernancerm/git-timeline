package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

public class GitLogPrettyPrinter {

    public void print(Commit commit) {
        String gitLogFullLine = commit.getGitLogFullLine();
        int indexOfSerializedCommit = gitLogFullLine.indexOf('{');
        System.out.println(ansi().render(
                gitLogFullLine.substring(0, indexOfSerializedCommit)
                        + "@|yellow " + commit.getAbbreviatedHash() + "|@"
                        + " @|green " + commit.getAuthorName() + "|@"
                        + " @|cyan " + commit.getAuthorDate() + "|@"
                        + commit.getRefNamesColored()
                        + " " + commit.getSubjectLine()));
    }
}
