package me.hernancerm;

public class AnsiUtils {

    private AnsiUtils() {
    }

    public static String buildHyperlink(String url, String title) {
        // https://unix.stackexchange.com/a/437585
        // To get the octal escape sequences for '\e', '\a', etc., do this:
        // 1. $ echo -n '\e' > _.txt
        // 2. $ nvim _.txt
        // 3. ga
        return "\033]8;;" + url + "\007" + title + "\033]8;;\007";
    }
}
