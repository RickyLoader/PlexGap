import okhttp3.OkHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handle Plex server operations
 */
public class PlexServer {
    private final String ip, token, library;
    private final TheMovieDatabase TMDB;
    private final HashMap<String, String> headers;
    private final OkHttpClient client;

    /**
     * Create the Plex server
     *
     * @param ip      IP of plex server
     * @param token   Plex token
     * @param library Plex library id
     * @param TMDB    TMDB
     * @param client  Network client for API calls
     */
    public PlexServer(String ip, String token, String library, TheMovieDatabase TMDB, OkHttpClient client) {
        this.ip = "http://" + ip + "/library";
        this.token = "?X-Plex-Token=" + token;
        this.TMDB = TMDB;
        this.library = library;
        this.client = client;

        // Force Plex to use JSON not default XML
        headers = new HashMap<>();
        headers.put("accept", "application/json");
    }

    /**
     * Get the Plex library overview
     *
     * @return List of movies on Plex
     */
    public JSONArray getLibraryOverview() {
        String libraryJSON = new NetworkRequest(null, headers, client)
                .send(ip + "/sections/" + library + "/all/" + token);
        return new JSONObject(libraryJSON).getJSONObject("MediaContainer").getJSONArray("Metadata");
    }

    /**
     * Get the overview for a specific movie
     *
     * @param ratingKey Plex ratingKey
     * @return Specific movie metadata
     */
    public JSONObject getMovieOverview(String ratingKey) {
        String movieJSON = new NetworkRequest(null, headers, client)
                .send(ip + "/metadata/" + ratingKey + token);
        return new JSONObject(movieJSON)
                .getJSONObject("MediaContainer")
                .getJSONArray("Metadata")
                .getJSONObject(0);
    }

    /**
     * Extract the unique id from the guid Plex uses. E.g: "com.plexapp.agents.imdb://tt0309593?lang=en" = tt0309593
     * Either points to TMDB or IMDB, TMDB supports either, Plex supplies only one
     *
     * @param guid The rating agent used by Plex to pull the rating for the movie
     * @return id The id of the movie
     */
    public String getMovieID(String guid) {
        String regex;
        String id = null;

        // e.g: "com.plexapp.agents.imdb://tt0309593?lang=en"
        if(guid.contains("imdb")) {
            regex = "tt[0-9]+";
        }
        // e.g: "com.plexapp.agents.themoviedb://14161?lang=en"
        else {
            regex = "[0-9]+";
        }

        Matcher matcher = Pattern.compile(regex).matcher(guid);

        if(matcher.find()) {
            id = guid.substring(matcher.start(), matcher.end());
        }
        return id;
    }


    /**
     * Fetch the required information from TMDB to create a movie, using the general movie overview from Plex.
     *
     * @param movieDetails General overview of a specific movie given by Plex
     * @return A movie object containing the info
     */
    public Movie jsonToMovie(JSONObject movieDetails) {
        Movie movie = null;
        long movieSize = movieDetails
                .getJSONArray("Media")
                .getJSONObject(0)
                .getJSONArray("Part")
                .getJSONObject(0)
                .getLong("size");
        String filename = movieDetails.getString("title");

        String guid = movieDetails.getString("guid");

        // Movie only has a plex id
        if(guid.contains("plex://")) {
            guid = getMovieOverview(movieDetails.getString("ratingKey"))
                    .getJSONArray("Guid")
                    .getJSONObject(0)
                    .getString("id");
        }

        String json = TMDB.getMovieInfo(getMovieID(guid));
        if(json != null) {
            movie = Movie.createMovie(new JSONObject(json), movieSize, filename, true);
        }
        return movie;
    }
}