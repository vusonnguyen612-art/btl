package Network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Lấy thông tin public URL từ ngrok API (http://localhost:4040) để expose server ra ngoài. */
public class NgrokTunnel {
    private static final String NGROK_API_URL = "http://localhost:4040/api/tunnels";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final String host;
    private final int port;

    /** @param host host từ ngrok URL
     *  @param port port từ ngrok URL */
    private NgrokTunnel(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /** Gọi API ngrok để lấy thông tin tunnel public. */
    public static NgrokTunnel fetch() throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NGROK_API_URL))
                .timeout(TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Ngrok API returned status " + response.statusCode());
            }
            return parse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ngrok API request interrupted", e);
        }
    }

    private static NgrokTunnel parse(String json) throws IOException {
        int idx = json.indexOf("\"public_url\"");
        if (idx < 0) {
            throw new IOException("No tunnel found in ngrok API response. Make sure ngrok is running.");
        }

        int colon = json.indexOf(':', idx + "\"public_url\"".length());
        if (colon < 0) throw new IOException("Invalid ngrok public_url format");

        int quoteStart = json.indexOf('"', colon);
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteStart < 0 || quoteEnd < 0) throw new IOException("Invalid ngrok public_url format");

        String publicUrl = json.substring(quoteStart + 1, quoteEnd);

        URI uri = URI.create(publicUrl);
        String host = uri.getHost();
        int port = uri.getPort();

        if (host == null || port < 0) {
            throw new IOException("Could not parse host/port from: " + publicUrl);
        }

        return new NgrokTunnel(host, port);
    }
}
