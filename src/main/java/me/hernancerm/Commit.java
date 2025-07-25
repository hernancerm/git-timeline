package me.hernancerm;

import lombok.Data;

@Data
public class Commit {
    private String hash;
    private String authorName;
    private String authorDate;
    private String subjectLine;

    public void reset() {
        hash = null;
        authorName = null;
        authorDate = null;
        subjectLine = null;
    }
}
