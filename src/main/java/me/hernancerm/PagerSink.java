package me.hernancerm;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class PagerSink implements OutputSink {

    private final Process process;
    private final PrintWriter writer;

    private PagerSink(Process process) {
        this.process = process;
        this.writer = new PrintWriter(
                new BufferedOutputStream(process.getOutputStream()), false);
    }

    public static PagerSink start(List<String> command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return new PagerSink(processBuilder.start());
    }

    @Override
    public void println(String line) {
        writer.println(line);
        writer.flush();
    }

    @Override
    public boolean isOpen() {
        return process.isAlive();
    }

    @Override
    public void close() throws InterruptedException {
        // Closing the writer signals EOF to the pager (less), which starts interactive mode.
        writer.close();
        process.waitFor();
    }
}
