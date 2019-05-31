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
        String summary = "Trying to put "+movie.getTMDBId() + " in to the collection, current values: ";
        for(String id:movies.keySet()){
            summary+=id+" " + movies.get(id)+"\n";
        }
        System.out.println(summary);
        movies.put(movie.getTMDBId(), true);
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
