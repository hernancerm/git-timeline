package me.hernancerm;

import lombok.Data;

@Data
public class Commit {
    private String gitLogFullLine;
    private String fullHash;
    private String abbreviatedHash;
    private String abbreviatedParentHashes;
    private String authorName;
    private String authorDate;
    private String committerName;
    private String subjectLine;
    private String refNamesColored;
}
