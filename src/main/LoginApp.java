import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class LoginApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxmlUrl = getClass().getResource("/login.fxml");

        if (fxmlUrl == null) {
            throw new IllegalStateException(
                    "Không tìm thấy file login.fxml. Hãy đặt file vào src/main/resources/login.fxml"
            );
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }
}