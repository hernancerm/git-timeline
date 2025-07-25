package me.hernancerm;

import java.util.Map;

public class CommitMapper {

    private CommitMapper() {}

    public static Commit mapToCommit(Map<String, String> result) {
        Commit commit = new Commit();
        commit.setHash(result.get(GitLogProcessBuilder.HASH));
        commit.setAuthorName(result.get(GitLogProcessBuilder.AUTHOR_NAME));
        commit.setAuthorDate(result.get(GitLogProcessBuilder.AUTHOR_DATE));
        commit.setSubjectLine(result.get(GitLogProcessBuilder.SUBJECT_LINE));
        return commit;
    }
}
