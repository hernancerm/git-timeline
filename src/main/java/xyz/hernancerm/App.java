package xyz.hernancerm;

import xyz.hernancerm.git.GitLogRunner;

public class App {

    public static void main(String[] args) throws Exception {
        System.exit(new GitTimeline(args, new GitLogRunner()).call());
    }
}
