package me.hernancerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import picocli.CommandLine;

public class App {

    public static void main(String[] args) throws IOException, InterruptedException {
//        CommandLine commandLine = new CommandLine(
//                new GitTimeline(
//                        args,
//                        new GitLogProcessBuilder(),
//                        new GitLogFormatter())
//        );
//        commandLine.setUnmatchedArgumentsAllowed(true);
//        System.exit(commandLine.execute(args));

        try {
            // Start the `less` process
            // Force less to stay open (-X), avoid auto-quit (-F)
            ProcessBuilder pb = new ProcessBuilder("less", "-F");
            pb.redirectError(ProcessBuilder.Redirect.INHERIT); // show any errors in our terminal
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT); // Pipe less output to terminal
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);      // We write into its stdin
            Process less = pb.start();

            // Get output stream of the less process (its stdin)
            OutputStream lessStdin = less.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(lessStdin), true);

            // Write a bunch of lines to less
            for (int i = 1; i <= 100; i++) {
                writer.println("Line number " + i);
            }

            // Close the writer to signal EOF to `less`
            writer.close();

            // Wait for the user to quit less
            int exitCode = less.waitFor();
            System.out.println("less exited with code " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}