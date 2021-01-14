import org.json.JSONException;
import org.json.JSONObject;

/**
 * Hold movie info
 */
public class Movie {
    private final String title, TMDBId, IMDBId, releaseDate, collectionId, rating, filename;
    private final long size;

    /**
     * Create a movie
     *
     * @param builder Movie builder
     */
    private Movie(MovieBuilder builder) {
        this.title = builder.title;
        this.TMDBId = builder.TMDBId;
        this.IMDBId = builder.IMDBId;
        this.collectionId = builder.collectionId;
        this.releaseDate = builder.releaseDate;
        this.rating = builder.rating;
        this.size = builder.size;
        this.filename = builder.filename;
    }

    public static class MovieBuilder {
        private String title, TMDBId, IMDBId, releaseDate, collectionId, rating, filename;
        private long size;

        /**
         * Set the title of the movie
         *
         * @param title Title of movie
         * @return Builder
         */
        public MovieBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * Set the size of the movie file
         *
         * @param size Size of movie file
         * @return Builder
         */
        public MovieBuilder setSize(long size) {
            this.size = size;
            return this;
        }

        /**
         * Set the TMDB id of the movie
         *
         * @param TMDBId TMDB id of movie
         * @return Builder
         */
        public MovieBuilder setTMDBId(String TMDBId) {
            this.TMDBId = TMDBId;
            return this;
        }

        /**
         * Set the IMDB id of the movie
         *
         * @param IMDBId IMDB id of movie
         * @return Builder
         */
        public MovieBuilder setIMDBId(String IMDBId) {
            this.IMDBId = IMDBId;
            return this;
        }

        /**
         * Set the title of the movie
         *
         * @param releaseDate Release date of movie
         * @return Builder
         */
        public MovieBuilder setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
            return this;
        }

        /**
         * Set the collection id of the movie
         *
         * @param collectionId ID of collection which movie belongs to
         * @return Builder
         */
        public MovieBuilder setCollectionID(String collectionId) {
            this.collectionId = collectionId;
            return this;
        }

        /**
         * Set the rating of the movie
         *
         * @param rating Rating of movie
         * @return Builder
         */
        public MovieBuilder setRating(String rating) {
            this.rating = rating;
            return this;
        }

        /**
         * Set the name of the movie file
         *
         * @param filename Movie filename
         * @return Builder
         */
        public MovieBuilder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        /**
         * Build the movie
         *
         * @return Movie
         */
        public Movie build() {
            return new Movie(this);
        }
    }

    /**
     * Get the movie filename
     *
     * @return Movie filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Get the rating of the movie
     *
     * @return Movie rating
     */
    public String getRating() {
        return rating;
    }

    /**
     * Get the release date of the movie
     *
     * @return Movie release date
     */
    public String getReleaseDate() {
        return releaseDate;
    }

    /**
     * Get the TMDB (The movie database) id of the movie
     *
     * @return TMDB id
     */
    public String getTMDBId() {
        return TMDBId;
    }

    /**
     * Get the id of the collection that the movie belongs to
     *
     * @return Collection id
     */
    public String getCollectionId() {
        return collectionId;
    }

    /**
     * Get the movie title
     *
     * @return Movie title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Check if the movie is a member of a collection
     *
     * @return Movie belongs to collection
     */
    public boolean inCollection() {
        return collectionId != null;
    }

    /**
     * Get the IMDB (Internet movie database) id of the movie
     *
     * @return IMDB id
     */
    public String getIMDBId() {
        return IMDBId;
    }

    /**
     * Convert the movie object to a JSON object
     *
     * @return JSON of movie
     */
    public JSONObject toJSON() {
        return new JSONObject()
                .put("belongs_to_collection", inCollection() ? new JSONObject().put("id", collectionId) : collectionId)
                .put("size", size)
                .put("tmdb_id", TMDBId)
                .put("imdb_id", IMDBId)
                .put("title", title)
                .put("filename", filename)
                .put("release_date", releaseDate)
                .put("rating", rating);
    }

    /**
     * Create a movie object from a JSON object
     *
     * @param json JSON representing a movie either from JSON file or TMDB
     * @param size Size of movie given by Plex
     * @param tmdb If JSON comes from TMDB or json file
     * @return A movie object created using information in the JSON
     */
    public static Movie createMovie(JSONObject json, long size, String filename, boolean tmdb) {
        String collectionId = Collection.getCollection(json);

        try {
            String IMDBId = json.getString("imdb_id");
            String date = json.getString("release_date");
            String title;
            String rating;
            String TMDBId;

            // Key refactoring occurs when saving to json for clarity: id -> tmdb_id (to allow saving multiple ids)
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
            return new Movie.MovieBuilder()
                    .setIMDBId(IMDBId)
                    .setTMDBId(TMDBId)
                    .setCollectionID(collectionId)
                    .setTitle(title)
                    .setReleaseDate(date)
                    .setFilename(filename)
                    .setRating(rating)
                    .setSize(size)
                    .build();
        }
        catch(JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
