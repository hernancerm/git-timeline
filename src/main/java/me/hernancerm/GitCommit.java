package me.hernancerm;

import lombok.Data;

@Data
public class GitCommit {
    private String gitLogFullLine;
    private String fullHash;
    private String abbreviatedHash;
    private String abbreviatedParentHashes;
    private String committerName;
    private String authorName;
    private String authorDate;
    private String refNamesColored;
    private String subjectLine;
}
