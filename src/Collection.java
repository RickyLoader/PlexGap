import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Hold information on a movie collection
 */
public class Collection {
    private final String title, id;
    private final ArrayList<String> movies;

    /**
     * Create a movie collection
     *
     * @param id     Collection id
     * @param title  Title of collection
     * @param movies List of movie ids
     */
    public Collection(String id, String title, ArrayList<String> movies) {
        this.title = title;
        this.movies = movies;
        this.id = id;
    }

    /**
     * Get the collection id
     *
     * @return Collection id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the list of movie ids in the collection
     *
     * @return List of movie ids in collection
     */
    public ArrayList<String> getMovies() {
        return movies;
    }

    /**
     * Get the title of the collection
     *
     * @return Collection title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Mark a movie as found in the collection
     *
     * @param movie Movie to mark as found
     */
    public void markFound(Movie movie) {
        movies.remove(movie.getTMDBId());
    }

    /**
     * Check if the collection is complete (all movies have been removed/marked as found)
     *
     * @return Collection is complete
     */
    public boolean isComplete() {
        return movies.isEmpty();
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
