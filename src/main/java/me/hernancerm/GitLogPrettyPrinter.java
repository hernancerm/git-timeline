package me.hernancerm;

import static org.fusesource.jansi.Ansi.ansi;

public class GitLogPrettyPrinter {

    public void print(GitCommit commit) {
        String gitLogFullLine = commit.getGitLogFullLine();
        int indexOfSerializedCommit = gitLogFullLine.indexOf('{');
        System.out.println(ansi().render(
                // Substring required due to the option `--graph` of git-log.
                gitLogFullLine.substring(0, indexOfSerializedCommit)
                        + "@|yellow " + commit.getAbbreviatedHash() + "|@"
                        + " @|green " + commit.getAuthorName() + "|@"
                        + " @|cyan " + commit.getAuthorDate() + "|@"
                        + commit.getRefNamesColored()
                        + " " + commit.getSubjectLine()));
    }
}
