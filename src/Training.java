import java.util.ArrayList;

public class Training {
    private static String toTrainingPhrase(String prefix, String title) {
        String q = "\"";
        String optional = "{" + q + "text" + q + ":" + q + prefix + q + "," + q + "userDefined" + q + ":" +
                "false" + "},";
        String open = "{" + q + "data" + q + ":" + "[";
        String required = "{" + q + "text" + q + ":" + q + title + q + "," + q + "alias" + q + ":" + q + "movie-name" + q + ","
                + q + "meta" + q + ":" + q + "@movie-name" + q + "," + q + "userDefined" + q + ":" + "false}],";
        String close = q + "isTemplate" +
                q + ":" + "false" + "," + q + "count" + q + ":" + 0 + "," + q + "updated" + q + ":" + 0 + "," + q + "isAuto" + q + ":" +
                "false},";
        if(prefix == null) {
            return open + required + close;
        }
        return open + optional + required + close;
    }

    private static void trainingData(ArrayList<Movie> movies) {
        String result = "";
        for(int i = 0; i < movies.size(); i++) {
            Movie movie = movies.get(i);
            String title = movie.getTitle()
                    .replace("(", "")
                    .replace(")", "");
            result += toTrainingPhrase(null, title);
        }
        result = result.substring(0, result.length() - 1);
        //writeToFile("dave.json", result);
    }
}
