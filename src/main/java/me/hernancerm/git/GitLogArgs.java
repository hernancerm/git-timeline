package me.hernancerm.git;

public record GitLogArgs(
        String[] unparsedArgs,
        boolean isColorEnabled,
        boolean isPagerEnabled) {}
