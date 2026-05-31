package Controller;

import Controller.utils.AlertUtils;
import Controller.utils.CategoryMapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import Model.Item;
import Model.AuctionSession;
import DAO.ItemDAO;
import DAO.AuctionSessionDAO;
import Factory.ItemFactory;
import Model.User;
import Network.NetworkService;
import Network.Message;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Controller cho form tạo sản phẩm mới (FXML: CreateItems.fxml). Hỗ trợ cả chế độ tạo mới và chỉnh sửa. */
public class CreateItemsController implements UserController.LinkedController {

    private User currentUser;
    private boolean editMode = false;
    private Item editingItem;

    /**
     * Gán {@link UserController} và lấy thông tin người dùng hiện tại
     * để sử dụng trong việc tạo sản phẩm mới.
     *
     * @param userController UserController quản lý điều khiển người dùng
     */
    @Override
    public void setUserController(UserController userController) {
        this.currentUser = userController.getCurrentUser();
    }

    @FXML
    private TextField tenSanPham;

    @FXML
    private TextField giaKhoiDau;

    @FXML
    private TextField moTa;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextField thoiGianDauGia;

    @FXML
    private Label thoiGianLabel;

    @FXML
    private HBox durationButtonBox;

    @FXML
    private Button btn30Phut;

    @FXML
    private Button btn60Phut;

    @FXML
    private Button btn120Phut;

    @FXML
    private Button btn180Phut;

    @FXML
    private ImageView productImageView;

    @FXML
    private Button chonAnhButton;

    @FXML
    private Button taoSanPhamButton;

    private long selectedDuration = 60;
    private File selectedImageFile;

    private final String BUTTON_STYLE_DEFAULT =
            "-fx-background-color: #333333; -fx-text-fill: white; -fx-background-radius: 15;";

    private final String BUTTON_STYLE_SELECTED =
            "-fx-background-color: #d9b15f; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15;";

    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionSessionDAO sessionDAO = new AuctionSessionDAO();
    private final NetworkService networkService = NetworkService.getInstance();

    /** Đặt chế độ chỉnh sửa với dữ liệu sản phẩm và phiên hiện tại. */
    public void setEditMode(Item item) {
        this.editMode = true;
        this.editingItem = item;

        tenSanPham.setText(item.getName());
        giaKhoiDau.setText(String.valueOf((long) item.getStartPrice()));
        moTa.setText(item.getDescription());
        categoryComboBox.setValue(CategoryMapper.toVietnamese(item.getCategory()));
        categoryComboBox.setDisable(true);

        thoiGianLabel.setVisible(false);
        thoiGianLabel.setManaged(false);
        thoiGianDauGia.setVisible(false);
        thoiGianDauGia.setManaged(false);
        durationButtonBox.setVisible(false);
        durationButtonBox.setManaged(false);

        taoSanPhamButton.setText("Lưu thay đổi");

        if (item.getImagePath() != null && !item.getImagePath().isBlank()) {
            File imgFile = new File(item.getImagePath());
            if (imgFile.exists()) {
                productImageView.setImage(new Image(imgFile.toURI().toString(), true));
            }
        }
    }

    @FXML
    /** Khởi tạo ComboBox danh mục và giá trị mặc định. */
    private void initialize() {
        thoiGianDauGia.setText("60");
        categoryComboBox.getItems().addAll("Nghệ thuật", "Điện tử", "Xe cộ", "Thời trang", "Sách", "Thể thao", "Trang sức", "Âm nhạc", "Nội thất");
        categoryComboBox.setValue("Nghệ thuật");
    }

    /**
     * Mở hộp thoại chọn file ảnh sản phẩm từ hệ thống.
     * Hỗ trợ các định dạng: PNG, JPG, JPEG, GIF, BMP.
     * Sau khi chọn, ảnh sẽ được hiển thị preview trên ImageView.
     *
     * @param event sự kiện nhấp chuột nút "Chọn ảnh"
     */
    @FXML
    private void chonAnhSanPham(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) chonAnhButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        selectedImageFile = file;
        productImageView.setImage(new Image(file.toURI().toString(), true));
    }

    @FXML
    /** Chọn thời gian 30 phút. */
    private void setDuration30(ActionEvent event) {
        selectedDuration = 30;
        thoiGianDauGia.setText("30");
        updateDurationButtons(btn30Phut);
    }

    @FXML
    /** Chọn thời gian 60 phút (mặc định). */
    private void setDuration60(ActionEvent event) {
        selectedDuration = 60;
        thoiGianDauGia.setText("60");
        updateDurationButtons(btn60Phut);
    }

    @FXML
    /** Chọn thời gian 120 phút. */
    private void setDuration120(ActionEvent event) {
        selectedDuration = 120;
        thoiGianDauGia.setText("120");
        updateDurationButtons(btn120Phut);
    }

    @FXML
    /** Chọn thời gian 180 phút. */
    private void setDuration180(ActionEvent event) {
        selectedDuration = 180;
        thoiGianDauGia.setText("180");
        updateDurationButtons(btn180Phut);
    }

    /**
     * Cập nhật kiểu hiển thị của các nút chọn thời gian đấu giá.
     * Nút được chọn sẽ hiển thị kiểu nổi bật, các nút còn lại hiển thị kiểu mặc định.
     *
     * @param selected nút đang được chọn
     */
    private void updateDurationButtons(Button selected) {
        Button[] buttons = {btn30Phut, btn60Phut, btn120Phut, btn180Phut};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.setStyle(btn == selected ? BUTTON_STYLE_SELECTED : BUTTON_STYLE_DEFAULT);
            }
        }
    }

    @FXML
    /** Tạo sản phẩm mới hoặc cập nhật sản phẩm hiện tại. */
    private void TaoSanPham(ActionEvent event) {
        if (editMode) {
            saveEdit();
        } else {
            createNew();
        }
    }

    /**
     * Lưu thay đổi chỉnh sửa sản phẩm.
     * Validate dữ liệu đầu vào, cập nhật sản phẩm trên server,
     * lưu ảnh mới nếu có, và đóng form.
     */
    private void saveEdit() {
        String ten = tenSanPham.getText().trim();
        String giaStr = giaKhoiDau.getText().trim();
        String Mota = moTa.getText().trim();

        if (ten.isBlank()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng nhập tên sản phẩm.");
            return;
        }

        if (giaStr.isBlank()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng nhập giá khởi đầu.");
            return;
        }

        try {
            BigDecimal gia = new BigDecimal(giaStr);
            if (gia.compareTo(BigDecimal.ZERO) <= 0) {
                AlertUtils.showWarning("Lỗi", "Giá phải lớn hơn 0.");
                return;
            }

            editingItem.setName(ten);
            editingItem.setDescription(Mota);
            editingItem.setStartPrice(gia.doubleValue());

            if (selectedImageFile != null) {
                try {
                    editingItem.setImagePath(saveSelectedImage(editingItem.getId()));
                } catch (IOException e) {
                    AlertUtils.showWarning("Lỗi", "Không thể lưu ảnh sản phẩm: " + e.getMessage());
                    return;
                }
            }

            Message response = networkService.updateItem(editingItem);
            if (response.getType() == Message.Type.SUCCESS) {
                AlertUtils.showInfo("Thành công", "Cập nhật sản phẩm thành công!");
                closeWindow();
            } else {
                AlertUtils.showWarning("Lỗi", response.getContent());
            }

        } catch (NumberFormatException e) {
            AlertUtils.showWarning("Lỗi", "Giá không hợp lệ.");
        } catch (Exception e) {
            AlertUtils.showWarning("Lỗi", "Không thể cập nhật sản phẩm: " + e.getMessage());
        }
    }

    /**
     * Tạo sản phẩm mới và phiên đấu giá tương ứng.
     * Validate tất cả dữ liệu đầu vào (tên, giá, thời gian),
     * lưu sản phẩm và phiên đấu giá trên server, hiển thị thông báo thành công.
     */
    private void createNew() {
        String ten = tenSanPham.getText().trim();
        String giaStr = giaKhoiDau.getText().trim();
        String Mota = moTa.getText().trim();
        String thoiGianStr = thoiGianDauGia.getText().trim();

        if (ten.isBlank()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng nhập tên sản phẩm.");
            return;
        }

        if (giaStr.isBlank()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng nhập giá khởi đầu.");
            return;
        }

        if (thoiGianStr.isBlank()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng nhập thời gian đấu giá.");
            return;
        }

        try {
            BigDecimal gia = new BigDecimal(giaStr);
            if (gia.compareTo(BigDecimal.ZERO) <= 0) {
                AlertUtils.showWarning("Lỗi", "Giá phải lớn hơn 0.");
                return;
            }

            long duration;
            try {
                duration = Long.parseLong(thoiGianStr);
                if (duration <= 0) {
                    AlertUtils.showWarning("Lỗi", "Thời gian phải lớn hơn 0 phút.");
                    return;
                }
                if (duration > 1440) {
                    AlertUtils.showWarning("Lỗi", "Thời gian tối đa là 1440 phút (24 giờ).");
                    return;
                }
            } catch (NumberFormatException e) {
                AlertUtils.showWarning("Lỗi", "Thời gian không hợp lệ.");
                return;
            }

            String category = CategoryMapper.toEnglish(categoryComboBox.getValue());
            Item item = ItemFactory.createItem(category, ten, Mota, gia.doubleValue(), currentUser.getId());
            item.setSellerId(currentUser.getId());
            if (selectedImageFile != null) {
                try {
                    item.setImagePath(saveSelectedImage(item.getId()));
                } catch (IOException e) {
                    AlertUtils.showWarning("Lỗi", "Không thể lưu ảnh sản phẩm: " + e.getMessage());
                    return;
                }
            }

            if (itemDAO.save(item)) {
                String sessionId = UUID.randomUUID().toString();
                AuctionSession session = new AuctionSession(
                    sessionId,
                    item,
                    currentUser.getId(),
                    gia.doubleValue(),
                    duration
                );
                sessionDAO.save(session);

                String durationText = duration >= 60
                        ? (duration / 60) + " giờ" + (duration % 60 > 0 ? " " + (duration % 60) + " phút" : "")
                        : duration + " phút";

                AlertUtils.showInfo("Thành công", "Tạo sản phẩm thành công!\nThời gian đấu giá: " + durationText);
                closeWindow();
            } else {
                AlertUtils.showWarning("Lỗi", "Không thể tạo sản phẩm.");
            }

        } catch (NumberFormatException e) {
            AlertUtils.showWarning("Lỗi", "Giá không hợp lệ.");
        }
    }

    /**
     * Chuyển đổi tên danh mục từ tiếng Việt sang mã tiếng Anh tương ứng để lưu trữ trong database.
     *
     * @param vietnamese Tên danh mục tiếng Việt từ giao diện.
     * @return Mã danh mục tiếng Anh chuẩn hoặc giá trị gốc nếu không khớp.
     */
    private String mapCategoryToEnglish(String vietnamese) {
        return switch (vietnamese) {
            case "Nghệ thuật" -> "ART";
            case "Điện tử" -> "ELECTRONICS";
            case "Xe cộ" -> "VEHICLE";
            case "Thời trang" -> "FASHION";
            case "Sách" -> "BOOKS";
            case "Thể thao" -> "SPORTS";
            case "Trang sức" -> "JEWELRY";
            case "Âm nhạc" -> "MUSIC";
            case "Nội thất" -> "FURNITURE";
            default -> vietnamese;
        };
    }

    /**
     * Chuyển đổi mã danh mục tiếng Anh từ database sang tên tiếng Việt tương ứng để hiển thị trên giao diện.
     *
     * @param english Mã danh mục tiếng Anh từ database.
     * @return Tên danh mục tiếng Việt tương ứng hoặc giá trị gốc nếu không khớp.
     */
    private String mapCategoryToVietnamese(String english) {
        return switch (english.toUpperCase()) {
            case "ART" -> "Nghệ thuật";
            case "ELECTRONICS" -> "Điện tử";
            case "VEHICLE" -> "Xe cộ";
            case "FASHION" -> "Thời trang";
            case "BOOKS" -> "Sách";
            case "SPORTS" -> "Thể thao";
            case "JEWELRY" -> "Trang sức";
            case "MUSIC" -> "Âm nhạc";
            case "FURNITURE" -> "Nội thất";
            default -> english;
        };
    }

    /**
     * Lưu ảnh sản phẩm được chọn vào thư mục image/uploads trên hệ thống.
     * File ảnh sẽ được đổi tên thành mã sản phẩm + phần mở rộng gốc để tránh trùng lặp.
     *
     * @param itemId mã sản phẩm dùng làm tên file
     * @return đường dẫn tuyệt đối đến file ảnh đã lưu
     * @throws IOException nếu có lỗi khi tạo thư mục hoặc copy file
     */
    private String saveSelectedImage(String itemId) throws IOException {
        String fileName = selectedImageFile.getName();
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = fileName.substring(dotIndex).toLowerCase();
        }

        Path uploadDir = Path.of("image", "uploads");
        Files.createDirectories(uploadDir);

        String targetFileName = itemId + extension;
        Path targetPath = uploadDir.resolve(targetFileName);
        Files.copy(selectedImageFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toString();
    }

    /**
     * Đóng cửa sổ form tạo/sửa sản phẩm hiện tại.
     */
    private void closeWindow() {
        Stage stage = (Stage) tenSanPham.getScene().getWindow();
        stage.close();
    }
}
