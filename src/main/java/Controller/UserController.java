package Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import Model.Item;
import Model.User;
import Model.AuctionSession;
import Model.Bid;
import Network.NetworkService;
import Network.Message;

/** Controller chính: quản lý menu, hiển thị sản phẩm, kho, nạp tiền, cài đặt. */
public class UserController {

    @FXML
    private Label Name;

    @FXML
    private Label Sodutaikhoan1;

    @FXML
    private Label Sodutaikhoan;

    @FXML
    private ToggleButton TrangchuButton;

    @FXML
    private ToggleButton KhoButton;

    @FXML
    private ToggleButton NaptienButton;

    @FXML
    private ToggleButton BidHistory;

    @FXML
    private ToggleButton CaidatButton;

    @FXML
    private AnchorPane TrangchuPane;

    @FXML
    private AnchorPane KhoPane;

    @FXML
    private AnchorPane NaptienPane;

    @FXML
    private AnchorPane LichsudaugiaPane;

    @FXML
    private AnchorPane CaidatPane;

    @FXML
    private VBox AllItems;

    @FXML
    private VBox Items;

    @FXML
    private TextField Nganhangnaptien;

    @FXML
    private TextField Sotaikhoannaptien;

    @FXML
    private TextField Sotiencannap;

    @FXML
    private TextField Matkhauhientai;

    @FXML
    private TextField Matkhaumoi;

    @FXML
    private TextField Nhaplaimatkhaumoi;

    private final ToggleGroup menuGroup = new ToggleGroup();

    private User currentUser;
    private NetworkService networkService = NetworkService.getInstance();
    private BigDecimal soDuTaiKhoan = new BigDecimal("300000");

    private static final String MENU_STYLE =
            "-fx-background-color: #1E1E1D;" +
                    "-fx-text-fill: #eacd8f;";

    private static final String MENU_SELECTED_STYLE =
            "-fx-background-color: #d9b15f;" +
                    "-fx-text-fill: #1E1E1D;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 20;";

    private static final String CREATE_ITEM_FXML = "/CreateItems.fxml";
    private static final String LOGIN_FXML = "/login.fxml";

    @FXML
    /** Khởi tạo: setup menu buttons, load dữ liệu trang chủ và kho. */
    private void initialize() {
        setupMenuButtons();
        setupDefaultScreen();
        loadHomeItems();
        loadWarehouseItems();
    }

    private void setupMenuButtons() {
        bindMenuButton(TrangchuButton, TrangchuPane);
        bindMenuButton(KhoButton, KhoPane);
        bindMenuButton(NaptienButton, NaptienPane);
        bindMenuButton(BidHistory, LichsudaugiaPane);
        bindMenuButton(CaidatButton, CaidatPane);
    }

    private void bindMenuButton(ToggleButton button, AnchorPane pane) {
        if (button == null || pane == null) {
            return;
        }

        button.setToggleGroup(menuGroup);
        button.setStyle(MENU_STYLE);

        button.setOnAction(event -> {
            showPane(pane);
            menuGroup.selectToggle(button);
            updateMenuStyle();
        });
    }

    private void setupDefaultScreen() {
        showPane(TrangchuPane);

        if (TrangchuButton != null) {
            menuGroup.selectToggle(TrangchuButton);
        }

        updateMenuStyle();
    }

    private void showPane(AnchorPane paneToShow) {
        AnchorPane[] panes = {
                TrangchuPane,
                KhoPane,
                NaptienPane,
                LichsudaugiaPane,
                CaidatPane
        };

        for (AnchorPane pane : panes) {
            if (pane != null) {
                pane.setVisible(false);
                pane.setManaged(false);
            }
        }

        if (paneToShow != null) {
            paneToShow.setVisible(true);
            paneToShow.setManaged(true);
        }
    }

    private void updateMenuStyle() {
        ToggleButton[] buttons = {
                TrangchuButton,
                KhoButton,
                NaptienButton,
                BidHistory,
                CaidatButton
        };

        for (ToggleButton button : buttons) {
            if (button == null) {
                continue;
            }

            if (button.isSelected()) {
                button.setStyle(MENU_SELECTED_STYLE);
            } else {
                button.setStyle(MENU_STYLE);
            }
        }
    }

    /** Format số tiền với dấu phẩy phân cách. */
    private String formatMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');

        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(value);
    }

    /** Parse chuỗi tiền tệ (loại bỏ ký tự đặc biệt, dấu phẩy). */
    private BigDecimal parseMoney(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền cần nạp!");
        }

        String normalized = rawText
                .trim()
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "")
                .replace("$", "")
                .replace("\u20ab", "");

        if (!normalized.matches("\\d+")) {
            throw new IllegalArgumentException("Số tiền không hợp lệ!.");
        }

        BigDecimal amount = new BigDecimal(normalized);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0!");
        }

        return amount;
    }

    @FXML
    /** Xử lý nạp tiền: kiểm tra input, gọi NetworkService.deposit(), cập nhật số dư. */
    private void Naptien(ActionEvent event) {
        try {
            String nganHang = Nganhangnaptien.getText().trim();
            String soTaiKhoan = Sotaikhoannaptien.getText().trim();
            String soTienRaw = Sotiencannap.getText().trim();

            if (nganHang.isEmpty()) {
                showWarning("Thiếu thông tin!  ", "Vui lòng nhập tên ngân hàng!");
                return;
            }

            if (soTaiKhoan.isEmpty()) {
                showWarning("Thiếu thông tin!", "Vui lòng nhập số tài khoản!");
                return;
            }

            BigDecimal soTienNap = parseMoney(soTienRaw);

            Message response = networkService.deposit(soTienNap);
            if (response.getType() == Message.Type.SUCCESS && response.getData() != null) {
                soDuTaiKhoan = (BigDecimal) response.getData();
                updateBalanceLabels();

                Nganhangnaptien.clear();
                Sotaikhoannaptien.clear();
                Sotiencannap.clear();

                showInfo("Nạp tiền thành công", "Bạn đã nạp thêm " + formatMoney(soTienNap) + " $ vào tài khoản.");
            } else {
                showError("Lỗi", "Nạp tiền thất bại: " + response.getContent());
            }

        } catch (IllegalArgumentException e) {
            showWarning("Dữ liệu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            showError("Lỗi", "Không thể nạp tiền. Chi tiết: " + e.getMessage());
        }
    }

    @FXML
    /** Xử lý đổi mật khẩu (chưa hỗ trợ server-side). */
    private void Doimatkhau(ActionEvent event) {
        if (currentUser == null) {
            showWarning("Lỗi", "Vui lòng đăng nhập.");
            return;
        }

        String matKhauHienTai = Matkhauhientai.getText();
        String matKhauMoi = Matkhaumoi.getText();
        String nhapLaiMatKhauMoi = Nhaplaimatkhaumoi.getText();

        if (matKhauHienTai.isBlank() || matKhauMoi.isBlank() || nhapLaiMatKhauMoi.isBlank()) {
            showWarning("Dữ liệu thiếu", "Vui long nhập đầy đủ thông tin.");
            return;
        }

        if (!matKhauMoi.equals(nhapLaiMatKhauMoi)) {
            showWarning("Lỗi", "Mật khẩu mới không khớp.");
            return;
        }

        if (matKhauMoi.length() < 6) {
            showWarning("Lỗi", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return;
        }

        showWarning("Chưa hỗ trợ", "Tính năng đổi mật khẩu qua Server chưa được cài đặt.");

        Matkhauhientai.clear();
        Matkhaumoi.clear();
        Nhaplaimatkhaumoi.clear();
    }

    @FXML
    /** Mở modal tạo sản phẩm, sau đó reload dữ liệu. */
    private void CreateItems(ActionEvent event) {
        openModalFXML(CREATE_ITEM_FXML, "Tạo sản phẩm");
        loadHomeItems();
        loadWarehouseItems();
    }

    @FXML
    /** Mở phòng đấu giá (modal), reload số dư khi đóng. */
    private void OpenAuctionRoom(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionRoom.fxml"));
            Parent root = loader.load();

            AuctionRoomController auctionRoomController = loader.getController();
            auctionRoomController.setCurrentUser(currentUser);

            Stage stage = new Stage();
            stage.setTitle("Phong Dau Gia");
            stage.setScene(new Scene(root, 900, 600));
            stage.initOwner(getCurrentStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setOnHidden(e -> {
                auctionRoomController.stopRefresh();
                Message balanceResponse = networkService.getUserBalance();
                if (balanceResponse.getType() == Message.Type.SUCCESS && balanceResponse.getData() != null) {
                    soDuTaiKhoan = (BigDecimal) balanceResponse.getData();
                    updateBalanceLabels();
                }
                loadHomeItems();
                loadWarehouseItems();
            });
            stage.show();

        } catch (Exception e) {
            showError("Lỗi", "Không thể mở phòng đấuu giá: " + e.getMessage());
        }
    }

    @FXML
    /** Đăng xuất và quay về màn hình đăng nhập. */
    private void Doitaikhoan(ActionEvent event) {
        boolean confirmed = showConfirm(
                "Đổi tài khoản",
                "Bạn có chắc muốn đăng xuất và đổi tài khoản không?"
        );

        if (!confirmed) {
            return;
        }

        switchScene(LOGIN_FXML, "Đăng nhập");
    }

    @FXML
    /** Thoát chương trình (có xác nhận). */
    private void Exit(ActionEvent event) {
        boolean confirmed = showConfirm(
                "Thoát",
                "Bạn có chắc chắn muốn thoát chương trình không?"
        );

        if (!confirmed) {
            return;
        }

        Stage stage = getCurrentStage();
        if (stage != null) {
            stage.close();
        }
    }

    /** Gán dữ liệu người dùng hiện tại và cập nhật UI. */
    public void setUserData(User user) {
        this.currentUser = user;

        if (user != null && user.getUsername() != null) {
            if (Name != null) {
                Name.setText(user.getUsername());
            }
        }

        Message response = networkService.getUserBalance();
        if (response.getType() == Message.Type.SUCCESS && response.getData() != null) {
            this.soDuTaiKhoan = (BigDecimal) response.getData();
        }

        updateBalanceLabels();
        loadHomeItems();
        loadWarehouseItems();
    }

    private void updateBalanceLabels() {
        String balanceText = formatMoney(soDuTaiKhoan);

        if (Sodutaikhoan != null) {
            Sodutaikhoan.setText(balanceText);
        }

        if (Sodutaikhoan1 != null) {
            Sodutaikhoan1.setText(balanceText);
        }
    }

    public BigDecimal getSoDuTaiKhoan() {
        return soDuTaiKhoan;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /** Refresh toàn bộ dữ liệu hiển thị. */
    public void refreshAllData() {
        updateBalanceLabels();
        loadHomeItems();
        loadWarehouseItems();
    }

    private void loadHomeItems() {
        if (AllItems == null) {
            return;
        }

        AllItems.getChildren().clear();

        try {
            Message itemsResponse = networkService.getItems();
            Message auctionsResponse = networkService.getAuctions();
            if (itemsResponse.getType() == Message.Type.SUCCESS && itemsResponse.getData() instanceof List) {
                List<Item> allItems = (List<Item>) itemsResponse.getData();
                List<AuctionSession> allAuctions = (auctionsResponse.getType() == Message.Type.SUCCESS && auctionsResponse.getData() instanceof List)
                        ? (List<AuctionSession>) auctionsResponse.getData() : List.of();

                if (allItems.isEmpty()) {
                    Label emptyLabel = createEmptyLabel("Chưa có sản phẩm đấu giá nào.");
                    AllItems.getChildren().add(emptyLabel);
                } else {
                    Label headerLabel = new Label("Sản phẩm đấu giá");
                    headerLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 20px; -fx-font-weight: bold;");
                    headerLabel.setPadding(new Insets(0, 0, 10, 0));
                    AllItems.getChildren().add(headerLabel);

                    List<Item> runningItems = new java.util.ArrayList<>();
                    List<Item> otherItems = new java.util.ArrayList<>();
                    java.util.Set<String> runningItemIds = new java.util.HashSet<>();
                    for (AuctionSession auction : allAuctions) {
                        if (auction.isRunning() && auction.getItem() != null) {
                            runningItemIds.add(auction.getItem().getId());
                        }
                    }
                    for (Item item : allItems) {
                        if (runningItemIds.contains(item.getId())) {
                            runningItems.add(item);
                        } else {
                            otherItems.add(item);
                        }
                    }
                    runningItems.addAll(otherItems);

                    for (Item item : runningItems) {
                        HBox itemBox = createItemCard(item, allAuctions);
                        AllItems.getChildren().add(itemBox);
                    }
                }
            }
        } catch (Exception e) {
            Label errorLabel = createEmptyLabel("Lỗi tại sản phẩm.");
            AllItems.getChildren().add(errorLabel);
        }
    }

    private HBox createItemCard(Item item, List<AuctionSession> auctions) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/item_card.fxml"));
            HBox card = loader.load();
            ItemCardController controller = loader.getController();
            controller.setItem(item, auctions);
            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return new HBox();
        }
    }

    private void loadWarehouseItems() {
        if (Items == null) {
            return;
        }

        Items.getChildren().clear();

        if (currentUser == null) {
            Label emptyLabel = createEmptyLabel("Vui lòng đăng nhập.");
            Items.getChildren().add(emptyLabel);
            return;
        }

        try {
            Message itemsResponse = networkService.getItems();
            Message auctionsResponse = networkService.getAuctions();

            List<Item> userItems = (itemsResponse.getType() == Message.Type.SUCCESS && itemsResponse.getData() instanceof List)
                    ? (List<Item>) itemsResponse.getData() : List.of();
            List<AuctionSession> userAuctions = (auctionsResponse.getType() == Message.Type.SUCCESS && auctionsResponse.getData() instanceof List)
                    ? (List<AuctionSession>) auctionsResponse.getData() : List.of();

            if (userItems.isEmpty() && userAuctions.isEmpty()) {
                Label emptyLabel = createEmptyLabel("Kho của bạn hiện chưa có sản phẩm.");
                Items.getChildren().add(emptyLabel);
            } else {
                Label headerLabel = new Label("Phiên đấu giá của bạn");
                headerLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 18px; -fx-font-weight: bold;");
                headerLabel.setPadding(new Insets(0, 0, 10, 0));
                Items.getChildren().add(headerLabel);

                boolean hasAuctions = false;
                for (AuctionSession auction : userAuctions) {
                    if (auction.getSellerId() != null && auction.getSellerId().equals(currentUser.getId())) {
                        HBox auctionCard = createAuctionCard(auction);
                        Items.getChildren().add(auctionCard);
                        hasAuctions = true;
                    }
                }

                if (!hasAuctions && !userItems.isEmpty()) {
                    Label noAuctionLabel = new Label("Chưa tạo phiên đấu giá. Nhấn 'Tạo sản phẩm để bắt đầu.");
                    noAuctionLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
                    noAuctionLabel.setPadding(new Insets(10, 0, 10, 0));
                    Items.getChildren().add(noAuctionLabel);
                }
            }
        } catch (Exception e) {
            Label errorLabel = createEmptyLabel("Lỗi tại kho: " + e.getMessage());
            Items.getChildren().add(errorLabel);
        }
    }

    private HBox createAuctionCard(AuctionSession auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction_card.fxml"));
            HBox card = loader.load();
            AuctionCardController controller = loader.getController();

            boolean isRunning = auction.isRunning();
            boolean canStart = auction.isOpen();

            controller.setAuction(auction, isRunning, canStart);
            controller.setOnStartAuction(() -> {
                Message response = networkService.startAuction(auction.getId());
                if (response.getType() == Message.Type.SUCCESS) {
                    showInfo("Thành công", "Đã bắt đầu phiên đấu giá.");
                    loadWarehouseItems();
                    loadHomeItems();
                } else {
                    showError("Lỗi", "Không thể bắt đầu phiên đấu giá.");
                }
            });

            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return new HBox();
        }
    }

    private void loadBidHistory() {
        if (LichsudaugiaPane == null) {
            return;
        }

        LichsudaugiaPane.getChildren().clear();

        if (currentUser == null) {
            Label label = createEmptyLabel("Vui lòng đăng nhập để xem lịch sử.");
            label.setLayoutX(150);
            label.setLayoutY(250);
            LichsudaugiaPane.getChildren().add(label);
            return;
        }

        Label label = createEmptyLabel("Chưa có lịch sử đấu giá");
        label.setLayoutX(220);
        label.setLayoutY(250);
        LichsudaugiaPane.getChildren().add(label);
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 18px;");
        return label;
    }

    private String formatDuration(long minutes) {
        if (minutes >= 60) {
            long hours = minutes / 60;
            long mins = minutes % 60;
            if (mins > 0) {
                return hours + "h " + mins + "p";
            }
            return hours + "h";
        }
        return minutes + " phút";
    }

    private void openModalFXML(String fxmlPath, String title) {
        try {
            URL resourceUrl = getClass().getResource(fxmlPath);
            if (resourceUrl == null) {
                resourceUrl = Thread.currentThread().getContextClassLoader().getResource(
                    fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath
                );
            }
            System.out.println("Loading FXML from: " + resourceUrl);
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent root = loader.load();

            Object childController = loader.getController();
            connectChildController(childController);

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initOwner(getCurrentStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError(
                    "Không thể mở giao diện",
                    "Không tìm thấy hoặc không LOAD được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
            );
        }
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object nextController = loader.getController();
            connectChildController(nextController);

            Stage stage = getCurrentStage();

            if (stage == null) {
                stage = new Stage();
            }

            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            showError(
                    "Không thể chuyển giao diện",
                    "Không tìm thấy hoặc không LOAD được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
            );
        }
    }

    private Stage getCurrentStage() {
        if (TrangchuPane != null && TrangchuPane.getScene() != null) {
            return (Stage) TrangchuPane.getScene().getWindow();
        }

        if (KhoPane != null && KhoPane.getScene() != null) {
            return (Stage) KhoPane.getScene().getWindow();
        }

        if (NaptienPane != null && NaptienPane.getScene() != null) {
            return (Stage) NaptienPane.getScene().getWindow();
        }

        if (CaidatPane != null && CaidatPane.getScene() != null) {
            return (Stage) CaidatPane.getScene().getWindow();
        }

        return null;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .isPresent();
    }

    private void connectChildController(Object controller) {
        if (controller instanceof LinkedController linkedController) {
            linkedController.setUserController(this);
        }
    }

    /** Interface cho các controller con (modal) cần liên kết với UserController. */
    public interface LinkedController {
        void setUserController(UserController userController);
    }
}
