package me.hernancerm.git;

import java.util.regex.Pattern;

/**
 * The line format git-log is asked for, and the parser that reads it back. Both halves live
 * here so that changing one is done next to the other.
 */
public class CommitLineParser {

    // Field delimiter.
    // No field placed before the subject line can contain it.
    // - Ref names (%d) reject every ASCII control char, so they cannot hold the 0x1F.
    // - Ident names (%an, %cn) are cut by git at the first '<', so they cannot hold the '<'.
    // - Hashes (%H, %h, %p) are hex digits and spaces.
    // The subject line (%s) can hold it, so goes last.
    static final String DELIMITER = "\u001F<";

    private static final Pattern DELIMITER_PATTERN = Pattern.compile(Pattern.quote(DELIMITER));

    // The delimiter as spelled in a git-log `--pretty=format:` string. %x1f is the 0x1F byte.
    private static final String DELIMITER_FORMAT = "%x1f<";

    static final String PRETTY_FORMAT = String.join(DELIMITER_FORMAT,
            // The leading delimiter closes the prefix added by the git-log option `--graph`.
            "",
            "%H", "%h", "%p", "%C(auto)%d", "%cn", "%an", "%ad", "%s");

    // The `--graph` prefix plus the eight fields of PRETTY_FORMAT.
    private static final int PART_COUNT = 9;

    private CommitLineParser() {
    }

    /** Returns the fields of a commit line, or null when the line carries no commit data. */
    static String[] splitCommitLine(String line) {
        String[] parts = DELIMITER_PATTERN.split(line, PART_COUNT);
        return parts.length == PART_COUNT ? parts : null;
    }

    /** Builds a commit out of the fields of a commit line. */
    static GitCommit toCommit(String[] parts) {
        return new GitCommit(
                parts[1],
                parts[2],
                parts[3].split("\\s"),
                parts[4],
                parts[5],
                parts[6],
                parts[7],
                parts[8]);
    }

    /**
     * Drops the delimiter's control byte from a pass-through arg. Without this a user supplied
     * format, e.g. `--date=format:`, could inject a field boundary.
     */
    static String stripDelimiter(String arg) {
        return arg.replace("\u001F", "");
    }
}
