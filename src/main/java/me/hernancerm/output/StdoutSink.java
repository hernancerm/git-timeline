package me.hernancerm.output;

public class StdoutSink implements OutputSink {

    @Override
    public void println(String line) {
        System.out.println(line);
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    // Stdout outlives this program, so there is nothing to close.
    @Override
    public void close() {
    }
}
