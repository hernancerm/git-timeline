package me.hernancerm;

import lombok.Data;

@Data
public class GitCommit {

    private String abbreviatedHash;
    private String[] abbreviatedParentHashes;
    private String authorName;
    private String authorDate;
    private String committerName;
    private String subjectLine;
    private String refNamesColored;
    private GitRemote gitRemote;

    public void reset() {
        abbreviatedHash = null;
        abbreviatedParentHashes = null;
        authorName = null;
        authorDate = null;
        committerName = null;
        subjectLine = null;
        refNamesColored = null;
        gitRemote = null;
    }
}
