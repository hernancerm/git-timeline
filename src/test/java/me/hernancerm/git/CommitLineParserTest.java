package me.hernancerm.git;

import static me.hernancerm.git.CommitLineParser.DELIMITER;
import static me.hernancerm.git.CommitLineParser.splitCommitLine;
import static me.hernancerm.git.CommitLineParser.toCommit;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommitLineParserTest {


    private static final String FULL_HASH = "3bb28d0d0d1c978894e22c5206c0d1f07f5b9071";
    private static final String ABBREVIATED_HASH = "3bb28d0";

    /** Builds a git-log line the way PRETTY_FORMAT does, with an optional `--graph` prefix. */
    private static String line(String graphPrefix, String refNames, String authorName,
            String authorDate, String subjectLine) {
        return graphPrefix
                + DELIMITER + FULL_HASH
                + DELIMITER + ABBREVIATED_HASH
                + DELIMITER + "0816af9"
                + DELIMITER + refNames
                + DELIMITER + "Hernan Cervera"
                + DELIMITER + authorName
                + DELIMITER + authorDate
                + DELIMITER + subjectLine;
    }

    private static GitCommit parse(String line) {
        String[] parts = splitCommitLine(line);
        assertNotNull(parts, "Expected the line to carry commit data");
        return toCommit(parts);
    }

    @Test
    void splitCommitLine_givenPlainCommit_thenPopulateEveryField() {
        GitCommit commit = parse(line("", "", "Hernan Cervera", "Dec-31-2025", "Test commit"));

        assertEquals(FULL_HASH, commit.fullHash());
        assertEquals(ABBREVIATED_HASH, commit.abbreviatedHash());
        assertArrayEquals(new String[]{"0816af9"}, commit.abbreviatedParentHashes());
        assertEquals("Hernan Cervera", commit.authorName());
        assertEquals("Dec-31-2025", commit.authorDate());
        assertEquals("Test commit", commit.subjectLine());
    }

    @Test
    void splitCommitLine_givenGraphPrefix_thenKeepPrefixAsFirstPart() {
        String[] parts = splitCommitLine(line("| * ", "", "Hernan Cervera", "Dec-31-2025", "Sub"));

        assertNotNull(parts);
        assertEquals("| * ", parts[0]);
    }

    @Test
    void splitCommitLine_givenGraphConnectorLine_thenReturnNull() {
        assertNull(splitCommitLine("|\\"));
        assertNull(splitCommitLine("| |/"));
        assertNull(splitCommitLine(""));
    }

    @Test
    void splitCommitLine_givenIncompleteLine_thenReturnNull() {
        // A line with too few fields is passed through instead of being half parsed.
        assertNull(splitCommitLine(DELIMITER + FULL_HASH + DELIMITER + ABBREVIATED_HASH));
    }

    @Test
    void splitCommitLine_givenDelimiterInSubjectLine_thenKeepSubjectLineWhole() {
        // The subject line is the only field that can hold the delimiter. It is last, so it takes
        // the rest of the line rather than shifting the fields after it.
        String subjectLine = "evil " + DELIMITER + " tail";
        GitCommit commit = parse(line("", "", "Hernan Cervera", "Dec-31-2025", subjectLine));

        assertEquals(subjectLine, commit.subjectLine());
        assertEquals("Dec-31-2025", commit.authorDate());
        assertEquals(FULL_HASH, commit.fullHash());
    }

    @Test
    void splitCommitLine_givenTagsOfTheOldFormatInSubjectLine_thenKeepSubjectLineWhole() {
        // Regression: these tags used to delimit the fields, so they broke the parse.
        String subjectLine = "evil </hernancerm.git-timeline.subject-line> tail";
        GitCommit commit = parse(line("", "", "Hernan Cervera", "Dec-31-2025", subjectLine));

        assertEquals(subjectLine, commit.subjectLine());
    }

    @Test
    void splitCommitLine_givenAngleBracketsInRefNames_thenParseUnaffected() {
        // Ref names may hold '<' and '>' but never the 0x1F of the delimiter, so a branch named
        // like a field boundary cannot shift the fields.
        String refNames = " (HEAD -> main, x</hernancerm.git-timeline.ref-names-colored>y)";
        GitCommit commit = parse(line("", refNames, "Hernan Cervera", "Dec-31-2025", "Test commit"));

        assertEquals(refNames, commit.refNamesColored());
        assertEquals("Hernan Cervera", commit.authorName());
        assertEquals("Test commit", commit.subjectLine());
    }

    @Test
    void splitCommitLine_givenControlByteInAuthorName_thenParseUnaffected() {
        // Ident names may hold the 0x1F but never the '<' of the delimiter, which holds even for a
        // hand crafted commit object: git cuts the name at the first '<'.
        String authorName = "EVIL\u001FNAME";
        GitCommit commit = parse(line("", "", authorName, "Dec-31-2025", "Test commit"));

        assertEquals(authorName, commit.authorName());
        assertEquals("Dec-31-2025", commit.authorDate());
        assertEquals("Test commit", commit.subjectLine());
    }

    @Test
    void splitCommitLine_givenMergeCommit_thenParseEveryParentHash() {
        String line = DELIMITER + FULL_HASH
                + DELIMITER + ABBREVIATED_HASH
                + DELIMITER + "0816af9 1a2b3c4"
                + DELIMITER + ""
                + DELIMITER + "Hernan Cervera"
                + DELIMITER + "Hernan Cervera"
                + DELIMITER + "Dec-31-2025"
                + DELIMITER + "Merge pull request #382";
        GitCommit commit = parse(line);

        assertArrayEquals(new String[]{"0816af9", "1a2b3c4"}, commit.abbreviatedParentHashes());
    }
}
