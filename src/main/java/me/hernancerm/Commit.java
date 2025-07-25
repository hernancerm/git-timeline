package me.hernancerm;

import lombok.Data;

@Data
public class Commit {
    private String hash;
    private String authorName;
    private String subjectLine;
    private String date;
}
