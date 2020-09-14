import org.json.JSONException;
import org.json.JSONObject;

public class Movie {
    private final String title, TMDBId, IMDBId, releaseDate, collectionId, rating, filename;
    private final long size;

    private Movie(String title, String TMDBId, String IMDBId, String collectionId, String releaseDate, String rating, String filename, long size) {
        this.title = title;
        this.TMDBId = TMDBId;
        this.IMDBId = IMDBId;
        this.collectionId = collectionId;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.size = size;
        this.filename = filename;
    }

    String getFilename() {
        return filename;
    }

    String getRating() {
        return rating;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    String getTMDBId() {
        return TMDBId;
    }

    String getCollectionId() {
        return collectionId;
    }

    String getTitle() {
        return title;
    }

    boolean isCollection() {
        return collectionId != null;
    }

    String getIMDBId() {
        return IMDBId;
    }

    String toJSON() {
        return new JSONObject()
                .put("belongs_to_collection", isCollection() ? new JSONObject().put("id", collectionId) : collectionId)
                .put("size", size)
                .put("tmdb_id", TMDBId)
                .put("imdb_id", IMDBId)
                .put("title", title)
                .put("filename", filename)
                .put("release_date", releaseDate)
                .put("rating", rating)
                .toString();
    }

    int getSizeKilobyte() {
        return (int) Math.ceil((double) (size / 1024));
    }

    String getSizeSummary() {
        return " --- " + String.format("%,d MB", getSizeKilobyte());
    }

    String getRatingSummary() {
        return rating + " --- " + title;
    }


    /**
     * Create a movie object from a JSON object
     *
     * @param json JSON representing a movie either from JSON file or TMDB
     * @param size Size of movie given by Plex
     * @param tmdb If JSON comes from TMDB or json file
     * @return A movie object created using information in the JSON
     */
    static Movie createMovie(JSONObject json, long size, String filename, boolean tmdb) {
        Movie result = null;
        String collection = Collection.getCollection(json);

        try {
            String IMDBId = json.getString("imdb_id");
            String date = json.getString("release_date");
            String title;
            String rating;
            String TMDBId;

            // Key refactoring occurs when saving to json for clarity
            if(tmdb) {
                TMDBId = String.valueOf(json.getInt("id"));
                rating = String.valueOf(json.getDouble("vote_average"));
                title = json.getString("original_title");
            }
            else {
                TMDBId = String.valueOf(json.getInt("tmdb_id"));
                rating = json.getString("rating");
                title = json.getString("title");
            }
            result = new Movie(title, TMDBId, IMDBId, collection, date, rating, filename, size);
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
