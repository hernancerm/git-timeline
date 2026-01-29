package me.hernancerm;

public record GitLogArgs(
        String[] unparsedArgs,
        boolean isPagerEnabled,
        boolean isGraphEnabled) {}
