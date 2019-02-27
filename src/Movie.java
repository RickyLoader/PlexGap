public class Movie{
    private String title;
    private String id;
    private String collection;

    public Movie(String title, String id, String collection){
        this.title = title;
        this.id = id;
        this.collection = collection;
    }

    public String getId(){
        return id;
    }

    public String getCollection(){
        return collection;
    }

    public String getTitle(){
        return title;
    }

    public boolean isCollection(){
        return collection != null;
    }
}
