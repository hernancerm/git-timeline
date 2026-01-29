package me.hernancerm;

import java.util.Arrays;
import java.util.Objects;

public class GitCommit {

    private String fullHash;
    private String abbreviatedHash;
    private String[] abbreviatedParentHashes;
    private String authorName;
    private String authorDate;
    private String committerName;
    private String subjectLine;
    private String refNamesColored;
    private GitRemote remote;
    private GitLogArgs args;

    public void reset() {
        fullHash = null;
        abbreviatedHash = null;
        abbreviatedParentHashes = null;
        authorName = null;
        authorDate = null;
        committerName = null;
        subjectLine = null;
        refNamesColored = null;
        remote = null;
        args = null;
    }

    public String getFullHash() {
        return fullHash;
    }

    public void setFullHash(String fullHash) {
        this.fullHash = fullHash;
    }

    public String getAbbreviatedHash() {
        return abbreviatedHash;
    }

    public void setAbbreviatedHash(String abbreviatedHash) {
        this.abbreviatedHash = abbreviatedHash;
    }

    public String[] getAbbreviatedParentHashes() {
        return abbreviatedParentHashes;
    }

    public void setAbbreviatedParentHashes(String[] abbreviatedParentHashes) {
        this.abbreviatedParentHashes = abbreviatedParentHashes;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorDate() {
        return authorDate;
    }

    public void setAuthorDate(String authorDate) {
        this.authorDate = authorDate;
    }

    public String getCommitterName() {
        return committerName;
    }

    public void setCommitterName(String committerName) {
        this.committerName = committerName;
    }

    public String getSubjectLine() {
        return subjectLine;
    }

    public void setSubjectLine(String subjectLine) {
        this.subjectLine = subjectLine;
    }

    public String getRefNamesColored() {
        return refNamesColored;
    }

    public void setRefNamesColored(String refNamesColored) {
        this.refNamesColored = refNamesColored;
    }

    public GitRemote getRemote() {
        return remote;
    }

    public void setRemote(GitRemote remote) {
        this.remote = remote;
    }

    public GitLogArgs getArgs() {
        return args;
    }

    public void setArgs(GitLogArgs args) {
        this.args = args;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                fullHash,
                abbreviatedHash,
                Arrays.hashCode(abbreviatedParentHashes),
                authorName,
                authorDate,
                committerName,
                subjectLine,
                refNamesColored,
                remote,
                args);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        GitCommit other = (GitCommit) obj;

        return Objects.equals(fullHash, other.fullHash)
                && Objects.equals(abbreviatedHash, other.abbreviatedHash)
                && Arrays.equals(abbreviatedParentHashes, other.abbreviatedParentHashes)
                && Objects.equals(authorName, other.authorName)
                && Objects.equals(authorDate, other.authorDate)
                && Objects.equals(committerName, other.committerName)
                && Objects.equals(subjectLine, other.subjectLine)
                && Objects.equals(refNamesColored, other.refNamesColored)
                && Objects.equals(remote, other.remote)
                && Objects.equals(args, other.args);
    }
}
