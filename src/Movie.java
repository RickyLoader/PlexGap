public class Movie {
    private final String title;
    private final String TMDBId;
    private final String IMDBId;
    private final String releaseDate;
    private final String collection;
    private final String rating;
    private long size;

    public Movie(String title, String TMDBId, String IMDBId, String collection, String releaseDate, String rating, long size) {
        this.title = title;
        this.TMDBId = TMDBId;
        this.IMDBId = IMDBId;
        this.collection = collection;
        this.releaseDate = releaseDate;
        this.rating = rating;
        this.size = size;
    }

    public String getRating() {
        return rating;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getTMDBId() {
        return TMDBId;
    }

    public String getCollection() {
        return collection;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCollection() {
        return collection != null;
    }

    public String getIMDBId() {
        return IMDBId;
    }

    public String toJSON() {
        StringBuilder builder = new StringBuilder();
        String q = "\"";
        String series = collection;
        if(isCollection()) {
            series = "{" + q + "id" + q + ":" + collection + "}";
        }
        builder
                .append("{")
                .append(q + "belongs_to_collection" + q + ":")
                .append(series + ",")
                .append(q + "size" + q + ":")
                .append(q + size + q + ",")
                .append(q + "id" + q + ":")
                .append(q + TMDBId + q + ",")
                .append(q + "imdb_id" + q + ":")
                .append(q + IMDBId + q + ",")
                .append(q + "original_title" + q + ":")
                .append(q + title + q + ",")
                .append(q + "release_date" + q + ":")
                .append(q + releaseDate + q + ",")
                .append(q + "vote_average" + q + ":")
                .append(q + rating + q)
                .append("}");
        return builder.toString();
    }

    public int getSizeMegabyte() {
        return Math.toIntExact(size / 1000);
    }

    public String getSizeSummary() {
        return title + " --- " + String.format("%,d MB", getSizeMegabyte());
    }

    public String getRatingSummary() {
        return rating + " --- " + title;
    }
}
