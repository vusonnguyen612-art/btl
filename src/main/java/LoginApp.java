import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import Network.NetworkService;

/**
 * Lớp chính (Main Application) JavaFX — khởi động ứng dụng đấu giá.
 * <p>
 * Khi khởi động:
 * <ol>
 *   <li>Kết nối đến TCP server qua {@link NetworkService} (nếu chưa kết nối)</li>
 *   <li>Load giao diện {@code login.fxml} từ resources</li>
 *   <li>Hiển thị màn hình đăng nhập (600×400)</li>
 * </ol>
 * Khi đóng ứng dụng ({@link #stop()}), ngắt kết nối network.
 */
public class LoginApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        NetworkService networkService = NetworkService.getInstance();
        if (!networkService.isConnected()) {
            networkService.connect();
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Scene scene = new Scene(loader.load(), 600, 400);
        stage.setTitle("Dang nhap");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        NetworkService.getInstance().disconnect();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
