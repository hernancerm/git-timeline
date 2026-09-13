package me.hernancerm.output;

/** A destination for formatted git-log lines, whose reader can go away at any time. */
public interface OutputSink extends AutoCloseable {

    /** Writes a line, followed by a line separator. */
    void println(String line);

    /** Returns false once the reader is gone. */
    boolean isOpen();

    /** Signals EOF to the reader and waits for it to finish. */
    @Override
    void close() throws InterruptedException;
}
