import okhttp3.OkHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Handle TMDB operations
 */
public class TheMovieDatabase {
    private final String key, token;
    private final OkHttpClient client;

    /**
     * Create the movie database.
     *
     * @param key    API key
     * @param token  Read access token
     * @param client Network client for API calls
     */
    public TheMovieDatabase(String key, String token, OkHttpClient client) {
        this.key = key;
        this.token = token;
        this.client = client;
    }

    /**
     * Query the TMDB API for info on a movie from the given id
     *
     * @param id Movie id
     * @return JSON summary of the movie
     */
    public String getMovieInfo(String id) {
        String json = null;
        if(id != null) {
            String url = "https://api.themoviedb.org/3/movie/" + id + "?api_key=" + key + "&language=en-US";
            json = new NetworkRequest(null, null, client).send(url);
        }
        return json;
    }

    /**
     * Query the TMDB API for info on a movie collection from the given id
     *
     * @param id Collection id
     * @return JSON summary of the collection
     */
    public String getCollectionInfo(String id) {
        String json = null;
        if(id != null) {
            String url = "https://api.themoviedb.org/3/collection/" + id + "?api_key=" + key + "&language=en-US";
            json = new NetworkRequest(null, null, client).send(url);
        }
        return json;
    }

    /**
     * Create an empty list for movies to later populate
     *
     * @param writeAccessToken Temporary write access token granted by user to create the list
     * @return id of newly created list
     */
    private String createEmptyList(String writeAccessToken) {
        Scanner scan = new Scanner(System.in);
        String name = "";
        System.out.println("Enter a name for your list:\n\n");
        while(name.isEmpty()) {
            name = scan.nextLine();
        }
        String body = new JSONObject().put("name", name).put("iso_639_1", "en").toString();
        String json = new NetworkRequest(
                body,
                getRequestHeaders(writeAccessToken),
                client
        ).send("https://api.themoviedb.org/4/list");
        return getJsonValue(json, "id", true);
    }

    /**
     * Get write access from the user.
     * Create and populate a list with the provided list of movies
     *
     * @param movies List of movies to add to the list
     */
    public void createList(ArrayList<Movie> movies) {
        JSONObject list = new JSONObject();
        JSONArray movieJSON = new JSONArray();

        for(Movie movie : movies) {
            movieJSON.put(
                    new JSONObject()
                            .put("media_type", "movie")
                            .put("media_id", Integer.valueOf(movie.getTMDBId()))
            );
        }

        String accessToken = getWriteAccess();
        System.out.println("\n\nCreating list...\n\n");
        String listID = createEmptyList(accessToken);

        new NetworkRequest(
                list.put("items", movieJSON).toString(),
                getRequestHeaders(accessToken),
                client
        ).send("https://api.themoviedb.org/4/list/" + listID + "/items");

        System.out.println(
                "\n\nYour list has been created!\n\nVisit:\n\n" + "https://www.themoviedb.org/list/" + listID
        );
    }

    /**
     * Get the required headers to authenticate with TMDB
     *
     * @param token Token required for authorization header
     * @return Request headers
     */
    private HashMap<String, String> getRequestHeaders(String token) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer " + token);
        headers.put("content-type", "application/json;charset=utf-8");
        return headers;
    }

    /**
     * Retrieve a temporary write access token to the user's TMDB profile
     *
     * @return Write access token
     */
    private String getWriteAccess() {
        HashMap<String, String> headers = getRequestHeaders(token);
        String json = new NetworkRequest(
                "{}",
                headers,
                client
        ).send("https://api.themoviedb.org/4/auth/request_token");

        // Ask the user to authenticate the request token
        String requestToken = getJsonValue(json, "request_token", false);
        String url = "https://www.themoviedb.org/auth/access?request_token=" + requestToken;
        System.out.println(
                "Please visit:\n\n" + url + "\n\nTo approve the application, this allows" +
                        " it to create a TMDB list containing your missing movies.\n\nType \"ok\" when ready:\n\n"
        );

        String answer = "";
        Scanner scan = new Scanner(System.in);

        while(!answer.equalsIgnoreCase("ok")) {
            answer = scan.nextLine();
        }

        // Use the authenticated request token to obtain an access token for write permission
        String body = new JSONObject().put("request_token", requestToken).toString();
        json = new NetworkRequest(body, headers, client).send("https://api.themoviedb.org/4/auth/access_token");
        return getJsonValue(json, "access_token", false);
    }

    /**
     * Retrieve a value from json given the key
     *
     * @param json    The json to search
     * @param key     The key to retrieve
     * @param integer Whether the value is an integer
     * @return Retrieved value
     */
    private static String getJsonValue(String json, String key, boolean integer) {
        String result = null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            if(integer) {
                result = String.valueOf(jsonObject.getInt(key));
            }
            else {
                result = jsonObject.getString(key);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
