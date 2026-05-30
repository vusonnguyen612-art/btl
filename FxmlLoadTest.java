import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.net.URL;

public class FxmlLoadTest {
    public static void main(String[] args) throws Exception {
        try {
            URL resource = FxmlLoadTest.class.getResource("/signin.fxml");
            System.out.println("Resource: " + resource);
            if (resource == null) {
                System.out.println("Resource NOT FOUND!");
                return;
            }
            Parent root = FXMLLoader.load(resource);
            System.out.println("Loaded successfully!");
            System.out.println("Root class: " + root.getClass().getName());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace(System.out);
            if (e.getCause() != null) {
                System.out.println("Caused by: " + e.getCause().getMessage());
                e.getCause().printStackTrace(System.out);
            }
        }
    }
}
