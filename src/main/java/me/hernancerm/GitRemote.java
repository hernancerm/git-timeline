package me.hernancerm;

public record GitRemote(
        Platform platform,
        String repositoryName,
        String ownerName) {

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
