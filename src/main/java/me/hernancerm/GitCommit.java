package me.hernancerm;

import java.util.Arrays;

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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fullHash == null) ? 0 : fullHash.hashCode());
		result = prime * result + ((abbreviatedHash == null) ? 0 : abbreviatedHash.hashCode());
		result = prime * result + Arrays.hashCode(abbreviatedParentHashes);
		result = prime * result + ((authorName == null) ? 0 : authorName.hashCode());
		result = prime * result + ((authorDate == null) ? 0 : authorDate.hashCode());
		result = prime * result + ((committerName == null) ? 0 : committerName.hashCode());
		result = prime * result + ((subjectLine == null) ? 0 : subjectLine.hashCode());
		result = prime * result + ((refNamesColored == null) ? 0 : refNamesColored.hashCode());
		result = prime * result + ((remote == null) ? 0 : remote.hashCode());
		result = prime * result + ((args == null) ? 0 : args.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GitCommit other = (GitCommit) obj;
		if (fullHash == null) {
			if (other.fullHash != null)
				return false;
		} else if (!fullHash.equals(other.fullHash))
			return false;
		if (abbreviatedHash == null) {
			if (other.abbreviatedHash != null)
				return false;
		} else if (!abbreviatedHash.equals(other.abbreviatedHash))
			return false;
		if (!Arrays.equals(abbreviatedParentHashes, other.abbreviatedParentHashes))
			return false;
		if (authorName == null) {
			if (other.authorName != null)
				return false;
		} else if (!authorName.equals(other.authorName))
			return false;
		if (authorDate == null) {
			if (other.authorDate != null)
				return false;
		} else if (!authorDate.equals(other.authorDate))
			return false;
		if (committerName == null) {
			if (other.committerName != null)
				return false;
		} else if (!committerName.equals(other.committerName))
			return false;
		if (subjectLine == null) {
			if (other.subjectLine != null)
				return false;
		} else if (!subjectLine.equals(other.subjectLine))
			return false;
		if (refNamesColored == null) {
			if (other.refNamesColored != null)
				return false;
		} else if (!refNamesColored.equals(other.refNamesColored))
			return false;
		if (remote == null) {
			if (other.remote != null)
				return false;
		} else if (!remote.equals(other.remote))
			return false;
		if (args == null) {
			if (other.args != null)
				return false;
		} else if (!args.equals(other.args))
			return false;
		return true;
	}
}
