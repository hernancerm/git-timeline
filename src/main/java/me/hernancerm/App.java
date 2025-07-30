package me.hernancerm;

public class App {

    public static void main(String[] args) throws Exception {
        GitLogFormatter formatter = new GitLogFormatter();
        GitLogProcessBuilder processBuilder = new GitLogProcessBuilder();
        System.exit(new GitTimeline(args, processBuilder, formatter).call());
    }
}
