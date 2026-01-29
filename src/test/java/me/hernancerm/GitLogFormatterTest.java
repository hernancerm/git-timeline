package me.hernancerm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitLogFormatterTest {

    private GitLogFormatter gitLogFormatter;

    @BeforeEach
    void setUp() {
        gitLogFormatter = new GitLogFormatter();
    }

    @Test
    void format_givenCommitWithRemote_thenFormatCommit() {
        // Given
        GitCommit commit = getCommit();

        // When
        String formattedCommit = gitLogFormatter.format(commit);

        // Then
        assertNotNull(formattedCommit);
    }

    @Test
    void format_givenCommitWithNullRemote_thenFormatCommit() {
        // Given
        GitCommit commit = getCommit();
        commit.setRemote(null);

        // When
        String formattedCommit = gitLogFormatter.format(commit);

        // Then
        assertNotNull(formattedCommit);
    }

    private GitCommit getCommit() {
        GitRemote remote = new GitRemote(
                GitRemote.Platform.BITBUCKET_ORG,
                "test-repo",
                "hernancerm");

        GitCommit commit = new GitCommit();
        commit.setFullHash("3bb28d0d0d1c978894e22c5206c0d1f07f5b9071");
        commit.setAbbreviatedHash("3bb28d0");
        commit.setAbbreviatedParentHashes(new String[]{"0816af9"});
        commit.setAuthorName("Hernán Cervera");
        commit.setAuthorDate("2025-12-31");
        commit.setCommitterName("Hernán Cervera");
        commit.setSubjectLine("Test commit");
        commit.setRefNamesColored("");
        commit.setRemote(remote);
        commit.setArgs(null);
        return commit;
    }
}
