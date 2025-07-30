package me.hernancerm;

import lombok.Data;

@Data
public class GitLogArgs {
    private String[] unparsedArgs;
    private boolean isPagerEnabled = true;
}
