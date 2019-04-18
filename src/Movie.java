public class Movie{
    private String title;
    private String TMDBId;
    private String IMDBId;
    private String releaseDate;
    private String collection;

    public Movie(String title, String TMDBId, String IMDBId,String collection,String releaseDate){
        this.title = title;
        this.TMDBId = TMDBId;
        this.IMDBId = IMDBId;
        this.collection = collection;
        this.releaseDate = releaseDate;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getId(){
        return TMDBId;
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

    public void getSummary(){
        System.out.println("title: "+title+"\n"+"id: "+TMDBId+"\n"+"collection: "+collection);
    }

    public String getIMDBId() {
        return IMDBId;
    }
}
