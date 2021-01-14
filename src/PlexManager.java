import okhttp3.OkHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
        if(!credentials.isAuthenticated()) {
            System.out.println("Error in credentials.json, make sure file exists and credentials are entered correctly.");
            return;
        }
        OkHttpClient client = new OkHttpClient();
        TMDB = new TheMovieDatabase(
                credentials.getTmdbKey(),
                credentials.getTmdbReadToken(),
                client
        );
        plexServer = new PlexServer(
                credentials.getPlexIp(),
                credentials.getPlexToken(),
                credentials.getLibraryID(),
                TMDB,
                client
        );

        ArrayList<Movie> movies = promptMovieSource();

        if(movies.isEmpty()) {
            System.out.println(
                    "No movies were found, please check that your credentials are correct or "
                            + credentials.getMovieJsonFileName() + " exists."
            );
            return;
        }
        processMovies(movies);
    }

    /**
     * Prompt the user for the location to parse the Plex movies from.
     * Create a list of movies found from the desired location.
     *
     * @return ArrayList of movie objects
     */
    private static ArrayList<Movie> promptMovieSource() {
        ArrayList<Movie> movies = new ArrayList<>();
        Scanner scan = new Scanner(System.in);
        System.out.println(
                "Where would you like to read your Plex library in from?\n\n"
                        + "1. " + credentials.getMovieJsonFileName() + "\n"
                        + "2. The Plex API\n"
        );

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
     * Check for any movies on Plex that are not in the file and update the file accordingly
     *
     * @return ArrayList of movies
     */
    private static ArrayList<Movie> readSaved() {
        ArrayList<Movie> movies = new ArrayList<>();

        String fileName = credentials.getMovieJsonFileName();
        String fileContent = FileHandler.getFileContents(fileName);

        if(fileContent == null) {
            return movies;
        }

        HashMap<String, Movie> seen = new HashMap<>();

        JSONObject file = new JSONObject(fileContent);
        JSONArray fileMovies = file.getJSONArray("movies");

        for(int i = 0; i < fileMovies.length(); i++) {
            JSONObject movieJSON = fileMovies.getJSONObject(i);
            Movie movie = Movie.createMovie(
                    movieJSON,
                    movieJSON.getLong("size"),
                    movieJSON.getString("filename"),
                    false
            );
            if(movie == null) {
                continue;
            }

            movies.add(movie);
            seen.put(movie.getIMDBId(), movie);
            seen.put(movie.getTMDBId(), movie);
        }

        System.out.println("Checking Plex for movies not currently in " + fileName + " ...\n");

        JSONArray plexLibrary = plexServer.getLibraryOverview();
        int missing = 0;

        for(int i = 0; i < plexLibrary.length(); i++) {
            JSONObject movieContainer = plexLibrary.getJSONObject(i);
            String id = plexServer.getMovieID(movieContainer.getString("guid"));
            if(seen.containsKey(id)) {
                continue;
            }
            Movie movie = plexServer.jsonToMovie(movieContainer);
            System.out.println(movie.getTitle() + " was found on Plex and not in " + fileName + "\n");
            fileMovies.put(movie.toJSON());
            movies.add(movie);
            missing++;
        }

        FileHandler.writeToFile(fileName, file.toString());
        System.out.println(
                missing == 0
                        ? credentials.getMovieJsonFileName() + " is up to date!\n"
                        : missing + " movies added to " + credentials.getMovieJsonFileName() + " from Plex!\n"
        );
        return movies;
    }


    /**
     * Create an list of movies found in the Plex library of the user, using the Plex API.
     * Write the list to a JSON file
     *
     * @return ArrayList of movies
     */
    private static ArrayList<Movie> getPlexMovies() {
        ArrayList<Movie> movies = new ArrayList<>();
        JSONArray plexLibrary = plexServer.getLibraryOverview();
        JSONArray jsonArray = new JSONArray();

        for(int i = 0; i < plexLibrary.length(); i++) {
            System.out.println("Getting info for movie " + (i + 1) + "/" + plexLibrary.length());
            Movie movie = plexServer.jsonToMovie(plexLibrary.getJSONObject(i));
            if(movie != null) {
                movies.add(movie);
                jsonArray.put(movie.toJSON());
            }
        }
        FileHandler.writeToFile(
                credentials.getMovieJsonFileName(),
                new JSONObject().put("movies", jsonArray).toString()
        );
        return movies;
    }

    /**
     * Ask user which action to perform on the list of movies
     *
     * @param movies List of movies
     */
    private static void processMovies(ArrayList<Movie> movies) {
        System.out.println("\n" + movies.size() + " movies found!\n");
        Scanner scan = new Scanner(System.in);
        System.out.println("What would you like to do with your movies?\n\n1. Find missing sequels\n");
        if(scan.nextLine().equals("1")) {
            findMissingSequels(movies);
        }
    }

    /**
     * Find missing movie sequels by attempting to complete each movie collection with the given movies
     *
     * @param movies List of movies
     */
    private static void findMissingSequels(ArrayList<Movie> movies) {
        HashMap<String, Collection> collections = new HashMap<>();

        int count = 1;

        for(Movie movie : movies) {
            if(!movie.inCollection()) {
                continue;
            }
            System.out.println("Checking " + movie.getTitle() + " (" + (count) + "/" + movies.size() + ")");
            String id = movie.getCollectionId();

            if(collections.containsKey(id)) {
                System.out.println(
                        movie.getTitle()
                                + " is a member of the " + collections.get(id).getTitle()
                                + " collection - marking as seen\n\n"
                );
            }
            else {
                System.out.println(
                        movie.getTitle()
                                + " is a member of a collection which has not yet been seen - fetching info...\n"
                );

                String collectionJson = TMDB.getCollectionInfo(id);
                if(collectionJson == null) {
                    continue;
                }

                Collection collection = parseCollectionInfo(id, collectionJson);
                System.out.println(
                        "Collection has been found - "
                                + movie.getTitle() + " belongs to the " + collection.getTitle() + "\n\n"
                );
                collections.put(collection.getId(), collection);
            }
            collections.get(id).markFound(movie);
            count++;
        }

        ArrayList<Movie> missing = new ArrayList<>();
        for(String id : collections.keySet()) {
            Collection c = collections.get(id);
            if(c.isComplete()) {
                continue;
            }
            ArrayList<String> ids = c.getMovies();
            for(String movieId : ids) {
                JSONObject movieJSON = new JSONObject(TMDB.getMovieInfo(movieId));
                missing.add(
                        Movie.createMovie(movieJSON, 0, null, true)
                );
            }
        }
        TMDB.createList(missing);
    }

    /**
     * Create a collection object which holds the unique id of each movie in a series.
     *
     * @param id   The unique id of the collection
     * @param json JSON containing information regarding the collection
     * @return A collection object representing the movie series
     */
    private static Collection parseCollectionInfo(String id, String json) {
        Collection collection = null;
        try {
            JSONObject file = new JSONObject(json);
            String title = file.getString("name");
            JSONArray movies = file.getJSONArray("parts");
            ArrayList<String> parts = new ArrayList<>();

            for(int i = 0; i < movies.length(); i++) {
                JSONObject movie = movies.getJSONObject(i);

                // Ignore movies without a release date (typically announced future sequels)
                if(movie.has("release_date") && !movie.isNull("release_date")) {
                    String date = movie.getString("release_date");

                    // Ignore movies which are not released
                    if(validDate(date)) {
                        parts.add(String.valueOf(movie.getInt("id")));
                    }
                }
            }
            collection = new Collection(id, title, parts);

        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return collection;
    }

    /**
     * Date is valid if it is prior to the current date. Movie sequels can be announced years in advance
     *
     * @param date The date to be verified
     * @return Date is valid
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
}
