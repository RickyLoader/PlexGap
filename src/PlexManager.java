import org.json.JSONArray;
import org.json.JSONObject;
import sun.nio.ch.Net;

import java.io.*;
import java.net.URL;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlexManager {

    private static Credentials credentials;
    private static PlexServer plexServer;
    private static TheMovieDatabase TMDB;

    /**
     * Validate credentials and process movies
     *
     * @param args none
     */
    public static void main(String[] args) {
        credentials = new Credentials();
        if(credentials.isAuthenticated()) {
            TMDB = new TheMovieDatabase(credentials.getTmdbKey(), credentials.getTmdbReadToken());
            plexServer = new PlexServer(credentials.getPlexIp(), credentials.getPlexToken(), credentials.getLibraryID(), TMDB);
            ArrayList<Movie> movies = promptMovieSource();

            if(movies == null || movies.isEmpty()) {
                System.out.println("No movies were found, please check that your credentials are correct or " + credentials.getMovieJsonFileName() + " exists.");
                return;
            }
            processMovies(movies);
        }
        else {
            System.out.println("Error in credentials.json, make sure file exists and credentials are entered correctly.");
        }
    }

    /**
     * Create a list of movies found from the Plex API or a previously built JSON file
     *
     * @return ArrayList of movie objects
     */
    private static ArrayList<Movie> promptMovieSource() {
        ArrayList<Movie> movies = new ArrayList<>();
        Scanner scan = new Scanner(System.in);
        System.out.println("Where would you like to read your Plex library in from?\n\n1. " + credentials.getMovieJsonFileName() + "\n2. The Plex API\n");

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
     * Ask user which action to perform on movies
     *
     * @param movies List of movies found by chosen method
     */
    private static void processMovies(ArrayList<Movie> movies) {
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
        }
    }

    /**
     * Create a list of movies from a previously created JSON file
     *
     * @return ArrayList of movie objects
     */
    private static ArrayList<Movie> readSaved() {
        HashMap<String, Movie> saved = new HashMap<>();
        ArrayList<Movie> movies = new ArrayList<>();

        String file = FileHandler.getFileContents(credentials.getMovieJsonFileName());
        if(file == null) {
            return null;
        }

        JSONArray allMovies = new JSONObject(file).getJSONArray("movies");
        for(int i = 0; i < allMovies.length(); i++) {
            JSONObject jsonMovie = allMovies.getJSONObject(i);
            Movie movie = Movie.createMovie(jsonMovie, jsonMovie.getLong("size"), false);

            // Plex only supplies one id, it could be either so need both to check
            saved.put(movie.getIMDBId(), movie);
            saved.put(movie.getTMDBId(), movie);
            movies.add(movie);
        }

        System.out.println("Checking Plex for movies not currently in " + credentials.getMovieJsonFileName() + " ...\n");

        JSONArray plexLibrary = plexServer.getLibraryOverview();

        int missing = 0;

        for(int i = 0; i < plexLibrary.length(); i++) {
            JSONObject movieContainer = plexLibrary.getJSONObject(i);
            String id = plexServer.getMovieID(movieContainer.getString("guid"));
            if(!saved.containsKey(id)) {
                Movie movie = plexServer.jsonToMovie(movieContainer);
                if(movie != null) {
                    missing++;
                    System.out.println("\"" + movie.getTitle() + "\" was found on Plex and not in " + credentials.getMovieJsonFileName() + "\n");
                    FileHandler.appendMovie(credentials.getMovieJsonFileName(), movie);
                    movies.add(movie);
                }
            }
        }
        System.out.println(missing == 0 ? credentials.getMovieJsonFileName() + " is up to date!\n" : missing + " Movies added to " + credentials.getMovieJsonFileName() + " from Plex!\n");
        return movies;
    }

    /**
     * Create an ArrayList of movie objects from movies found in the Plex library of the user, using the Plex API
     *
     * @return ArrayList of movie objects
     */
    private static ArrayList<Movie> getPlexMovies() {
        FileHandler.writeToFile(credentials.getMovieJsonFileName(), "{\"movies\":[", true);
        ArrayList<Movie> movies = new ArrayList<>();
        JSONArray plexLibrary = plexServer.getLibraryOverview();

        for(int i = 0; i < plexLibrary.length(); i++) {
            System.out.println("Getting info for movie " + (i + 1) + "/" + plexLibrary.length());
            Movie movie = plexServer.jsonToMovie(plexLibrary.getJSONObject(i));

            if(movie != null) {
                movies.add(movie);
                boolean last = i == plexLibrary.length() - 1;
                String movieJSON = movie.toJSON();
                if(!last) {
                    movieJSON += ",";
                }
                FileHandler.writeToFile(credentials.getMovieJsonFileName(), movieJSON, true);
            }
        }
        FileHandler.writeToFile(credentials.getMovieJsonFileName(), "]}", true);
        return movies;
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
                    String url = "https://api.themoviedb.org/3/collection/" + id + "?api_key=" + credentials.getTmdbKey() + "&language=en-US";

                    // Create an object to hold the collection information and store it in the map
                    String collectionJson = new NetworkRequest(null, null).send(url);

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
        TMDB.updateList(collections);
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
     * Order movies by TMDB rating
     *
     * @param movies List of movies
     */
    private static void orderMoviesByRating(ArrayList<Movie> movies) {
        Collections.sort(movies, (o1, o2) -> Double.valueOf(o2.getRating()).compareTo(Double.valueOf(o1.getRating())));
        for(Movie movie : movies) {
            System.out.println(movie.getRatingSummary());
        }
    }

    /**
     * Order movies by size
     *
     * @param movies List of movies
     */
    private static void orderMoviesBySize(ArrayList<Movie> movies) {
        Collections.sort(movies, (o1, o2) -> o1.getSizeMegabyte() - o2.getSizeMegabyte());
        for(Movie movie : movies) {
            System.out.println(movie.getSizeSummary());
        }
    }
}
