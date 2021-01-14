import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Handle basic reading/writing from files
 */
public class FileHandler {

    /**
     * Get the contents of the given filename as a String
     *
     * @param filename Filename to read
     * @return File contents as String
     */
    public static String getFileContents(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
        }
        catch(IOException e) {
            return null;
        }
    }

    /**
     * Write the given String to the given filename
     *
     * @param filename Path to file
     * @param json     String to be written
     */
    public static void writeToFile(String filename, String json) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, false));
            writer.write(json);
            writer.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
