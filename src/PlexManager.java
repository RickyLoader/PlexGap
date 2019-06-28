import static java.lang.Thread.sleep;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlexManager {

    private static String ip = "/library";
    private static String plexToken = "?X-Plex-Token=";
    private static String tmdbKey = null;
    private static String tmdbReadToken = null;
    private static OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) {
        if(getCredentials()) {
            ArrayList<Movie> movies = promptSource();
            Scanner scan = new Scanner(System.in);
            System.out.println("\n\nWhat would you like to do with your movies?\n\n1. Find missing sequels\n");
            switch(scan.nextLine()) {
                case "1":
                    findMissingSequels(movies);
                    break;
            }
        }
    }

    private static String createList(String accessToken) {
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
        String json = jsonPostRequest("https://api.themoviedb.org/4/list", body, headers);
        return getJsonValue(json, "id", true);
    }

    private static boolean getCredentials() {
        boolean authenticated = false;
        try {
            String file = new String(Files.readAllBytes(Paths.get("credentials.json")), "utf-8");
            JSONObject credentials = new JSONObject(file);

            String location = credentials.getString("plex_ip");
            String auth = credentials.getString("plex_token");
            String tmdb = credentials.getString("tmdb_api_key");
            String readToken = credentials.getString("tmdb_read_access_token");

            if(!location.isEmpty() && !auth.isEmpty() && !tmdb.isEmpty() && !readToken.isEmpty()) {
                authenticated = true;
                ip = "http://" + location + ip;
                plexToken = plexToken + auth;
                tmdbKey = tmdb;
                tmdbReadToken = readToken.trim();
            }
            else {
                System.out.println("Error in credentials.json, make sure your plex token and ip are entered correctly.");
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return authenticated;
    }

    /**
     * Create a list of movies found from the Plex API or a previously built JSON file
     *
     * @return ArrayList of movie objects
     */
    private static ArrayList<Movie> promptSource() {
        ArrayList<Movie> movies = new ArrayList<>();
        Scanner scan = new Scanner(System.in);
        System.out.println("Where would you like to read your Plex library in from?\n1. A JSON file\n2. The Plex API");

        switch(scan.nextLine()) {
            case "1":
                movies = readSaved(getFilename(true));
                break;
            case "2":
                movies = getPlexMovies();
                break;
        }
        System.out.println(movies.size() + " movies found!");
        return movies;
    }

    /**
     * Create a list of movies from a previously created JSON file
     *
     * @param filename The file to be read
     * @return ArrayList of movie objects
     */
    private static ArrayList<Movie> readSaved(String filename) {
        ArrayList<Movie> movies = new ArrayList<>();
        try {
            String file = new String(Files.readAllBytes(Paths.get(filename)), "utf-8");
            JSONArray allMovies = new JSONObject(file).getJSONArray("movies");
            for(int i = 0; i < allMovies.length(); i++) {
                JSONObject movie = allMovies.getJSONObject(i);
                movies.add(createMovie(movie));
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return movies;
    }

    /**
     * Prompt the user for a valid filename
     *
     * @return File to be read
     */
    private static String getFilename(Boolean reading) {
        String msg = "Please enter the path (including file extension) to your JSON file:";
        if(!reading) {
            msg = "Please enter a full path and filename (including file extension) for the movies to be saved in JSON format:";
        }
        String filename = null;
        Scanner scan = new Scanner(System.in);
        boolean valid = false;
        while(!valid || filename == null || filename.isEmpty()) {
            System.out.println(msg);
            filename = scan.nextLine();
            valid = fileExists(filename, reading);
        }
        return filename;
    }

    /**
     * Verifies the existence and readability of a given file path
     *
     * @param filename File to be read
     * @param reading  True if reading from existing file
     * @return Boolean representing valid file path
     */
    private static boolean fileExists(String filename, Boolean reading) {
        File file = new File(filename);
        if(reading) {
            return file.exists() && file.canRead();
        }
        return true;
    }

    /**
     * Create an ArrayList of movie objects from movies found in the Plex library of the user, using the Plex API
     *
     * @return ArrayList of movie objects
     */
    private static ArrayList<Movie> getPlexMovies() {
        String filename = getFilename(false);
        writeToFile(filename, "{\"movies\":[");
        ArrayList<Movie> movies = new ArrayList<>();

        String allMovieEndpoint = ip + "/sections/1/all/" + plexToken;
        try {

            // XML file representing basic information on all movies in library
            Element generalRoot = getXML(allMovieEndpoint);

            // All XML movie children
            NodeList movieContainers = generalRoot.getElementsByTagName("Video");
            for(int i = 0; i < movieContainers.getLength(); i++) {

                // Get basic information on single movie
                Node movieContainer = movieContainers.item(i);
                Element element = (Element) movieContainer;
                System.out.println("Getting info for movie " + (i + 1) + "/" + movieContainers.getLength());

                // Query separate endpoint for in depth movie information using its unique ratingKey
                String specificMovieEndpoint = ip + "/metadata/" + element.getAttribute("ratingKey") + "/" + plexToken;
                Element specificRoot = getXML(specificMovieEndpoint);

                // In depth movie information
                Element xmlMovie = (Element) specificRoot.getElementsByTagName("Video").item(0);

                // Get series information JSON from the TMDB API using the guid, e.g: "com.plexapp.agents.imdb://tt0309593?lang=en"
                String json = getTMDBJSON(xmlMovie.getAttribute("guid"));

                if(json != null) {
                    Movie movie = createMovie(new JSONObject(json));
                    movies.add(movie);
                    boolean last = i == movieContainers.getLength() - 1;
                    String movieJSON = movie.toJSON();
                    if(!last) {
                        movieJSON += ",";
                    }
                    writeToFile(filename, movieJSON);
                }
            }
            writeToFile(filename, "]}");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return movies;
    }

    /**
     * Query the TMDB API for series information given the guid, e.g: "com.plexapp.agents.imdb://tt0309593?lang=en".
     * Extract the unique id from the guid to query the TMDB API
     *
     * @param guid The rating agent used by Plex to pull the rating for the movie. Either points to TMDB or IMDB
     * @return JSON summary of the movie
     */
    private static String getTMDBJSON(String guid) {

        String regex;
        String json = null;
        String id;

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
            String url = "https://api.themoviedb.org/3/movie/" + id + "?api_key=" + tmdbKey + "&language=en-US";
            json = jsonGetRequest(url);
        }
        return json;
    }

    /**
     * Write a given String to a given filename
     *
     * @param filename Path to file
     * @param json     String to be written
     */
    private static void writeToFile(String filename, String json) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
            writer.write(json);
            writer.newLine();
            writer.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a movie object from a JSON object
     *
     * @param json TMDB JSON representing a movie
     * @return A movie object created using information in the JSON
     */
    private static Movie createMovie(JSONObject json) {
        Movie result = null;
        String collection = getCollection(json);

        try {
            String IMDBId = json.getString("imdb_id");
            String date = json.getString("release_date");
            String title = json.getString("original_title");
            String TMDBId = String.valueOf(json.getInt("id"));
            String rating = String.valueOf(json.getDouble("vote_average"));
            result = new Movie(title, TMDBId, IMDBId, collection, date, rating);
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Create a request for an XML resource
     *
     * @param url Location of the XML
     * @return The XML document
     */
    private static Element getXML(String url) {
        Element output = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document document = builder.parse(url);
            output = document.getDocumentElement();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    /**
     * Find missing movie sequels by attempting to complete each movie collection with the given movies
     *
     * @param movies An ArrayList of movie objects
     */
    private static void findMissingSequels(ArrayList<Movie> movies) {

        // Store collections via their unique id
        HashMap<String, Collection> collections = new HashMap<>();

        // API has a query limit
        int hits = 0;
        int count = 1;
        for(Movie movie : movies) {
            if(movie.isCollection()) {
                System.out.println("Checking " + movie.getTitle() + " (" + (count) + "/" + movies.size() + ")");
                String id = movie.getCollection();

                // If the collection has been seen before
                if(collections.containsKey(id)) {
                    System.out.println(movie.getTitle() + " is a member of the " + collections.get(id).getTitle() + " collection - marking as seen\n\n");
                }
                else {
                    System.out.println(movie.getTitle() + " is a member of a collection which has not yet been seen - fetching collection info...\n");

                    // Query TMDB for the movies belonging to the collection
                    String url = "https://api.themoviedb.org/3/collection/" + id + "?api_key=" + tmdbKey + "&language=en-US";

                    // Create an object to hold the collection information and store it in the map
                    String collectionJson = jsonGetRequest(url);
                    Collection collection = getCollectionInfo(id, collectionJson);
                    System.out.println("Collection has been found - " + movie.getTitle() + " belongs to the " + collection.getTitle() + "\n\n");
                    collections.put(collection.getId(), collection);
                    hits++;
                }

                // Else mark the current movie as present in the library
                collections.get(id).addMovie(movie);
            }
            count += 1;
        }
        updateList(collections);
    }

    private static void updateList(HashMap<String, Collection> collections) {

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
        System.out.println("Creating list...\n\n");
        String listID = createList(accessToken);

        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer " + accessToken);
        headers.put("content-type", "application/json;charset=utf-8");

        jsonPostRequest("https://api.themoviedb.org/4/list/" + listID + "/items", body, headers);
        System.out.println("\n\nYour list has been created!\n\nVisit:\n\n" + "https://www.themoviedb.org/list/" + listID);
    }

    /**
     * Create a collection object which holds the unique id of each movie in a series.
     *
     * @param id   The unique id of the collection
     * @param json JSON containing information regarding the movie series
     * @return A collection object representing the movie series
     */
    private static Collection getCollectionInfo(String id, String json) {
        Collection collection = null;
        try {
            JSONObject file = new JSONObject(json);
            String title = file.getString("name");

            // Movies belonging to the series
            JSONArray movies = file.getJSONArray("parts");

            // Unique id of a movie -> existence in library (false by default)
            HashMap<String, Boolean> parts = new HashMap<>();

            // Unique id of a movie -> title
            HashMap<String, String> titles = new HashMap<>();

            for(int i = 0; i < movies.length(); i++) {
                JSONObject movie = (JSONObject) movies.get(i);

                // Ignore movies without a release date (typically announced sequels with no release date)
                if(movie.has("release_date") && !movie.isNull("release_date")) {
                    String date = movie.getString("release_date");

                    // Ignore movies which are not released
                    if(validDate(date)) {
                        parts.put(String.valueOf(movie.getInt("id")), false);
                        titles.put(String.valueOf(movie.getInt("id")), movie.getString("title"));
                    }
                }
            }
            collection = new Collection(id, title, parts, titles);

        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return collection;
    }

    /**
     * Date is valid if it is prior to the current date. Movie sequels can be announced years in advance
     *
     * @param date The data to be verified
     * @return True if the date is valid
     */
    private static boolean validDate(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            // release date is not empty and is not after the current date (movie is released)
            return !date.isEmpty() && format.parse(date).compareTo(new Date()) < 0;
        }
        catch(ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Attempt to obtain the unique id of the collection a movie belongs to
     *
     * @param json JSON response from TMDB API
     * @return null or unique id of movie collection
     */
    private static String getCollection(JSONObject json) {

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

    private static void waitForRateLimit(int callsRemaining) {
        try {
            if(callsRemaining == 0) {
                System.out.println("\n\nSleeping 10 seconds (rate limit)...\n\n");
                sleep(10000);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Query a URL and return the JSON response
     *
     * @param url The URL to be queried
     * @return A JSON response from the URL
     */
    private static String jsonGetRequest(String url) {
        String json = null;
        try {
            Request.Builder builder = new Request.Builder().url(url);
            Response response = client.newCall(builder.build()).execute();
            if(response.isSuccessful()) {
                json = response.body().string();
                waitForRateLimit(Integer.valueOf(response.header("X-RateLimit-Remaining")));
            }

            response.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    private static String jsonPostRequest(String url, String body, HashMap<String, String> headers) {
        String json = null;
        try {
            Request.Builder builder = new Request.Builder().url(url);
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"), body
            );
            builder.post(requestBody);
            for(String header : headers.keySet()) {
                builder.addHeader(header, headers.get(header));
            }

            Response response = client.newCall(builder.build()).execute();
            json = response.body().string();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    private static String getWriteAccess() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer " + tmdbReadToken);
        headers.put("content-type", "application/json;charset=utf-8");

        // Get request token
        String json = jsonPostRequest("https://api.themoviedb.org/4/auth/request_token", "{}", headers);

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
        json = jsonPostRequest("https://api.themoviedb.org/4/auth/access_token", body, headers);
        return getJsonValue(json, "access_token", false);
    }

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
