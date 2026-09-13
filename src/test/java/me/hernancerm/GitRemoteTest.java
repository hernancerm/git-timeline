package me.hernancerm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GitRemoteTest {

    @Test
    void parse_givenSupportedUrl_thenExtractEveryPart() {
        // Host, owner, repository per remote url.
        String[][] urls = {
                // The `.git` suffix is optional. Requiring it used to throw on these.
                {"https://github.com/jeffreytse/zsh-vi-mode", "GITHUB_COM", "jeffreytse", "zsh-vi-mode"},
                {"https://github.com/o/r", "GITHUB_COM", "o", "r"},
                {"https://bitbucket.org/o/r", "BITBUCKET_ORG", "o", "r"},
                {"git@github.com:o/r", "GITHUB_COM", "o", "r"},
                {"https://user@github.com/o/r", "GITHUB_COM", "o", "r"},
                // Already worked.
                {"https://github.com/o/r.git", "GITHUB_COM", "o", "r"},
                {"https://github.com/o/r.git/", "GITHUB_COM", "o", "r"},
                {"https://github.com/o/r/", "GITHUB_COM", "o", "r"},
                {"http://github.com/o/r.git", "GITHUB_COM", "o", "r"},
                {"https://user:tok@github.com/o/r.git", "GITHUB_COM", "o", "r"},
                {"git@github.com:o/r.git", "GITHUB_COM", "o", "r"},
                {"https://bitbucket.org/o/r.git", "BITBUCKET_ORG", "o", "r"},
                // Valid ways to clone from GitHub that used to yield no hyperlinks.
                {"ssh://git@github.com/o/r.git", "GITHUB_COM", "o", "r"},
                {"ssh://git@github.com:22/o/r.git", "GITHUB_COM", "o", "r"},
                {"git://github.com/o/r.git", "GITHUB_COM", "o", "r"},
                // A dot in the repository name is not the `.git` suffix.
                {"https://github.com/o/my.repo", "GITHUB_COM", "o", "my.repo"},
        };

        for (String[] url : urls) {
            GitRemote remote = GitRemote.parse(url[0]);

            assertNotNull(remote, url[0]);
            assertEquals(GitRemote.Platform.valueOf(url[1]), remote.platform(), url[0]);
            assertEquals(url[2], remote.ownerName(), url[0]);
            assertEquals(url[3], remote.repositoryName(), url[0]);
        }
    }

    @Test
    void parse_givenUrlWithoutHyperlinks_thenReturnNull() {
        // No hyperlinks is the worst outcome of an unparseable or unsupported remote. Throwing
        // would take the whole log down with it.
        String[] urls = {
                "/local/path/repo",
                "../sibling/repo",
                "file:///local/path/repo",
                "https://gitlab.com/group/sub/repo.git",
                "",
                null,
        };

        for (String url : urls) {
            assertNull(GitRemote.parse(url), String.valueOf(url));
        }
    }
}
