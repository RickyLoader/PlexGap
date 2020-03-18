import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Credentials {

    private String ip = null;
    private String plexToken = null;
    private String tmdbKey = null;
    private String tmdbReadToken = null;
    private String jsonFile = null;
    private String libraryID = null;
    private boolean authenticated;

    public Credentials() {
        authenticated = readCredentials();
    }

    private boolean readCredentials() {
        boolean authenticated = false;
        try {
            String file = new String(Files.readAllBytes(Paths.get("credentials.json")), StandardCharsets.UTF_8);
            JSONObject credentials = new JSONObject(file);
            ip = credentials.getString("plex_ip");
            plexToken = credentials.getString("plex_token");
            tmdbKey = credentials.getString("tmdb_api_key");
            tmdbReadToken = credentials.getString("tmdb_read_access_token").trim();
            jsonFile = credentials.getString("json_file");
            libraryID = credentials.getString("library_id");

            if(!ip.isEmpty() && !plexToken.isEmpty() && !tmdbKey.isEmpty() && !tmdbReadToken.isEmpty() && !jsonFile.isEmpty() && !libraryID.isEmpty()) {
                authenticated = true;
                ip = "http://" + ip + "/library";
                plexToken = "?X-Plex-Token=" + plexToken;
            }
        }
        catch(Exception e) {
            return false;
        }
        return authenticated;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getMovieJsonFileName() {
        return jsonFile;
    }

    public String getPlexIp() {
        return ip;
    }

    public String getPlexToken() {
        return plexToken;
    }

    public String getLibraryID() {
        return libraryID;
    }

    public String getTmdbKey() {
        return tmdbKey;
    }

    public String getTmdbReadToken() {
        return tmdbReadToken;
    }
}
