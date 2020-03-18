import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileHandler {

    public static String getFileContents(String filename){
        try{
            return new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
        }
        catch(IOException e){
            return null;
        }
    }

    /**
     * Write a given String to a given filename
     *
     * @param filename Path to file
     * @param json     String to be written
     */
    public static void writeToFile(String filename, String json, boolean append) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, append));
            writer.write(json);
            writer.newLine();
            writer.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void appendMovie(String filename, Movie movie){
        String contents = getFileContents(filename).replaceAll("[\n\r]", "");
        contents = contents.substring(0,contents.length()-2);
        contents += "," + movie.toJSON() + "]}";
        writeToFile(filename,contents,false);
    }
}
