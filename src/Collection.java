import org.json.JSONObject;

import java.util.ArrayList;

public class Collection {
    private final String title, id;
    private final ArrayList<String> movies;

    public Collection(String id, String title, ArrayList<String> movies) {
        this.title = title;
        this.movies = movies;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addMovie(Movie movie) {
        movies.remove(movie.getTMDBId());
    }

    public String getTitle() {
        return title;
    }

    public boolean collectionComplete() {
        return movies.isEmpty();
    }

    public String getSummary() {
        JSONObject summary = new JSONObject();
        for(String movie : movies) {
            summary.put("media_type", "movie").put("media_id", Integer.valueOf(movie));
        }
        return summary.toString();
    }

    /**
     * Attempt to obtain the unique id of the collection a movie belongs to
     *
     * @param json JSON response from TMDB API
     * @return null or unique id of movie collection
     */
    public static String getCollection(JSONObject json) {
        if(json == null) {
            return null;
        }

        String id = null;
        try {
            if(json.has("belongs_to_collection") && !json.isNull("belongs_to_collection")) {
                JSONObject collection = json.getJSONObject("belongs_to_collection");
                id = String.valueOf(collection.getInt("id"));
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return id;
    }
}
