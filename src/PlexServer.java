import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlexServer {
    private String ip;
    private String token;
    private String library;
    TheMovieDatabase TMDB;
    private HashMap<String, String> headers;

    public PlexServer(String ip, String token, String library, TheMovieDatabase TMDB) {
        this.ip = ip;
        this.token = token;
        this.TMDB = TMDB;
        this.library = library;

        // Force Plex to use JSON not default XML
        headers = new HashMap<>();
        headers.put("accept", "application/json");
    }

    public JSONArray getLibraryOverview() {
        String allMovieEndpoint = ip + "/sections/" + library + "/all/" + token;
        String library = new NetworkRequest(null, headers).send(allMovieEndpoint);
        return new JSONObject(library).getJSONObject("MediaContainer").getJSONArray("Metadata");
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
        long movieSize = movieDetails.getJSONArray("Media").getJSONObject(0).getJSONArray("Part").getJSONObject(0).getLong("size");

        // Get series information JSON from the TMDB API using the guid, e.g: "com.plexapp.agents.imdb://tt0309593?lang=en"
        String json = TMDB.getMovieInfo(getMovieID(movieDetails.getString("guid")));
        if(json != null) {
            movie = Movie.createMovie(new JSONObject(json), movieSize, true);
        }
        return movie;
    }
}