package me.hernancerm;

import lombok.Data;

@Data
public class GitRemote {
    private Platform platform;
    private String repositoryName;
    private String ownerName;

    public enum Platform {
        BITBUCKET_ORG,
        GITHUB_COM;

        public static Platform toEnum(String platform) {
            Platform output = null;
            if (platform.equals("bitbucket.org")) {
                output = BITBUCKET_ORG;
            } else if (platform.equals("github.com")) {
                output = GITHUB_COM;
            }
            return output;
        }
    }
}
