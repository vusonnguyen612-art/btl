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
            
            if (currentUser != null) {
                userDAO.updateBalance(currentUser.getUsername(), soDuTaiKhoan);
            }
            
            syncBalanceLabels();

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

        boolean success = userDAO.changePassword(currentUser.getUsername(), matKhauHienTai, matKhauMoi);

        if (success) {
            Matkhauhientai.clear();
            Matkhaumoi.clear();
            Nhaplaimatkhaumoi.clear();
            showInfo("Thanh cong", "Doi mat khau thanh cong!");
        } else {
            showWarning("Loi", "Mat khau hien tai khong dung.");
        }
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

            refreshAllData();
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
        } catch (Exception e) {
            Label errorLabel = createEmptyLabel("Loi tai san pham.");
            AllItems.getChildren().add(errorLabel);
        }
    }

    private HBox createItemCard(Item item) {
        HBox hbox = new HBox(10);
        hbox.setStyle("-fx-background-color: #111111; -fx-border-color: #d4af5a; -fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5;");
        hbox.setSpacing(15);
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox infoBox = new VBox(3);
        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 16px; -fx-font-weight: bold;");

        BigDecimal price = new BigDecimal(String.valueOf(item.getStartPrice()));
        Label priceLabel = new Label(formatMoney(price) + " $");
        priceLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(nameLabel, priceLabel);

        VBox actionBox = new VBox(3);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        List<AuctionSession> auctions = auctionDAO.findAllAuctions();
        AuctionSession itemAuction = null;
        for (AuctionSession auction : auctions) {
            if (auction.getItem() != null && auction.getItem().getId().equals(item.getId())) {
                itemAuction = auction;
                break;
            }
        }

        if (itemAuction != null) {
            String statusColor = itemAuction.isRunning() ? "#4CAF50" : "#FF9800";
            String statusText = itemAuction.isRunning() ? "DANG DIEN RA" : itemAuction.isOpen() ? "CHUA BAT DAU" : "DA KET THUC";
            Label statusLabel = new Label(statusText);
            statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");

            BigDecimal currentPrice = new BigDecimal(String.valueOf(itemAuction.getCurrentPrice()));
            Label currentPriceLabel = new Label("Gia hien tai: " + formatMoney(currentPrice) + " $");
            currentPriceLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 13px;");

            String timeText = itemAuction.isRunning() && itemAuction.getEndTime() != null
                    ? "Con: " + getRemainingTime(itemAuction)
                    : "Thoi gian: " + formatDuration(itemAuction.getDurationMinutes());
            Label timeLabel = new Label(timeText);
            timeLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 12px;");

            actionBox.getChildren().addAll(statusLabel, currentPriceLabel, timeLabel);
        } else {
            Label noAuctionLabel = new Label("Chua co phien dau gia");
            noAuctionLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
            actionBox.getChildren().add(noAuctionLabel);
        }

        hbox.getChildren().addAll(infoBox, actionBox);
        return hbox;
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
            List<Item> userItems = itemDAO.findBySellerId(currentUser.getId());
            List<AuctionSession> userAuctions = auctionDAO.findAllAuctions();

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
        HBox hbox = new HBox(15);
        hbox.setStyle("-fx-background-color: #111111; -fx-border-color: #d4af5a; -fx-padding: 12; -fx-background-radius: 5; -fx-border-radius: 5;");
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String itemName = auction.getItem() != null ? auction.getItem().getName() : "Unknown";
        Label nameLabel = new Label(itemName);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        nameLabel.setPrefWidth(150);

        String statusColor = auction.isRunning() ? "#4CAF50" : auction.isOpen() ? "#FF9800" : "#ff6b6b";
        String statusText = auction.isRunning() ? "DANG CHAY" : auction.isOpen() ? "CHUA BAT DAU" : "DA KET THUC";
        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        statusLabel.setPrefWidth(100);

        BigDecimal currentPrice = new BigDecimal(String.valueOf(auction.getCurrentPrice()));
        Label priceLabel = new Label(formatMoney(currentPrice) + " $");
        priceLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 14px;");
        priceLabel.setPrefWidth(90);

        String timeText = auction.isRunning() && auction.getEndTime() != null
                ? "Con: " + getRemainingTime(auction)
                : "Thoi gian: " + formatDuration(auction.getDurationMinutes());
        Label timeLabel = new Label(timeText);
        timeLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 12px;");
        timeLabel.setPrefWidth(100);

        VBox actionBox = new VBox(5);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        if (auction.isOpen()) {
            Button startBtn = new Button("Bat dau");
            startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
            startBtn.setPrefSize(80, 30);
            startBtn.setOnAction(e -> {
                boolean success = auctionDAO.startAuction(auction.getId());
                if (success) {
                    showInfo("Thanh cong", "Da bat dau phien dau gia.");
                    loadWarehouseItems();
                    loadHomeItems();
                } else {
                    showError("Loi", "Khong the bat dau phien dau gia.");
                }
            });
            actionBox.getChildren().add(startBtn);
        }

        hbox.getChildren().addAll(nameLabel, statusLabel, priceLabel, timeLabel, actionBox);
        return hbox;
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

        try {
            List<Bid> bids = auctionDAO.getUserBidHistory(currentUser.getId());

            if (bids.isEmpty()) {
                Label label = createEmptyLabel("Chua co lich su dau gia.");
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
            Label label = createEmptyLabel("Loi tai lich su: " + e.getMessage());
            label.setLayoutX(150);
            label.setLayoutY(250);
            LichsudaugiaPane.getChildren().add(label);
        }
    }

    private HBox createBidHistoryCard(Bid bid) {
        HBox hbox = new HBox(15);
        hbox.setStyle("-fx-background-color: #111111; -fx-border-color: #d4af5a; -fx-padding: 12; -fx-background-radius: 5; -fx-border-radius: 5;");
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String timeStr = bid.getTimestamp().format(formatter);

        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
        timeLabel.setPrefWidth(140);

        BigDecimal amount = new BigDecimal(String.valueOf(bid.getAmount()));
        Label amountLabel = new Label(formatMoney(amount) + " $");
        amountLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label auctionLabel = new Label("Phien: " + bid.getAuctionId());
        auctionLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 13px;");

        hbox.getChildren().addAll(timeLabel, amountLabel, auctionLabel);
        return hbox;
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

    private String getRemainingTime(AuctionSession auction) {
        if (auction.getEndTime() == null) {
            return formatDuration(auction.getDurationMinutes());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return "Da ket thuc";
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
                    "Khong the load giao dien",
                    "Khong load duoc file: " + fxmlPath + "\n\nChi tiet: " + e.getMessage()
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
