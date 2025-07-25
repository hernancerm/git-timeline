package me.hernancerm;

import java.util.Map;

public class CommitMapper {

    private CommitMapper() {}

    public static Commit mapToCommit(Map<String, String> rawCommit) {
        Commit commit = new Commit();
        // TODO: Use these from constants shared in the git-log command.
        commit.setHash(rawCommit.get("HASH"));
        commit.setAuthorName(rawCommit.get("AUTHOR"));
        commit.setSubjectLine(rawCommit.get("MESSAGE"));
        commit.setDate(rawCommit.get("DATE"));
        return commit;
    }
}
