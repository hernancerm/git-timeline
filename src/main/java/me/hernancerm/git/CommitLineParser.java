package me.hernancerm.git;

import java.util.regex.Pattern;

/**
 * The line format git-log is asked for, and the parser that reads it back. Kept together so one
 * is changed next to the other.
 */
public class CommitLineParser {

    // Field delimiter. No field before the subject line can hold it:
    // - Ref names (%d) reject every ASCII control char, so not the 0x1F.
    // - Ident names (%an, %cn) are cut by git at the first '<'.
    // - Hashes (%H, %h, %p) are hex digits and spaces.
    // The subject line (%s) can, so it goes last.
    static final String DELIMITER = "\u001F<";

    private static final Pattern DELIMITER_PATTERN = Pattern.compile(Pattern.quote(DELIMITER));

    // DELIMITER as spelled for `--pretty=format:`. %x1f is the 0x1F byte.
    private static final String DELIMITER_FORMAT = "%x1f<";

    static final String PRETTY_FORMAT = String.join(DELIMITER_FORMAT,
            // The leading delimiter closes the `--graph` prefix.
            "",
            "%H", "%h", "%p", "%C(auto)%d", "%cn", "%an", "%ad", "%s");

    // The `--graph` prefix plus the eight PRETTY_FORMAT fields.
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

    /** Drops the delimiter's control byte, so an arg cannot inject a field boundary. */
    static String stripDelimiter(String arg) {
        return arg.replace("\u001F", "");
    }
}
