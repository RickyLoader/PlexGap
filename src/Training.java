import java.util.ArrayList;

public class Training {
    private static String toTrainingPhrase(String prefix, String title) {
        return new JSONObject()
                .put("text", prefix)
                .put("userDefined", false)
                .put("data",
                        new JSONArray()
                                .put(new JSONObject()
                                        .put("text", title)
                                        .put("alias", "movie-name")
                                        .put("meta", "@movie-name")
                                        .put("userDefined", false)
                                )
                )
                .put("isTemplate", "false")
                .put("count", 0)
                .put("updated", 0)
                .put("isAuto", false)
                .toString();
    }
}
