import java.util.HashMap;

public class Collection {
    private final String title;
    private final HashMap<String, Boolean> movies;
    private final HashMap<String, String> titles;
    private final String id;

    public Collection(String id, String title, HashMap<String, Boolean> movies, HashMap<String, String> titles) {
        this.title = title;
        this.movies = movies;
        this.id = id;
        this.titles = titles;
    }

    public String getId() {
        return id;
    }

    public void addMovie(Movie movie) {
        movies.put(movie.getTMDBId(), true);
    }

    public String getTitle() {
        return title;
    }

    public boolean collectionComplete() {
        for(String movie : movies.keySet()) {
            if(!movies.get(movie)) {
                return false;
            }
        }
        return true;
    }

    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        for(String movie : movies.keySet()) {
            if(!movies.get(movie)) {
                summary.append("{\"media_type\":\"movie\",\"media_id\":" + Integer.valueOf(movie) + "},");
            }
        }
        return summary.toString();
    }
}
