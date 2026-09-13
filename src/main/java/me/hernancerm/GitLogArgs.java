package me.hernancerm;

public record GitLogArgs(
        String[] unparsedArgs,
        boolean isColorEnabled,
        boolean isPagerEnabled) {}
