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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import Model.Item;
import Model.User;
import Model.AuctionSession;
import Model.Bid;
import DAO.UserDAO;
import DAO.ItemDAO;
import DAO.AuctionDAO;

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
    private final UserDAO userDAO = new UserDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();
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
    private void initialize() {
        setupMenuButtons();
        setupDefaultScreen();

        syncBalanceLabels();

        loadHomeItems();
        loadWarehouseItems();
        loadBidHistory();
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

    private void syncBalanceLabels() {
        String formattedBalance = formatMoney(soDuTaiKhoan);

        if (Sodutaikhoan1 != null) {
            Sodutaikhoan1.setText(formattedBalance);
        }

        if (Sodutaikhoan != null) {
            Sodutaikhoan.setText(formattedBalance);
        }
    }

    private String formatMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');

        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(value);
    }

    private BigDecimal parseMoney(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền cần nạp.");
        }

        String normalized = rawText
                .trim()
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "")
                .replace("$", "")
                .replace("\u20ab", "");

        if (!normalized.matches("\\d+")) {
            throw new IllegalArgumentException("Số tiền không hợp lệ.");
        }

        BigDecimal amount = new BigDecimal(normalized);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0.");
        }

        return amount;
    }

    @FXML
    private void Naptien(ActionEvent event) {
        try {
            String nganHang = Nganhangnaptien.getText().trim();
            String soTaiKhoan = Sotaikhoannaptien.getText().trim();
            String soTienRaw = Sotiencannap.getText().trim();

            if (nganHang.isEmpty()) {
                showWarning("Thiếu thông tin", "Vui lòng nhập tên ngân hàng.");
                return;
            }

            if (soTaiKhoan.isEmpty()) {
                showWarning("Thiếu thông tin", "Vui lòng nhập số tài khoản.");
                return;
            }

            BigDecimal soTienNap = parseMoney(soTienRaw);

            soDuTaiKhoan = soDuTaiKhoan.add(soTienNap);

            if (currentUser != null) {
                userDAO.updateBalance(currentUser.getUsername(), soDuTaiKhoan);
            }

            syncBalanceLabels();

            Nganhangnaptien.clear();
            Sotaikhoannaptien.clear();
            Sotiencannap.clear();

            showInfo("Nạp tiền thành công", "Bạn đã nạp thêm " + formatMoney(soTienNap) + " $ vào tài khoản.");

        } catch (IllegalArgumentException e) {
            showWarning("Dữ liệu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            showError("Lỗi giao dịch", "Không thể nạp tiền. Chi tiết lỗi: " + e.getMessage());
        }
    }

    @FXML
    private void Doimatkhau(ActionEvent event) {
        if (currentUser == null) {
            showWarning("Lỗi xác thực", "Vui lòng đăng nhập để thực hiện chức năng này.");
            return;
        }

        String matKhauHienTai = Matkhauhientai.getText();
        String matKhauMoi = Matkhaumoi.getText();
        String nhapLaiMatKhauMoi = Nhaplaimatkhaumoi.getText();

        if (matKhauHienTai.isBlank() || matKhauMoi.isBlank() || nhapLaiMatKhauMoi.isBlank()) {
            showWarning("Thiếu thông tin", "Vui lòng điền đầy đủ thông tin mật khẩu.");
            return;
        }

        if (!matKhauMoi.equals(nhapLaiMatKhauMoi)) {
            showWarning("Lỗi mật khẩu", "Mật khẩu mới không khớp. Vui lòng nhập lại.");
            return;
        }

        if (matKhauMoi.length() < 6) {
            showWarning("Lỗi bảo mật", "Mật khẩu mới phải có ít nhất 6 ký tự.");
            return;
        }

        boolean success = userDAO.changePassword(currentUser.getUsername(), matKhauHienTai, matKhauMoi);

        if (success) {
            Matkhauhientai.clear();
            Matkhaumoi.clear();
            Nhaplaimatkhaumoi.clear();
            showInfo("Thành công", "Đổi mật khẩu thành công!");
        } else {
            showWarning("Lỗi xác thực", "Mật khẩu hiện tại không chính xác.");
        }
    }

    @FXML
    private void CreateItems(ActionEvent event) {
        openModalFXML(CREATE_ITEM_FXML, "Tạo Sản Phẩm");

        loadHomeItems();
        loadWarehouseItems();
    }

    @FXML
    private void OpenAuctionRoom(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auctionRoom.fxml"));
            Parent root = loader.load();

            AuctionRoomController auctionRoomController = loader.getController();
            auctionRoomController.setCurrentUser(currentUser);

            Stage stage = new Stage();
            stage.setTitle("Phòng Đấu Giá");
            stage.setScene(new Scene(root, 900, 600));
            stage.initOwner(getCurrentStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setOnHidden(e -> auctionRoomController.stopRefresh());
            stage.show();

            refreshAllData();
        } catch (Exception e) {
            showError("Lỗi hệ thống", "Không thể mở phòng đấu giá: " + e.getMessage());
        }
    }

    @FXML
    private void Doitaikhoan(ActionEvent event) {
        boolean confirmed = showConfirm(
                "Đổi tài khoản",
                "Bạn có chắc chắn muốn đăng xuất và đổi tài khoản không?"
        );

        if (!confirmed) {
            return;
        }

        switchScene(LOGIN_FXML, "Đăng nhập");
    }

    @FXML
    private void Exit(ActionEvent event) {
        boolean confirmed = showConfirm(
                "Thoát chương trình",
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

    public void setUserData(User user, BigDecimal balance) {
        this.currentUser = user;

        if (user != null && user.getUsername() != null) {
            Name.setText(user.getUsername());
        }

        if (user != null && user.getBalance() != null) {
            this.soDuTaiKhoan = user.getBalance();
        } else if (balance != null) {
            this.soDuTaiKhoan = balance;
        }

        updateBalanceLabels();
        syncBalanceLabels();
        loadHomeItems();
        loadWarehouseItems();
        loadBidHistory();
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

    public void setSoDuTaiKhoan(BigDecimal soDuTaiKhoan) {
        if (soDuTaiKhoan == null) {
            return;
        }

        this.soDuTaiKhoan = soDuTaiKhoan;
        syncBalanceLabels();
    }

    public void refreshAllData() {
        syncBalanceLabels();
        loadHomeItems();
        loadWarehouseItems();
        loadBidHistory();
    }

    private void loadHomeItems() {
        if (AllItems == null) {
            return;
        }

        AllItems.getChildren().clear();

        try {
            List<Item> allItems = itemDAO.findAll();
            if (allItems.isEmpty()) {
                Label emptyLabel = createEmptyLabel("Chưa có sản phẩm đấu giá nào trên hệ thống.");
                AllItems.getChildren().add(emptyLabel);
            } else {
                Label headerLabel = new Label("Sản phẩm đang đấu giá");
                headerLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 20px; -fx-font-weight: bold;");
                headerLabel.setPadding(new Insets(0, 0, 10, 0));
                AllItems.getChildren().add(headerLabel);

                for (Item item : allItems) {
                    HBox itemBox = createItemCard(item);
                    AllItems.getChildren().add(itemBox);
                }
            }
        } catch (Exception e) {
            Label errorLabel = createEmptyLabel("Có lỗi xảy ra khi tải danh sách sản phẩm.");
            AllItems.getChildren().add(errorLabel);
        }
    }

    private HBox createItemCard(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/item_card.fxml"));
            HBox card = loader.load();
            ItemCardController controller = loader.getController();

            List<AuctionSession> auctions = auctionDAO.findAllAuctions();
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
            Label emptyLabel = createEmptyLabel("Vui lòng đăng nhập để xem kho.");
            Items.getChildren().add(emptyLabel);
            return;
        }

        try {
            List<Item> userItems = itemDAO.findBySellerId(currentUser.getId());
            List<AuctionSession> userAuctions = auctionDAO.findAllAuctions();

            if (userItems.isEmpty() && userAuctions.isEmpty()) {
                Label emptyLabel = createEmptyLabel("Kho của bạn hiện chưa có sản phẩm nào.");
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
                    Label noAuctionLabel = new Label("Chưa tạo phiên đấu giá. Nhấn 'Tạo Sản Phẩm' để bắt đầu.");
                    noAuctionLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
                    noAuctionLabel.setPadding(new Insets(10, 0, 10, 0));
                    Items.getChildren().add(noAuctionLabel);
                }
            }
        } catch (Exception e) {
            Label errorLabel = createEmptyLabel("Lỗi khi tải dữ liệu kho: " + e.getMessage());
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
                boolean success = auctionDAO.startAuction(auction.getId());
                if (success) {
                    showInfo("Thành công", "Đã bắt đầu phiên đấu giá.");
                    loadWarehouseItems();
                    loadHomeItems();
                } else {
                    showError("Lỗi hệ thống", "Không thể khởi động phiên đấu giá. Vui lòng thử lại.");
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
            Label label = createEmptyLabel("Vui lòng đăng nhập để xem lịch sử đấu giá.");
            label.setLayoutX(150);
            label.setLayoutY(250);
            LichsudaugiaPane.getChildren().add(label);
            return;
        }

        try {
            List<Bid> bids = auctionDAO.getUserBidHistory(currentUser.getId());

            if (bids.isEmpty()) {
                Label label = createEmptyLabel("Chưa có lịch sử tham gia đấu giá.");
                label.setLayoutX(220);
                label.setLayoutY(250);
                LichsudaugiaPane.getChildren().add(label);
            } else {
                VBox bidList = new VBox(10);
                bidList.setLayoutX(20);
                bidList.setLayoutY(20);
                bidList.setPrefWidth(620);

                for (Bid bid : bids) {
                    HBox bidCard = createBidHistoryCard(bid);
                    bidList.getChildren().add(bidCard);
                }

                ScrollPane scrollPane = new ScrollPane(bidList);
                scrollPane.setLayoutX(0);
                scrollPane.setLayoutY(0);
                scrollPane.setPrefWidth(660);
                scrollPane.setPrefHeight(500);
                scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
                scrollPane.setFitToWidth(true);

                LichsudaugiaPane.getChildren().add(scrollPane);
            }
        } catch (Exception e) {
            Label label = createEmptyLabel("Lỗi khi tải lịch sử: " + e.getMessage());
            label.setLayoutX(150);
            label.setLayoutY(250);
            LichsudaugiaPane.getChildren().add(label);
        }
    }

    private HBox createBidHistoryCard(Bid bid) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/bid_history_card.fxml"));
            HBox card = loader.load();
            BidHistoryCardController controller = loader.getController();
            controller.setBid(bid);
            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return new HBox();
        }
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

    private String getRemainingTime(AuctionSession auction) {
        if (auction.getEndTime() == null) {
            return formatDuration(auction.getDurationMinutes());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return "Đã kết thúc";
        }

        long minutes = java.time.Duration.between(now, auction.getEndTime()).toMinutes();
        long seconds = java.time.Duration.between(now, auction.getEndTime()).getSeconds() % 60;

        if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private void openModalFXML(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
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
            showError(
                    "Không thể mở giao diện",
                    "Không tìm thấy hoặc không tải được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
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
                    "Không tìm thấy hoặc không tải được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
            );
        }
    }

    private void loadFXMLIntoPane(String fxmlPath, AnchorPane targetPane) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object childController = loader.getController();
            connectChildController(childController);

            targetPane.getChildren().clear();
            targetPane.getChildren().add(root);

            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);

        } catch (IOException e) {
            showError(
                    "Không thể tải giao diện",
                    "Không tải được file: " + fxmlPath + "\n\nChi tiết: " + e.getMessage()
            );
        }
    }

    private void connectChildController(Object controller) {
        if (controller instanceof LinkedController linkedController) {
            linkedController.setUserController(this);
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

    public interface LinkedController {
        void setUserController(UserController userController);
    }
}