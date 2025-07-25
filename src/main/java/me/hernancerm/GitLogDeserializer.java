package me.hernancerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitLogDeserializer {

    /**
     * Deserialize commits as objects of the class {@link Commit} provided by the reader.
     * This reader should be obtained from the process built by {@link GitLogProcessBuilder}, i.e.:
     * <pre>
     * Process process = gitLogProcessBuilder.start(args);
     * try (
     *         var inputStreamReader = new InputStreamReader(process.getInputStream());
     *         var bufferedReader = new BufferedReader(inputStreamReader)
     * ) {
     *     // Use the callback to do something with the commit.
     *     gitLogDeserializer.process(bufferedReader, commit -> {});
     * }
     * </pre>
     * After deserialization, do something with the commit via the callback.
     * @param bufferedReader Reader providing the character input stream.
     * @param callback What to do with each commit that is deserialized.
     * @throws IOException When reading a line from the reader fails.
     */
    public void process(BufferedReader bufferedReader, Consumer<Commit> callback) throws IOException {
        String line = bufferedReader.readLine();
        Map<String, String> result = new HashMap<>();
        Pattern pattern = Pattern.compile("^([A-Z_]+)=(.*)$");
        while (line != null) {
            if (line.matches("^END$")) {
                callback.accept(CommitMapper.mapToCommit(result));
                result.clear();
            } else {
                Matcher matcher = pattern.matcher(line);
                if (!matcher.matches()) {
                    // TODO: Improve error message.
                    throw new IllegalStateException("A match is always expected");
                }
                result.put(matcher.group(1), matcher.group(2));
            }
            line = bufferedReader.readLine();
        }
    }
}
