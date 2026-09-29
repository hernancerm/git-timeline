package xyz.hernancerm.git;

public record GitLogArgs(
        String[] unparsedArgs,
        boolean isColorEnabled,
        boolean isPagerEnabled) {}
