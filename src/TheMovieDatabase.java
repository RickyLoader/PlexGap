import org.json.JSONObject;
import sun.nio.ch.Net;

import java.util.HashMap;
import java.util.Scanner;

public class TheMovieDatabase {
    private String key;
    private String token;

    public TheMovieDatabase(String key, String token) {
        this.key = key;
        this.token = token;
    }

    /**
     * Query the TMDB API for series information given the guid, e.g: "com.plexapp.agents.imdb://tt0309593?lang=en".
     * Extract the unique id from the guid to query the TMDB API
     *
     * @param id The rating agent used by Plex to pull the rating for the movie. Either points to TMDB or IMDB
     * @return JSON summary of the movie
     */
    public String getMovieInfo(String id) {
        String json = null;
        if(id != null) {
            String url = "https://api.themoviedb.org/3/movie/" + id + "?api_key=" + key + "&language=en-US";
            json = new NetworkRequest(null, null).send(url);
        }
        return json;
    }

    /**
     * Create an empty list for the movies to populate
     *
     * @param accessToken Temporary access token granted by user
     * @return id of list for populating
     */
    private String createList(String accessToken) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer " + accessToken);
        headers.put("content-type", "application/json;charset=utf-8");

        Scanner scan = new Scanner(System.in);
        String name = "";
        System.out.println("Enter a name for your list:\n\n");
        while(name.isEmpty()) {
            name = scan.nextLine();
        }
        String body = "{" + "\"name\"" + ":" + "\"" + name + "\",\"iso_639_1\":\"en\"}";
        String json = new NetworkRequest(body, headers).send("https://api.themoviedb.org/4/list");
        return getJsonValue(json, "id", true);
    }

    /**
     * Get write access from the user and create + populate a list on TMDB
     *
     * @param collections List of collections, find incomplete collections to add missing movies to TMDB list
     */
    public void updateList(HashMap<String, Collection> collections) {

        StringBuilder json = new StringBuilder("{\"items\":[");

        for(String id : collections.keySet()) {
            Collection c = collections.get(id);
            if(!c.collectionComplete()) {
                json.append(c.getSummary());
            }
        }
        String body = json.toString();
        body = body.substring(0, body.length() - 1);
        body += "]}";
        String accessToken = getWriteAccess();
        System.out.println("\n\nCreating list...\n\n");
        String listID = createList(accessToken);

        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer " + accessToken);
        headers.put("content-type", "application/json;charset=utf-8");

        new NetworkRequest(body, headers).send("https://api.themoviedb.org/4/list/" + listID + "/items");
        System.out.println("\n\nYour list has been created!\n\nVisit:\n\n" + "https://www.themoviedb.org/list/" + listID);
    }

    /**
     * Retrieve temporary write access to user TMDB account for creation of list
     *
     * @return access token for write access
     */
    public String getWriteAccess() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer " + token);
        headers.put("content-type", "application/json;charset=utf-8");

        // Get request token
        String json = new NetworkRequest("{}", headers).send("https://api.themoviedb.org/4/auth/request_token");

        // Ask the user to authenticate the request token
        String requestToken = getJsonValue(json, "request_token", false);
        String url = "https://www.themoviedb.org/auth/access?request_token=" + requestToken;
        System.out.println("Please visit:\n\n" + url + "\n\nTo approve the application, this allows it to create a TMDB list containing your missing movies.\n\nType \"ok\" when ready:\n\n");
        String answer = "";
        Scanner scan = new Scanner(System.in);

        while(!answer.equalsIgnoreCase("ok")) {
            answer = scan.nextLine();
        }

        // Use the authenticated request token to obtain an access token for write permission
        String body = "{" + "\"request_token\"" + ":" + "\"" + requestToken + "\"}";
        json = new NetworkRequest(body, headers).send("https://api.themoviedb.org/4/auth/access_token");
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
