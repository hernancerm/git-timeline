package me.hernancerm;

public record GitCommit(
        String fullHash,
        String abbreviatedHash,
        String[] abbreviatedParentHashes,
        String refNamesColored,
        String committerName,
        String authorName,
        String authorDate,
        String subjectLine) {}
