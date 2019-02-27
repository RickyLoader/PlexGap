import static java.lang.Thread.sleep;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlexGaps{

    private static String ip = "http://192.168.1.138:32400/library/";
    private static String plexToken = "?X-Plex-Token=CfsgymkTZzteGH78at3f";

    public static void main(String[] args){
        ArrayList<Movie> movies = getPlexMovies();
        findMissingSequels(movies);
    }


    private static ArrayList<Movie> getPlexMovies(){
        ArrayList<Movie> movies = new ArrayList<>();

        String allMovieEndpoint = ip + "sections/1/all/" + plexToken;
        try{
            Element root = getXML(allMovieEndpoint);
            NodeList movieContainers = root.getElementsByTagName("Video");
            for(int i = 0; i < movieContainers.getLength(); i++){
                Node movieContainer = movieContainers.item(i);
                Element element = (Element) movieContainer;
                String title = element.getAttribute("title");
                System.out.println("Getting info for movie " + (i + 1) + "/" + movieContainers.getLength());
                Movie movie = findMovieInfo(element.getAttribute("ratingKey"), title);
                if(movie != null){
                    movies.add(movie);
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return movies;
    }

    private static Element getXML(String url){
        Element output = null;
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document document = builder.parse(url);
            output = document.getDocumentElement();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return output;
    }

    private static Movie findMovieInfo(String key, String title){
        Movie result = null;

        String specificMovieEndpoint = ip + "metadata/" + key + "/" + plexToken;
        Element root = getXML(specificMovieEndpoint);
        Element movie = (Element) root.getElementsByTagName("Video").item(0);
        String id = movie.getAttribute("guid");
        String regex;
        if(id.contains("imdb")){
            regex = "tt[0-9]+";
        }
        else{
            regex = "[0-9]+";
        }
        Matcher matcher = Pattern.compile(regex).matcher(id);

        if(matcher.find()){
            id = id.substring(matcher.start(), matcher.end());
            String url = "https://api.themoviedb.org/3/movie/" + id + "?api_key=f6cb28abe2c490942956b7768bd18d79&language=en-US";
            String json = jsonRequest(url);
            id = fixID(json);
            String collection = getCollection(json);
            result = new Movie(title, id, collection);
        }
        return result;
    }

    private static String fixID(String json){
        String id = null;
        try{
            JSONObject file = new JSONObject(json);
            id = file.getString("id");
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return id;
    }

    private static void findMissingSequels(ArrayList<Movie> movies){
        HashMap<String, Collection> collections = new HashMap<>();
        int hits = 0;
        for(Movie movie : movies){
            if(movie.isCollection()){
                System.out.println("Getting collection for movie " + (hits + 1));
                String id = movie.getCollection();

                if(!collections.containsKey(id)){
                    try{
                        if(hits % 30 == 0){
                            sleep(5000);
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    String url = "https://api.themoviedb.org/3/collection/" + id + "?api_key=f6cb28abe2c490942956b7768bd18d79&language=en-US";
                    Collection collection = getCollectionInfo(id, jsonRequest(url));
                    collections.put(collection.getId(), collection);
                    hits++;
                }
                collections.get(id).addMovie(movie);
            }
        }

        for(String id : collections.keySet()){
            Collection c = collections.get(id);
            if(!c.collectionComplete()){
                System.out.println(c.getSummary());
            }
        }
    }

    private static Collection getCollectionInfo(String id, String json){
        Collection collection = null;
        try{
            JSONObject file = new JSONObject(json);
            String title = file.getString("name");
            JSONArray movies = file.getJSONArray("parts");
            HashMap<String, Boolean> parts = new HashMap<>();
            HashMap<String, String> titles = new HashMap<>();
            for(int i = 0; i < movies.length(); i++){
                JSONObject movie = (JSONObject) movies.get(i);
                if(movie.has("release_date") && !movie.isNull("release_date")){
                    String date = movie.getString("release_date");
                    if(!date.isEmpty()){
                        parts.put(movie.getString("id"), false);
                        titles.put(movie.getString("id"), movie.getString("title"));
                    }
                }
            }
            collection = new Collection(id, title, parts, titles);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return collection;
    }

    private static String getCollection(String json){

        if(json == null){
            return null;
        }

        String id = null;
        try{
            JSONObject file = new JSONObject(json);
            if(file.has("belongs_to_collection") && !file.isNull("belongs_to_collection")){
                JSONObject collection = file.getJSONObject("belongs_to_collection");
                id = collection.getString("id");
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return id;
    }

    private static String jsonRequest(String url){
        String json = null;
        try{
            StringBuffer response = new StringBuffer();
            URL apiURL = new URL(url);
            HttpURLConnection con = (HttpURLConnection) apiURL.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String readLine;

            while((readLine = in.readLine()) != null){
                response.append(readLine);
            }
            in.close();
            json = response.toString();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return json;
    }
}
