package Controller;

import Controller.utils.CategoryMapper;
import Controller.utils.FormatUtils;
import Controller.utils.UIUtils;
import Model.AuctionSession;
import Model.User;
import Network.Message;
import Network.NetworkService;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Controller xử lý màn hình Thống kê và Báo cáo doanh thu (Analytics Dashboard).
 * Thực hiện gom nhóm dữ liệu các phiên đấu giá đã thành công để biểu diễn lên BarChart và LineChart.
 */
public class ThongkePaneController implements UserController.LinkedController {

    @FXML private ScrollPane thongkeScrollPane;
    @FXML private Label totalRevenueLabel;
    @FXML private Label successCountLabel;
    @FXML private Label failureCountLabel;
    @FXML private BarChart<String, Number> categoryBarChart;
    @FXML private CategoryAxis categoryXAxis;
    @FXML private NumberAxis categoryYAxis;
    @FXML private LineChart<String, Number> dailyLineChart;
    @FXML private CategoryAxis dailyXAxis;
    @FXML private NumberAxis dailyYAxis;

    private UserController userController;
    private final NetworkService networkService = NetworkService.getInstance();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Gán UserController để liên kết chuyển màn hình hoặc gọi các thông báo.
     *
     * @param userController Đối tượng UserController cha điều phối.
     */
    @Override
    public void setUserController(UserController userController) {
        this.userController = userController;
    }

    /**
     * Hàm initialize của JavaFX để cài đặt sự kiện chuột cho ScrollPane và tắt animation của biểu đồ nhằm tối ưu hóa hiệu suất hiển thị.
     */
    @FXML
    private void initialize() {
        UIUtils.setupScrollFocus(thongkeScrollPane);
        UIUtils.fixScrollPaneViewport(thongkeScrollPane);
        
        if (categoryBarChart != null) {
            categoryBarChart.setAnimated(false);
        }
        if (dailyLineChart != null) {
            dailyLineChart.setAnimated(false);
        }
    }

    /**
     * Tải danh sách tất cả các phiên đấu giá từ server, lọc theo thông tin người bán hiện tại,
     * thực hiện tính toán và vẽ biểu đồ báo cáo doanh thu.
     */
    public void loadStatistics() {
        if (totalRevenueLabel == null) return;

        try {
            Message response = networkService.getAuctions();
            if (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<AuctionSession> allAuctions = (List<AuctionSession>) response.getData();
                User currentUser = networkService.getCurrentUser();
                
                if (currentUser == null) return;

                // Lọc các phiên đấu giá thuộc sở hữu của người dùng hiện tại (hoặc tất cả nếu là admin)
                List<AuctionSession> myAuctions;
                if (currentUser.isAdmin()) {
                    myAuctions = allAuctions;
                } else {
                    myAuctions = allAuctions.stream()
                            .filter(a -> a.getSellerId() != null && a.getSellerId().equals(currentUser.getId()))
                            .collect(Collectors.toList());
                }

                calculateAndDisplay(myAuctions);
            } else {
                showEmptyData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showEmptyData();
        }
    }

    /**
     * Tính toán các thông số thống kê và cập nhật lên UI biểu đồ.
     *
     * @param auctions danh sách các phiên đấu giá cần thống kê
     */
    private void calculateAndDisplay(List<AuctionSession> auctions) {
        double totalRevenue = 0.0;
        int successCount = 0;
        int failureCount = 0;

        // Cấu trúc gom nhóm doanh thu theo danh mục sản phẩm
        Map<String, Double> categoryRevenueMap = new TreeMap<>();
        // Cấu trúc gom nhóm doanh thu theo ngày kết thúc (sử dụng TreeMap để tự động sắp xếp theo LocalDate)
        Map<LocalDate, Double> dailyRevenueMap = new TreeMap<>();

        for (AuctionSession auction : auctions) {
            AuctionSession.Status status = auction.getStatus();
            
            // Bỏ qua các phiên chưa bắt đầu hoặc đang chạy
            if (status == AuctionSession.Status.OPEN || status == AuctionSession.Status.RUNNING) {
                continue;
            }

            // Phiên thành công là phiên đã kết thúc (FINISHED, PAID, PAYMENT_PENDING) và có người thắng cuộc
            boolean hasWinner = auction.getWinnerId() != null || auction.getHighestBidderId() != null;
            boolean isSuccessful = (status == AuctionSession.Status.PAID || 
                                   status == AuctionSession.Status.FINISHED || 
                                   status == AuctionSession.Status.PAYMENT_PENDING) && hasWinner;

            if (isSuccessful) {
                successCount++;
                double price = auction.getCurrentPrice();
                totalRevenue += price;

                // Gom nhóm theo danh mục sản phẩm
                String category = "Khác";
                if (auction.getItem() != null && auction.getItem().getCategory() != null) {
                    category = translateCategory(auction.getItem().getCategory());
                }
                categoryRevenueMap.put(category, categoryRevenueMap.getOrDefault(category, 0.0) + price);

                // Gom nhóm theo ngày kết thúc
                if (auction.getEndTime() != null) {
                    LocalDate date = auction.getEndTime().toLocalDate();
                    dailyRevenueMap.put(date, dailyRevenueMap.getOrDefault(date, 0.0) + price);
                }
            } else {
                // Phiên thất bại hoặc bị hủy bỏ
                failureCount++;
            }
        }

        // Cập nhật các thẻ thông số tóm tắt
        if (userController != null) {
            totalRevenueLabel.setText(FormatUtils.formatMoney(BigDecimal.valueOf(totalRevenue)) + " $");
        } else {
            totalRevenueLabel.setText(String.format("%,.0f $", totalRevenue));
        }
        successCountLabel.setText(String.valueOf(successCount));
        failureCountLabel.setText(String.valueOf(failureCount));

        // Vẽ biểu đồ doanh thu theo danh mục
        drawCategoryChart(categoryRevenueMap);

        // Vẽ biểu đồ doanh thu theo ngày
        drawDailyChart(dailyRevenueMap);
    }

    /**
     * Dịch tên danh mục sản phẩm tiếng Anh sang tiếng Việt để hiển thị trực quan hơn.
     */
    private String translateCategory(String category) {
        if (category == null) return "Khác";
        switch (category.toLowerCase()) {
            case "vehicle": return "Phương tiện";
            case "electronics": return "Điện tử";
            case "fashion": return "Thời trang";
            case "books": return "Sách";
            case "art": return "Nghệ thuật";
            case "music": return "Âm nhạc";
            case "sports": return "Thể thao";
            case "jewelry": return "Trang sức";
            case "furniture": return "Nội thất";
            default: return category;
        }
    }

    /**
     * Vẽ dữ liệu doanh thu lên biểu đồ cột BarChart.
     */
    private void drawCategoryChart(Map<String, Double> categoryRevenueMap) {
        if (categoryBarChart == null) return;
        categoryBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu");

        for (Map.Entry<String, Double> entry : categoryRevenueMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        categoryBarChart.getData().add(series);
    }

    /**
     * Vẽ dữ liệu doanh thu lên biểu đồ đường LineChart.
     */
    private void drawDailyChart(Map<LocalDate, Double> dailyRevenueMap) {
        if (dailyLineChart == null) return;
        dailyLineChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu theo ngày");

        for (Map.Entry<LocalDate, Double> entry : dailyRevenueMap.entrySet()) {
            String formattedDate = entry.getKey().format(DATE_FORMATTER);
            series.getData().add(new XYChart.Data<>(formattedDate, entry.getValue()));
        }

        dailyLineChart.getData().add(series);
    }

    /**
     * Hiển thị trạng thái dữ liệu trống khi có lỗi xảy ra hoặc chưa có phiên đấu giá nào.
     */
    private void showEmptyData() {
        totalRevenueLabel.setText("0 $");
        successCountLabel.setText("0");
        failureCountLabel.setText("0");
        if (categoryBarChart != null) categoryBarChart.getData().clear();
        if (dailyLineChart != null) dailyLineChart.getData().clear();
    }
}
