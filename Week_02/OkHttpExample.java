import java.io.IOException;
import java.util.Objects;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpExample {

    public static void main(String[] args) throws IOException {
        String url = "http://localhost:8801/";
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = httpClient.newCall(request).execute();
        System.out.println(Objects.requireNonNull(response.body()).string());
    }

}
