package Util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Tiện ích hiển thị hộp thoại thông báo (Alert) trong JavaFX.
 * Hỗ trợ hiển thị an toàn trên JavaFX Application Thread từ bất kỳ luồng nào.
 */
public class AlertUtils {

    /**
     * Hiển thị hộp thoại thông báo (Alert.INFORMATION).
     *
     * @param title   Tiêu đề hộp thoại.
     * @param message Nội dung thông báo.
     */
    public static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    /**
     * Hiển thị hộp thoại cảnh báo (Alert.WARNING).
     *
     * @param title   Tiêu đề hộp thoại.
     * @param message Nội dung cảnh báo.
     */
    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    /**
     * Hiển thị hộp thoại lỗi (Alert.ERROR).
     *
     * @param title   Tiêu đề hộp thoại.
     * @param message Nội dung lỗi.
     */
    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    /**
     * Hiển thị hộp thoại thông báo chung.
     *
     * @param type    Loại alert (INFORMATION, WARNING, ERROR, v.v.).
     * @param title   Tiêu đề hộp thoại.
     * @param message Nội dung thông báo.
     */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        if (Platform.isFxApplicationThread()) {
            createAndShowAlert(type, title, message);
        } else {
            Platform.runLater(() -> createAndShowAlert(type, title, message));
        }
    }

    private static void createAndShowAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị hộp thoại xác nhận (Alert.CONFIRMATION).
     * Chờ và trả về phản hồi của người dùng.
     *
     * @param title   Tiêu đề hộp thoại.
     * @param message Nội dung câu hỏi xác nhận.
     * @return {@code true} nếu người dùng nhấn OK, {@code false} nếu ngược lại.
     */
    public static boolean showConfirm(String title, String message) {
        if (Platform.isFxApplicationThread()) {
            return createAndShowConfirm(title, message);
        } else {
            FutureTask<Boolean> query = new FutureTask<>(() -> createAndShowConfirm(title, message));
            Platform.runLater(query);
            try {
                return query.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private static boolean createAndShowConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
