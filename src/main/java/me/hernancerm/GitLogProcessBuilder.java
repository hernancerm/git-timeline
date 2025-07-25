package me.hernancerm;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class GitLogProcessBuilder {

    public static final String HASH = "HASH";
    public static final String AUTHOR_NAME = "AUTHOR_NAME";
    public static final String AUTHOR_DATE = "AUTHOR_DATE";
    public static final String SUBJECT_LINE = "SUBJECT_LINE";

    /**
     * Run git-log making it output an easy-to-deserialize format which does not require
     * escape sequences to correctly capture any possible character for the values, e.g.,
     * for the subject line. Current constraint: Values which can have a newline character
     * are not handled correctly, e.g. the body. The deserialization output by this process
     * is handled by {@link GitLogDeserializer}.
     * @param args Optional additional args for git-log.
     * @return A new process for git log with the args.
     * @throws IOException If an I/O error occurs starting the process.
     */
    public Process start(List<String> args) throws IOException {
        List<String> command = Stream.concat(Stream.of(
                        "git",
                        "log",
                        "--date=format:%d/%b/%Y",
                        "--pretty="
                                + HASH + "=%H%n"
                                + AUTHOR_NAME + "=%an%n"
                                + AUTHOR_DATE + "=%ad%n"
                                + SUBJECT_LINE +"=%s%n"
                                +"END"),
                args.stream()).toList();
        return new ProcessBuilder(command).start();
    }
}
