import static java.lang.Thread.sleep;

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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlexGaps {

    private static String ip = "http://192.168.1.138:32400/library/";
    private static String plexToken = "?X-Plex-Token=CfsgymkTZzteGH78at3f";
    private static int bad = 0;

    public static void main(String[] args) {
        ArrayList<Movie> movies = promptSource();
        findMissingSequels(movies);
        System.out.println("there were " + bad + " bad movies");
    }

    private static ArrayList<Movie> promptSource() {
        ArrayList<Movie> movies = new ArrayList<>();
        Scanner scan = new Scanner(System.in);
        System.out.println("Where would you like to read your Plex library in from?\n1. A file\n2. The Plex API");
        switch(scan.nextLine()) {
            case "1":
                movies = readSaved(getFilename());

                break;
            case "2":
                movies = getPlexMovies();
                break;
        }
        return movies;
    }

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

    private static String getFilename() {
        String filename = null;
        Scanner scan = new Scanner(System.in);
        boolean valid = false;
        while(!valid || filename == null) {
            System.out.println("Please enter the path to your json file:");
            filename = scan.nextLine();
            valid = fileExists(filename);
        }
        return filename;
    }

    private static boolean fileExists(String filename) {
        File file = new File(filename);
        return file.exists() && file.canRead();
    }

    private static ArrayList<Movie> getPlexMovies() {
        String filename = getFilename();
        writeToFile(filename,"{\"movies\":[");
        ArrayList<Movie> movies = new ArrayList<>();

        String allMovieEndpoint = ip + "sections/1/all/" + plexToken;
        try {
            Element root = getXML(allMovieEndpoint);
            NodeList movieContainers = root.getElementsByTagName("Video");
            for(int i = 0; i < movieContainers.getLength(); i++) {
                Node movieContainer = movieContainers.item(i);
                Element element = (Element) movieContainer;
                System.out.println("Getting info for movie " + (i + 1) + "/" + movieContainers.getLength());
                Movie movie = createMovie(getTMDBJSON(element.getAttribute("ratingKey")));
                if(movie != null) {
                    movies.add(movie);
                    boolean last = i==movieContainers.getLength()-1;
                    writeToFile(filename, movieToJSON(movie,last));
                }
            }
            writeToFile(filename,"]}");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return movies;
    }

    private static String movieToJSON(Movie movie,boolean last){
        StringBuilder builder = new StringBuilder();
        String q = "\"";
        String collection = movie.getCollection();
        if(movie.isCollection()){
            collection = "{"+q+"id"+q+":"+movie.getCollection()+"}";
        }
        builder
                .append("{")
                .append(q+"belongs_to_collection"+q+":")
                .append(collection+",")
                .append(q+"id"+q+":")
                .append(movie.getId()+",")
                .append(q+"imdb_id"+q+":")
                .append(q+movie.getIMDBId()+q+",")
                .append(q+"original_title"+q+":")
                .append(q+movie.getTitle()+q+",")
                .append(q+"release_date"+q+":")
                .append(q+movie.getReleaseDate()+q)
                .append("}");
        if(!last){
            builder.append(",");
        }
        return builder.toString();
    }

    private static void writeToFile(String filename, String json) {
        try {
            System.out.println(json);
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename,true));
            writer.write(json);
            writer.newLine();
            writer.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static Movie createMovie(JSONObject json) {
        Movie result = null;
        if(json == null) {
            bad++;
            return result;
        }

        String collection = getCollection(json);

        try {
            String TMDBId = String.valueOf(json.getInt("id"));
            String IMDBId = json.getString("imdb_id");
            String date = json.getString("release_date");
            String title = json.getString("original_title");
            result = new Movie(title, TMDBId, IMDBId, collection,date);
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static JSONObject getTMDBJSON(String key) {
        String specificMovieEndpoint = ip + "metadata/" + key + "/" + plexToken;
        Element root = getXML(specificMovieEndpoint);
        Element movie = (Element) root.getElementsByTagName("Video").item(0);
        String guid = movie.getAttribute("guid");
        String regex;
        String json = null;
        String id;
        if(guid.contains("imdb")) {
            regex = "tt[0-9]+";
        }
        else {
            regex = "[0-9]+";
        }
        Matcher matcher = Pattern.compile(regex).matcher(guid);

        if(matcher.find()) {
            id = guid.substring(matcher.start(), matcher.end());
            String url = "https://api.themoviedb.org/3/movie/" + id + "?api_key=f6cb28abe2c490942956b7768bd18d79&language=en-US";
            json = jsonRequest(url);
            System.out.println(json);
        }
        return new JSONObject(json);
    }

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

    private static void findMissingSequels(ArrayList<Movie> movies) {
        HashMap<String, Collection> collections = new HashMap<>();
        int hits = 0;
        for(Movie movie : movies) {
            if(movie.isCollection()) {
                System.out.println("Getting collection for movie " + (hits + 1));
                String id = movie.getCollection();

                if(!collections.containsKey(id)) {
                    try {
                        if(hits % 30 == 0) {
                            sleep(5000);
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                    String url = "https://api.themoviedb.org/3/collection/" + id + "?api_key=f6cb28abe2c490942956b7768bd18d79&language=en-US";
                    System.out.println(url);
                    Collection collection = getCollectionInfo(id, jsonRequest(url));
                    collections.put(collection.getId(), collection);
                    hits++;
                }
                collections.get(id).addMovie(movie);
            }
        }

        for(String id : collections.keySet()) {
            Collection c = collections.get(id);
            if(!c.collectionComplete()) {
                System.out.println(c.getSummary());
            }
        }
    }

    private static Collection getCollectionInfo(String id, String json) {
        Collection collection = null;
        try {
            JSONObject file = new JSONObject(json);
            String title = file.getString("name");
            JSONArray movies = file.getJSONArray("parts");
            HashMap<String, Boolean> parts = new HashMap<>();
            HashMap<String, String> titles = new HashMap<>();
            for(int i = 0; i < movies.length(); i++) {
                JSONObject movie = (JSONObject) movies.get(i);
                if(movie.has("release_date") && !movie.isNull("release_date")) {
                    String date = movie.getString("release_date");
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

    private static boolean validDate(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            // release date is not empty and is not after the current date (movie is released)
            return !date.isEmpty() && format.parse(date).compareTo(new Date()) > 0;
        }
        catch(ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

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

    private static String jsonRequest(String url) {
        String json;
        try {
            StringBuffer response = new StringBuffer();
            URL apiURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) apiURL.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String readLine;

            while((readLine = in.readLine()) != null) {
                response.append(readLine);
            }
            in.close();
            json = response.toString();
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }
}
