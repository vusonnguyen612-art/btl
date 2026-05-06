import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import Network.NetworkService;

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