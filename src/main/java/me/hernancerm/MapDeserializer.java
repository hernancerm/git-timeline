package me.hernancerm;

import java.util.HashMap;
import java.util.Map;

/**
 * Deserialize maps.
 * The string maps conform to this format:
 * <br>
 * <br>
 * <pre>
 * {key-0}⏹{val-0}⏹{key-1}⏹{val-1}⏹{key-2}⏹{val-2}
 * </pre>
 * <p>
 * The black square Unicode symbol is <a href="https://www.compart.com/en/unicode/U+23F9">U+23F9</a>.
 * </p>
 */
public class MapDeserializer {

    private static final String SEPARATOR = "⏹";

    public Map<String, String> deserialize(String input) {
        String[] splitInput = input.split(SEPARATOR);
        if (splitInput.length % 2 == 0) {
            throw new RuntimeException(String.format(
                    "Even amount of separators found. An uneven amount of separators '%s' were expected in: %s",
                    SEPARATOR, input));
        }
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < splitInput.length; i += 2) {
            map.put(splitInput[i], splitInput[i + 1]);
            System.out.println(splitInput[i]);
            System.out.println(splitInput[i + 1]);
        }
        return map;
    }
}
