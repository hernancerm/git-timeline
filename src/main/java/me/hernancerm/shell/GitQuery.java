package me.hernancerm.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A git subprocess started ahead of the moment its output is needed, so that it runs alongside
 * whatever the caller does next instead of before it.
 */
public class GitQuery {

    // Null for a skipped query. Naming the value here keeps it out of both call sites.
    private final String what;
    private final Process process;

    private GitQuery(String what, Process process) {
        this.what = what;
        this.process = process;
    }

    /** Starts the query. {@code what} names the value it yields, for error messages. */
    public static GitQuery start(String what, String... command) {
        try {
            return new GitQuery(what, new ProcessBuilder(command).start());
        } catch (IOException e) {
            throw new RuntimeException("Error starting git process to get " + what, e);
        }
    }

    /** A query not worth running because its value cannot be used. Reads back as null. */
    public static GitQuery skipped() {
        return new GitQuery(null, null);
    }

    /** Returns the first line of stdout, or null when the query was skipped. */
    public String firstLine() {
        if (process == null) {
            return null;
        }

        try (
                var inputStreamReader = new InputStreamReader(process.getInputStream());
                var bufferedReader = new BufferedReader(inputStreamReader)
        ) {
            return bufferedReader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("Error reading " + what, e);
        }
    }
}
