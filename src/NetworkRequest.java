import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;

/**
 * Handle making network requests
 */
public class NetworkRequest {
    private final String body;
    private final HashMap<String, String> headers;
    private final OkHttpClient client;

    /**
     * Create a network request
     *
     * @param body    Body to send
     * @param headers Headers to use
     * @param client  Network client
     */
    public NetworkRequest(String body, HashMap<String, String> headers, OkHttpClient client) {
        this.body = body;
        this.headers = headers;
        this.client = client;
    }

    /**
     * Make a request to the given location
     *
     * @param location URL to make request to
     * @return Response
     */
    public String send(String location) {
        try {
            String json = null;
            Request.Builder builder = new Request.Builder().url(location);

            // POST REQUEST
            if(body != null) {
                RequestBody requestBody = RequestBody.create(
                        MediaType.parse("application/json"), body
                );
                builder.post(requestBody);
            }

            if(headers != null) {
                for(String header : headers.keySet()) {
                    builder.addHeader(header, headers.get(header));
                }
            }

            Response response = client.newCall(builder.build()).execute();
            if(response.isSuccessful() && response.body() != null) {
                json = response.body().string();
            }
            response.close();
            return json;
        }
        catch(IOException e) {
            return null;
        }
    }
}
