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
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    private static String jsonFile = null;
    private static final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) {
        if(getCredentials()) {
            ArrayList<Movie> movies = promptSource();
            if(movies.isEmpty()) {
                System.out.println("No movies were found, please check that " + jsonFile + " exists and is readable, or that your credentials are valid.");
                return;
            }
            System.out.println(movies.size() + " movies found!\n");
            Scanner scan = new Scanner(System.in);
            System.out.println("What would you like to do with your movies?\n\n1. Find missing sequels\n\n2. View movies by size\n\n3. View movies by rating\n");
            switch(scan.nextLine()) {
                case "1":
                    findMissingSequels(movies);
                    break;
                case "2":
                    orderMoviesBySize(movies);
                    break;
                case "3":
                    orderMoviesByRating(movies);
                    break;
                case "4":
                    trainingData(movies);
            }
        }
    }

    private static String toTrainingPhrase(String prefix, String title) {
        String q = "\"";
        String optional = "{" + q + "text" + q + ":" + q + prefix + q + "," + q + "userDefined" + q + ":" +
                "false" + "},";
        String open = "{" + q + "data" + q + ":" + "[";
        String required = "{" + q + "text" + q + ":" + q + title + q + "," + q + "alias" + q + ":" + q + "movie-name" + q + ","
                + q + "meta" + q + ":" + q + "@movie-name" + q + "," + q + "userDefined" + q + ":" + "false}],";
        String close = q + "isTemplate" +
                q + ":" + "false" + "," + q + "count" + q + ":" + 0 + "," + q + "updated" + q + ":" + 0 + "," + q + "isAuto" + q + ":" +
                "false},";
        if(prefix == null) {
            return open + required + close;
        }
        return open + optional + required + close;
    }

    private static void trainingData(ArrayList<Movie> movies) {
        String result = "";
        for(int i = 0; i < movies.size(); i++) {
            Movie movie = movies.get(i);
            String title = movie.getTitle()
                    .replace("(", "")
                    .replace(")", "");
            result += toTrainingPhrase(null, title);
        }
        result = result.substring(0, result.length() - 1);
        writeToFile("dave.json", result);
    }

    private static String getIMDBRating(String imdbID) {
        String rating = null;
        try {
            URL destination = new URL("https://www.imdb.com/title/" + imdbID);
            BufferedReader reader = new BufferedReader(new InputStreamReader(destination.openStream()));
            String line;
            while((line = reader.readLine()) != null) {
                Matcher matcher = Pattern.compile("(?<=\"ratingValue\": \")[0-9]+.[0-9]").matcher(line.trim());

                if(matcher.find()) {
                    rating = line.trim().substring(matcher.start(), matcher.end());
                    break;
                }

            }
            reader.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        return rating;
    }

    private static void orderMoviesByRating(ArrayList<Movie> movies) {
        Collections.sort(movies, new Comparator<Movie>() {
            @Override
            public int compare(Movie o1, Movie o2) {
                return Double.valueOf(o2.getRating()).compareTo(Double.valueOf(o1.getRating()));
            }
        });
        for(Movie movie : movies) {
            System.out.println(movie.getRatingSummary());
        }
    }

    private static void orderMoviesBySize(ArrayList<Movie> movies) {
        Collections.sort(movies, new Comparator<Movie>() {
            @Override
            public int compare(Movie o1, Movie o2) {
                return o1.getSizeMegabyte() - o2.getSizeMegabyte();
            }
        });
        for(Movie movie : movies) {
            System.out.println(movie.getSizeSummary());
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
            String json = credentials.getString("json_file");

            if(!location.isEmpty() && !auth.isEmpty() && !tmdb.isEmpty() && !readToken.isEmpty() && !json.isEmpty()) {
                authenticated = true;
                ip = "http://" + location + ip;
                plexToken = plexToken + auth;
                tmdbKey = tmdb;
                tmdbReadToken = readToken.trim();
                jsonFile = json;
            }
            else {
                System.out.println("Error in credentials.json, make sure they are entered correctly.");
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
        System.out.println("Where would you like to read your Plex library in from?\n\n1. " + jsonFile + "\n2. The Plex API\n");

        switch(scan.nextLine()) {
            case "1":
                movies = readSaved();
                break;
            case "2":
                movies = getPlexMovies();
                break;
        }
        return movies;
    }

    /**
     * Create a list of movies from a previously created JSON file
     *
     * @return ArrayList of movie objects
     */
    private static ArrayList<Movie> readSaved() {
        HashMap<String, Movie> saved = new HashMap<>();
        ArrayList<Movie> movies = new ArrayList<>();
        try {
            String file = new String(Files.readAllBytes(Paths.get(jsonFile)), StandardCharsets.UTF_8);
            JSONArray allMovies = new JSONObject(file).getJSONArray("movies");
            for(int i = 0; i < allMovies.length(); i++) {
                JSONObject jsonMovie = allMovies.getJSONObject(i);
                Movie movie = createMovie(jsonMovie, jsonMovie.getString("size"), false);

                // Plex only supplies one id, it could be either so need both to check
                saved.put(movie.getIMDBId(), movie);
                saved.put(movie.getTMDBId(), movie);
                movies.add(movie);
            }

            System.out.println("Checking Plex for new movies...\n");

            NodeList onPlex = getLibraryOverview();
            for(int i = 0; i < onPlex.getLength(); i++) {
                Node movieContainer = onPlex.item(i);
                String id = getMovieID(((Element) movieContainer).getAttribute("guid"));
                if(!saved.containsKey(id)) {
                    Movie movie = getMovie(movieContainer);
                    if(movie != null) {
                        System.out.println("\""+movie.getTitle() + "\" was found on Plex and not in " + jsonFile + "\n");
                        movies.add(movie);
                    }
                }
            }
        }
        catch(IOException i) {
            System.out.println("\n\n" + jsonFile + " not found, exiting program.\n");
        }
        return movies;
    }

    private static NodeList getLibraryOverview() {
        NodeList movies = null;
        String allMovieEndpoint = ip + "/sections/2/all/" + plexToken;
        try {
            // XML file representing basic information on all movies in library
            Element generalRoot = getXML(allMovieEndpoint);
            // All XML movie children
            movies = generalRoot.getElementsByTagName("Video");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return movies;
    }

    private static Movie getMovie(Node n) {
        Movie movie = null;
        // Get basic information on movie
        Element element = (Element) n;
        Element sizeInfo = (Element) element.getElementsByTagName("Part").item(0);

        // Query separate endpoint for in depth movie information using its unique ratingKey
        String specificMovieEndpoint = ip + "/metadata/" + element.getAttribute("ratingKey") + "/" + plexToken;
        Element specificRoot = getXML(specificMovieEndpoint);

        // In depth movie information
        Element xmlMovie = (Element) specificRoot.getElementsByTagName("Video").item(0);

        // Get series information JSON from the TMDB API using the guid, e.g: "com.plexapp.agents.imdb://tt0309593?lang=en"
        String json = getTMDBJSON(xmlMovie.getAttribute("guid"));
        if(json != null) {
            movie = createMovie(new JSONObject(json), sizeInfo.getAttribute("size"), true);
        }
        return movie;
    }

    /**
     * Create an ArrayList of movie objects from movies found in the Plex library of the user, using the Plex API
     *
     * @return ArrayList of movie objects
     */
    private static ArrayList<Movie> getPlexMovies() {
        writeToFile(jsonFile, "{\"movies\":[");
        ArrayList<Movie> movies = new ArrayList<>();
        NodeList movieContainers = getLibraryOverview();
        for(int i = 0; i < movieContainers.getLength(); i++) {
            System.out.println("Getting info for movie " + (i + 1) + "/" + movieContainers.getLength());
            Movie movie = getMovie(movieContainers.item(i));

            if(movie != null) {
                movies.add(movie);
                boolean last = i == movieContainers.getLength() - 1;
                String movieJSON = movie.toJSON();
                if(!last) {
                    movieJSON += ",";
                }
                writeToFile(jsonFile, movieJSON);
            }
        }
        writeToFile(jsonFile, "]}");

        return movies;
    }

    /**
     * Extract the unique id from the guid e.g: "com.plexapp.agents.imdb://tt0309593?lang=en" = tt0309593
     * Either points to TMDB or IMDB, TMDB supports either, Plex supplies only one
     *
     * @param guid The rating agent used by Plex to pull the rating for the movie
     * @return id The id of the movie
     */
    private static String getMovieID(String guid) {
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
     * Query the TMDB API for series information given the guid, e.g: "com.plexapp.agents.imdb://tt0309593?lang=en".
     * Extract the unique id from the guid to query the TMDB API
     *
     * @param guid The rating agent used by Plex to pull the rating for the movie. Either points to TMDB or IMDB
     * @return JSON summary of the movie
     */
    private static String getTMDBJSON(String guid) {
        String json = null;
        String id = getMovieID(guid);
        if(id != null) {
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
    private static Movie createMovie(JSONObject json, String size, boolean api) {
        Movie result = null;
        String collection = getCollection(json);

        try {
            String IMDBId = json.getString("imdb_id");
            String date = json.getString("release_date");
            String title = json.getString("original_title");
            String TMDBId = String.valueOf(json.getInt("tmdb_id"));
            String rating;
            if(api) {
                rating = getIMDBRating(IMDBId);
            }
            else {
                rating = json.getString("vote_average");
            }
            result = new Movie(title, TMDBId, IMDBId, collection, date, rating, Long.parseLong(size));
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
                    if(collectionJson == null) {
                        System.out.println(url);
                        continue;
                    }
                    Collection collection = getCollectionInfo(id, collectionJson);
                    System.out.println("Collection has been found - " + movie.getTitle() + " belongs to the " + collection.getTitle() + "\n\n");
                    collections.put(collection.getId(), collection);
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
        System.out.println("\n\nCreating list...\n\n");
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
                System.out.println("\n\nSleeping 3 seconds (rate limit)...\n\n");
                sleep(3000);
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

                /* They removed rate limiting
                waitForRateLimit(Integer.parseInt(response.header("X-RateLimit-Remaining")));*/
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
