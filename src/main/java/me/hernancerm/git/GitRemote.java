package me.hernancerm.git;

import me.hernancerm.shell.GitQuery;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GitRemote(
        Platform platform,
        String repositoryName,
        String ownerName) {

    // Any url form git-clone accepts, including the scp-like `host:owner/repo`. The `.git`
    // suffix is optional. Groups: 1:host, 2:owner, 3:repository.
    // https://git-scm.com/docs/git-clone#_git_urls
    private static final Pattern REMOTE_URL = Pattern.compile(
            "^(?:(?:ssh|git|https?|ftps?)://)?(?:[^@/]+@)?"
                    + "([^/:]+)(?::\\d+)?[:/](.+)/([^/]+?)(?:[.]git)?/?$");

    /** Starts the lookup of the url of the remote `origin`. */
    public static GitQuery startLookup() {
        return GitQuery.start(
                "remote url for: origin", "git", "remote", "get-url", "origin");
    }

    /** Returns null for no remote, a non-git url (e.g. a local path) or an unsupported host. */
    public static GitRemote parse(String originUrl) {
        if (originUrl == null || originUrl.isEmpty()) {
            return null;
        }

        Matcher matcher = REMOTE_URL.matcher(originUrl);
        if (!matcher.matches()) {
            return null;
        }

        Platform platform = Platform.from(matcher.group(1));
        if (platform == null) {
            return null;
        }

        return new GitRemote(platform, matcher.group(3), matcher.group(2));
    }

    /** The web page of one commit. */
    public String commitUrl(String fullHash) {
        return switch (platform) {
            case BITBUCKET_ORG -> baseUrl() + "/commits/" + fullHash;
            case GITHUB_COM -> baseUrl() + "/commit/" + fullHash;
        };
    }

    /** The web page of an issue or a pull request. On GitHub one url covers both. */
    public String issueUrl(String issueNumber) {
        return switch (platform) {
            case BITBUCKET_ORG -> baseUrl() + "/pull-requests/" + issueNumber;
            case GITHUB_COM -> baseUrl() + "/issues/" + issueNumber;
        };
    }

    /** The Jira issue page, or null for a platform with no Jira. */
    public String jiraIssueUrl(String issueKey) {
        return switch (platform) {
            case BITBUCKET_ORG -> "https://" + ownerName + ".atlassian.net/browse/" + issueKey;
            case GITHUB_COM -> null;
        };
    }

    private String baseUrl() {
        return "https://" + platform.host + "/" + ownerName + "/" + repositoryName;
    }

    public enum Platform {

        BITBUCKET_ORG("bitbucket.org"),
        GITHUB_COM("github.com");

        private final String host;

        Platform(String host) {
            this.host = host;
        }

        /** Returns null for an unsupported host. */
        public static Platform from(String host) {
            for (Platform platform : values()) {
                if (platform.host.equals(host)) {
                    return platform;
                }
            }
            return null;
        }
    }
}
