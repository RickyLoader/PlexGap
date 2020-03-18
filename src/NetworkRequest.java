import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;

public class NetworkRequest {
    private String body;
    private HashMap<String, String> headers;
    private OkHttpClient client;

    public NetworkRequest(String body, HashMap<String, String> headers) {
        this.body = body;
        this.headers = headers;
        client = new OkHttpClient();
    }

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

            if(headers!=null){
                for(String header : headers.keySet()) {
                    builder.addHeader(header, headers.get(header));
                }
            }

            Response response = client.newCall(builder.build()).execute();
            if(response.isSuccessful()) {
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
