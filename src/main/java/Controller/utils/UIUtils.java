package Controller.utils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

public final class UIUtils {

    private UIUtils() {}

    public static void fixScrollPaneViewport(ScrollPane scrollPane) {
        if (scrollPane == null) return;
        Platform.runLater(() -> {
            Node viewport = scrollPane.lookup(".viewport");
            if (viewport != null) {
                viewport.setStyle("-fx-background-color: #1E1E1D;");
            }
        });
    }

    public static void setupScrollFocus(ScrollPane scrollPane) {
        if (scrollPane != null) {
            scrollPane.setOnMouseEntered(e -> scrollPane.requestFocus());
        }
    }

    public static void resolveAndSetImage(ImageView imageView, String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            imageView.setImage(null);
            imageView.setVisible(false);
            imageView.setManaged(false);
            return;
        }

        String imageUrl;
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            imageUrl = imageFile.toURI().toString();
        } else if (imagePath.startsWith("file:") || imagePath.startsWith("http:") || imagePath.startsWith("https:")) {
            imageUrl = imagePath;
        } else {
            imageView.setImage(null);
            imageView.setVisible(false);
            imageView.setManaged(false);
            return;
        }

        imageView.setImage(new Image(imageUrl, true));
        imageView.setVisible(true);
        imageView.setManaged(true);
    }

    public static Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 18px;");
        return label;
    }

    public static void initSearchCombos(ComboBox<String> categoryCombo, ComboBox<String> statusCombo, ComboBox<String> sortCombo) {
        if (categoryCombo != null) {
            categoryCombo.getItems().add("Tất cả");
            categoryCombo.getItems().addAll("Điện tử", "Xe cộ", "Nghệ thuật", "Thời trang", "Sách", "Thể thao", "Trang sức", "Âm nhạc", "Nội thất");
            categoryCombo.getSelectionModel().select("Tất cả");
        }
        if (statusCombo != null) {
            statusCombo.getItems().add("Tất cả");
            statusCombo.getItems().addAll("Đang diễn ra", "Sắp diễn ra", "Chờ thanh toán", "Đã kết thúc", "Đã hủy");
            statusCombo.getSelectionModel().select("Tất cả");
        }
        if (sortCombo != null) {
            sortCombo.getItems().addAll("Mới nhất", "Cũ nhất", "Giá tăng dần", "Giá giảm dần", "Tên A-Z");
            sortCombo.getSelectionModel().select("Mới nhất");
        }
    }

    public static void resetSearchFields(javafx.scene.control.TextField keyword,
                                          ComboBox<String> categoryCombo,
                                          ComboBox<String> statusCombo,
                                          javafx.scene.control.TextField minPrice,
                                          javafx.scene.control.TextField maxPrice,
                                          ComboBox<String> sortCombo) {
        if (keyword != null) keyword.clear();
        if (categoryCombo != null) categoryCombo.getSelectionModel().select("Tất cả");
        if (statusCombo != null) statusCombo.getSelectionModel().select("Tất cả");
        if (minPrice != null) minPrice.clear();
        if (maxPrice != null) maxPrice.clear();
        if (sortCombo != null) sortCombo.getSelectionModel().select("Mới nhất");
    }
}
