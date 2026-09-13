package me.hernancerm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GitRemote(
        Platform platform,
        String repositoryName,
        String ownerName) {

    // Git remote url in any form git-clone accepts, including the scp-like `host:owner/repo`.
    // The `.git` suffix is optional: `git remote add origin https://github.com/o/r` is valid.
    // Capture groups: 1:Host, 2:Owner, 3:Repository.
    // https://git-scm.com/docs/git-clone#_git_urls
    private static final Pattern REMOTE_URL = Pattern.compile(
            "^(?:(?:ssh|git|https?|ftps?)://)?(?:[^@/]+@)?([^/:]+)(?::\\d+)?[:/](.+)/([^/]+?)(?:[.]git)?/?$");

    /**
     * Returns null when there is no remote, the url is not a git url (e.g. a local path) or
     * the host is unsupported. Only the hyperlinks are lost, the rest of the output is
     * unaffected.
     */
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

    public enum Platform {
        BITBUCKET_ORG,
        GITHUB_COM;

        public static Platform from(String host) {
            return switch (host) {
                case "bitbucket.org" -> BITBUCKET_ORG;
                case "github.com" -> GITHUB_COM;
                default -> null;
            };
        }
    }
}
