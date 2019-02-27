import java.util.HashMap;

public class Collection{
    private String title;
    private HashMap<String, Boolean> movies;
    private HashMap<String, String> titles;
    private String id;

    public Collection(String id, String title, HashMap<String, Boolean> movies, HashMap<String, String> titles){
        this.title = title;
        this.movies = movies;
        this.id = id;
        this.titles = titles;
    }

    public String getId(){
        return id;
    }

    public void addMovie(Movie movie){
        movies.put(movie.getId(), true);
    }

    public boolean collectionComplete(){
        for(String movie : movies.keySet()){
            if(!movies.get(movie)){
                return false;
            }
        }
        return true;
    }

    public String getSummary(){
        StringBuilder summary = new StringBuilder(id + " " + title + ":\n");
        for(String movie : movies.keySet()){
            if(!movies.get(movie)){
                summary.append(movie + ": " + titles.get(movie) + " \n");
            }
        }
        return summary.toString();
    }
}
