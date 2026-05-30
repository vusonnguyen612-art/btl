package Network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Lấy thông tin public URL từ ngrok API (http://localhost:4040) để expose server ra internet.
 * <p>
 * Sử dụng {@link HttpClient} để gọi REST API của ngrok, parse JSON response thủ công
 * để trích xuất host và port của tunnel public, phục vụ cho kết nối client từ xa.
 * </p>
 */
public class NgrokTunnel {
    private static final String NGROK_API_URL = "http://localhost:4040/api/tunnels";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final String host;
    private final int port;

    /**
     * Khởi tạo đối tượng NgrokTunnel với thông tin host và port đã parse từ ngrok API.
     *
     * @param host Hostname từ public URL của ngrok.
     * @param port Port từ public URL của ngrok.
     */
    private NgrokTunnel(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Lấy hostname của tunnel public.
     *
     * @return Hostname dạng {@code 0.tcp.ap.ngrok.io} hoặc tương tự.
     */
    public String getHost() {
        return host;
    }

    /**
     * Lấy port của tunnel public.
     *
     * @return Số cổng TCP của tunnel.
     */
    public int getPort() {
        return port;
    }

    /**
     * Gọi API ngrok tại {@code http://localhost:4040/api/tunnels} để lấy thông tin tunnel public.
     * Parse JSON response thủ công để trích xuất host và port.
     *
     * @return Đối tượng {@link NgrokTunnel} chứa host và port của tunnel.
     * @throws IOException nếu không thể kết nối đến ngrok API, response không phải 200,
     *                     hoặc không tìm thấy tunnel nào đang chạy.
     */
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

    /**
     * Parse JSON response từ ngrok API để trích xuất host và port của tunnel public đầu tiên.
     * Thực hiện parse thủ công bằng tìm kiếm chuỗi, không dùng thư viện JSON.
     *
     * @param json Chuỗi JSON trả về từ ngrok API.
     * @return Đối tượng {@link NgrokTunnel} chứa host và port.
     * @throws IOException nếu không tìm thấy tunnel, hoặc URL không parse được.
     */
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
