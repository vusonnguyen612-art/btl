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
import java.util.List;
import java.util.Locale;

import Model.Item;
import Model.User;
import Model.AuctionSession;
import Model.Bid;
import Network.NetworkService;
import Network.Message;

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

    private String formatMoney(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');

        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(value);
    }

    private BigDecimal parseMoney(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui long nhap so tien can nap.");
        }

        String normalized = rawText
                .trim()
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "")
                .replace("$", "")
                .replace("\u20ab", "");

        if (!normalized.matches("\\d+")) {
            throw new IllegalArgumentException("So tien khong hop le.");
        }

        BigDecimal amount = new BigDecimal(normalized);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("So tien phai lon hon 0.");
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
                showWarning("Thieu thong tin", "Vui long nhap ten ngan hang.");
                return;
            }

            if (soTaiKhoan.isEmpty()) {
                showWarning("Thieu thong tin", "Vui long nhap so tai khoan.");
                return;
            }

            BigDecimal soTienNap = parseMoney(soTienRaw);

            soDuTaiKhoan = soDuTaiKhoan.add(soTienNap);

            if (Sodutaikhoan1 != null) {
                Sodutaikhoan1.setText(formatMoney(soDuTaiKhoan));
            }

            if (Sodutaikhoan != null) {
                Sodutaikhoan.setText(formatMoney(soDuTaiKhoan));
            }

            Nganhangnaptien.clear();
            Sotaikhoannaptien.clear();
            Sotiencannap.clear();

            showInfo("Nap tien thanh cong", "Ban da nap them " + formatMoney(soTienNap) + " $ vao tai khoan.");

        } catch (IllegalArgumentException e) {
            showWarning("Du lieu khong hop le", e.getMessage());
        } catch (Exception e) {
            showError("Loi", "Khong the nap tien. Chi tiet: " + e.getMessage());
        }
    }

    @FXML
    private void Doimatkhau(ActionEvent event) {
        if (currentUser == null) {
            showWarning("Loi", "Vui long dang nhap.");
            return;
        }

        String matKhauHienTai = Matkhauhientai.getText();
        String matKhauMoi = Matkhaumoi.getText();
        String nhapLaiMatKhauMoi = Nhaplaimatkhaumoi.getText();

        if (matKhauHienTai.isBlank() || matKhauMoi.isBlank() || nhapLaiMatKhauMoi.isBlank()) {
            showWarning("Du lieu thieu", "Vui long nhap day du thong tin.");
            return;
        }

        if (!matKhauMoi.equals(nhapLaiMatKhauMoi)) {
            showWarning("Loi", "Mat khau moi khong khop.");
            return;
        }

        if (matKhauMoi.length() < 6) {
            showWarning("Loi", "Mat khau moi phai co it nhat 6 ky tu.");
            return;
        }

        showWarning("Chua ho tro", "Tinh nang doi mat khau qua Server chua duoc cai dat.");

        Matkhauhientai.clear();
        Matkhaumoi.clear();
        Nhaplaimatkhaumoi.clear();
    }

    @FXML
    private void CreateItems(ActionEvent event) {
        openModalFXML(CREATE_ITEM_FXML, "Tao san pham");
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
            stage.setTitle("Phong Dau Gia");
            stage.setScene(new Scene(root, 900, 600));
            stage.initOwner(getCurrentStage());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setOnHidden(e -> auctionRoomController.stopRefresh());
            stage.show();

        } catch (Exception e) {
            showError("Loi", "Khong the mo phong dau gia: " + e.getMessage());
        }
    }

    @FXML
    private void Doitaikhoan(ActionEvent event) {
        boolean confirmed = showConfirm(
                "Doi tai khoan",
                "Ban co chac muon dang xuat va doi tai khoan khong?"
        );

        if (!confirmed) {
            return;
        }

        switchScene(LOGIN_FXML, "Dang nhap");
    }

    @FXML
    private void Exit(ActionEvent event) {
        boolean confirmed = showConfirm(
                "Thoat",
                "Ban co chac muon thoat chuong trinh khong?"
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
            if (Name != null) {
                Name.setText(user.getUsername());
            }
        }

        if (balance != null) {
            this.soDuTaiKhoan = balance;
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
            Message response = networkService.getItems();
            if (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List) {
                List<Item> allItems = (List<Item>) response.getData();
                if (allItems.isEmpty()) {
                    Label emptyLabel = createEmptyLabel("Chua co san pham dau gia nao.");
                    AllItems.getChildren().add(emptyLabel);
                } else {
                    Label headerLabel = new Label("San pham dang dau gia");
                    headerLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 20px; -fx-font-weight: bold;");
                    headerLabel.setPadding(new Insets(0, 0, 10, 0));
                    AllItems.getChildren().add(headerLabel);

                    for (Item item : allItems) {
                        HBox itemBox = createItemCard(item);
                        AllItems.getChildren().add(itemBox);
                    }
                }
            }
        } catch (Exception e) {
            Label errorLabel = createEmptyLabel("Loi tai san pham.");
            AllItems.getChildren().add(errorLabel);
        }
    }

    private HBox createItemCard(Item item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/item_card.fxml"));
            HBox card = loader.load();
            ItemCardController controller = loader.getController();

            Message response = networkService.getAuctions();
            List<AuctionSession> auctions = (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List)
                    ? (List<AuctionSession>) response.getData() : List.of();
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
            Label emptyLabel = createEmptyLabel("Vui long dang nhap.");
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
                Label emptyLabel = createEmptyLabel("Kho cua ban hien chua co san pham.");
                Items.getChildren().add(emptyLabel);
            } else {
                Label headerLabel = new Label("Phien dau gia cua ban");
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
                    Label noAuctionLabel = new Label("Chua tao phien dau gia. Nhan 'Tao san pham' de bat dau.");
                    noAuctionLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");
                    noAuctionLabel.setPadding(new Insets(10, 0, 10, 0));
                    Items.getChildren().add(noAuctionLabel);
                }
            }
        } catch (Exception e) {
            Label errorLabel = createEmptyLabel("Loi tai kho: " + e.getMessage());
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
                    showInfo("Thanh cong", "Da bat dau phien dau gia.");
                    loadWarehouseItems();
                    loadHomeItems();
                } else {
                    showError("Loi", "Khong the bat dau phien dau gia.");
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
            Label label = createEmptyLabel("Vui long dang nhap de xem lich su.");
            label.setLayoutX(150);
            label.setLayoutY(250);
            LichsudaugiaPane.getChildren().add(label);
            return;
        }

        Label label = createEmptyLabel("Chua co lich su dau gia.");
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
        return minutes + " phut";
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
                    "Khong the mo giao dien",
                    "Khong tim thay hoac khong load duoc file: " + fxmlPath + "\n\nChi tiet: " + e.getMessage()
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
                    "Khong the chuyen giao dien",
                    "Khong tim thay hoac khong load duoc file: " + fxmlPath + "\n\nChi tiet: " + e.getMessage()
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

    public interface LinkedController {
        void setUserController(UserController userController);
    }
}
