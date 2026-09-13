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
        GitRemote remote = new GitRemote(
                GitRemote.Platform.BITBUCKET_ORG,
                "test-repo",
                "hernancerm");

        // When
        String formattedCommit = gitLogFormatter.format(getCommit(), remote);

        // Then
        assertNotNull(formattedCommit);
    }

    @Test
    void format_givenCommitWithNullRemote_thenFormatCommit() {
        // When
        String formattedCommit = gitLogFormatter.format(getCommit(), null);

        // Then
        assertNotNull(formattedCommit);
    }

    private GitCommit getCommit() {
        return new GitCommit(
                "3bb28d0d0d1c978894e22c5206c0d1f07f5b9071",
                "3bb28d0",
                new String[]{"0816af9"},
                "",
                "Hernán Cervera",
                "Hernán Cervera",
                "2025-12-31",
                "Test commit");
    }
}
