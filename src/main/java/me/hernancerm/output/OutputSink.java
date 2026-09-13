package me.hernancerm.output;

/**
 * A destination for formatted git-log lines. The reader on the other end can go away before the
 * last line is written, so check {@link #isOpen()} as you go.
 */
public interface OutputSink extends AutoCloseable {

    /** Writes a line, followed by a line separator. */
    void println(String line);

    /** Returns false once the reader on the other end is gone. */
    boolean isOpen();

    /** Signals EOF to the reader and waits for it to finish. */
    @Override
    void close() throws InterruptedException;
}
