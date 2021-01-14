import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Read in and store the various required credentials
 */
public class Credentials {
    private String ip, plexToken, tmdbKey, tmdbReadToken, jsonFileName, libraryID;
    private final boolean authenticated;

    /**
     * Parse the required credentials
     */
    public Credentials() {
        JSONObject credentials = parseCredentialsFile();
        if(credentials == null) {
            authenticated = false;
            return;
        }
        authenticated = readCredentials(credentials);
    }

    /**
     * Read in the credentials file to a JSON object
     *
     * @return JSON object containing credentials
     */
    private JSONObject parseCredentialsFile() {
        try {
            return new JSONObject(new String(Files.readAllBytes(Paths.get("credentials.json"))));
        }
        catch(Exception e) {
            return null;
        }
    }

    /**
     * Read and store the credentials
     *
     * @param credentials JSON credentials
     * @return Valid credentials
     */
    private boolean readCredentials(JSONObject credentials) {
        boolean authenticated = false;
        try {
            ip = credentials.getString("plex_ip");
            plexToken = credentials.getString("plex_token");
            tmdbKey = credentials.getString("tmdb_api_key");
            tmdbReadToken = credentials.getString("tmdb_read_access_token").trim();
            jsonFileName = credentials.getString("json_file");
            libraryID = credentials.getString("library_id");

            if(!ip.isEmpty() && !plexToken.isEmpty() && !tmdbKey.isEmpty() && !tmdbReadToken.isEmpty() && !jsonFileName.isEmpty() && !libraryID.isEmpty()) {
                authenticated = true;
            }
        }
        catch(Exception e) {
            return false;
        }
        return authenticated;
    }

    /**
     * Check if the credentials are valid
     *
     * @return Valid credentials
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Get the desired name of the destination JSON file
     *
     * @return JSON file name
     */
    public String getMovieJsonFileName() {
        return jsonFileName;
    }

    /**
     * Get the Plex server IP
     *
     * @return IP of Plex server
     */
    public String getPlexIp() {
        return ip;
    }

    /**
     * Get the Plex token
     *
     * @return Plex token
     */
    public String getPlexToken() {
        return plexToken;
    }

    /**
     * Get the Plex movie library id
     *
     * @return Plex movie library id
     */
    public String getLibraryID() {
        return libraryID;
    }

    /**
     * Get the TMDB API key
     *
     * @return TMDB API key
     */
    public String getTmdbKey() {
        return tmdbKey;
    }

    /**
     * Get the TMDB read access token
     *
     * @return TMDB read access token
     */
    public String getTmdbReadToken() {
        return tmdbReadToken;
    }
}
