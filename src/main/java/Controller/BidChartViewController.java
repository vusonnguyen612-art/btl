package Controller;

import Model.AuctionSession;
import Model.Bid;
import Network.Message;
import Network.NetworkService;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Controller cho cửa sổ xem biểu đồ giá đấu giá chi tiết (FXML: bid_chart_view.fxml).
 * Hiển thị thông tin phiên (tên, trạng thái, giá cuối, thời gian kết thúc),
 * biểu đồ đường (LineChart) biểu diễn sự thay đổi giá theo thời gian,
 * và danh sách chi tiết các lượt đặt giá.
 */
public class BidChartViewController {

    @FXML private Label auctionNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label priceLabel;
    @FXML private Label endTimeLabel;
    @FXML private VBox chartContainer;
    @FXML private VBox bidList;

    private NetworkService networkService = NetworkService.getInstance();
    private AuctionSession auction;

    private static final DecimalFormat moneyFormat;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        moneyFormat = new DecimalFormat("#,###", symbols);
    }

    /**
     * Gán phiên đấu giá và tải dữ liệu biểu đồ + lịch sử đặt giá.
     *
     * @param auction Phiên đấu giá cần hiển thị.
     */
    public void setAuction(AuctionSession auction) {
        this.auction = auction;
        loadData();
    }

    /**
     * Tải dữ liệu phiên và lịch sử đặt giá từ server, sau đó vẽ biểu đồ và danh sách bid.
     */
    private void loadData() {
        if (auction == null) return;

        if (auction.getItem() != null) {
            auctionNameLabel.setText(auction.getItem().getName());
        }
        statusLabel.setText("Trạng thái: " + translateStatus(auction.getStatus().name()));
        priceLabel.setText("Giá cuối: " + formatMoney(auction.getCurrentPrice()) + " $");
        if (auction.getEndTime() != null) {
            endTimeLabel.setText("Kết thúc: " + auction.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        }

        Message response = networkService.getBidHistory(auction.getId());
        List<Bid> bids = (response.getType() == Message.Type.SUCCESS && response.getData() instanceof List)
                ? (List<Bid>) response.getData() : List.of();

        buildChart(bids);
        buildBidList(bids);
    }

    /**
     * Xây dựng LineChart biểu diễn giá đấu theo thời gian (tính bằng giây từ lúc bắt đầu).
     *
     * @param bids Danh sách các lượt đặt giá để vẽ lên biểu đồ.
     */
    private void buildChart(List<Bid> bids) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Thời gian (giây)");
        xAxis.setAnimated(false);
        xAxis.setForceZeroInRange(false);
        xAxis.setTickLabelFill(Color.valueOf("#eacd8f"));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Giá ($)");
        yAxis.setAnimated(false);
        yAxis.setForceZeroInRange(false);
        yAxis.setTickLabelFill(Color.valueOf("#eacd8f"));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Giá đấu");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(250);
        chart.setTitle("Biểu đồ giá đấu");
        chart.setStyle("-fx-background-color: #1E1E1D;");

        chart.getData().add(series);

        chartContainer.getChildren().add(chart);

        javafx.application.Platform.runLater(() -> {
            chart.applyCss();
            Node node;
            node = chart.lookup(".chart-title");
            if (node != null) node.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 14px;");
            node = chart.lookup(".chart-plot-background");
            if (node != null) node.setStyle("-fx-background-color: #1E1E1D;");
            node = chart.lookup(".chart-series-line");
            if (node != null) node.setStyle("-fx-stroke: #4CAF50; -fx-stroke-width: 2px;");
            node = chart.lookup(".chart-alternative-column-fill");
            if (node != null) node.setStyle("-fx-fill: transparent;");
            node = chart.lookup(".chart-alternative-row-fill");
            if (node != null) node.setStyle("-fx-fill: transparent;");
            node = chart.lookup(".chart-vertical-zero-line");
            if (node != null) node.setStyle("-fx-stroke: #333333;");
            node = chart.lookup(".chart-horizontal-zero-line");
            if (node != null) node.setStyle("-fx-stroke: #333333;");
        });

        if (bids.isEmpty()) return;

        LocalDateTime baseTime = auction.getStartTime();
        if (baseTime == null) {
            baseTime = bids.get(0).getTimestamp();
        }
        if (baseTime == null) return;

        for (Bid bid : bids) {
            long seconds = ChronoUnit.SECONDS.between(baseTime, bid.getTimestamp());
            series.getData().add(new XYChart.Data<>(seconds, bid.getAmount()));
        }
    }

    /**
     * Xây dựng danh sách các lượt đặt giá dạng Label trong VBox bidList,
     * hiển thị thời gian, số tiền, và tên người đặt.
     *
     * @param bids Danh sách các lượt đặt giá.
     */
    private void buildBidList(List<Bid> bids) {
        bidList.getChildren().clear();

        if (bids.isEmpty()) {
            Label emptyLabel = new Label("Không có lượt đặt giá nào.");
            emptyLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
            bidList.getChildren().add(emptyLabel);
            return;
        }

        for (Bid bid : bids) {
            String timeStr = bid.getTimestamp() != null
                    ? bid.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    : "--:--";
            String bidderStr = bid.getBidderUsername() != null ? bid.getBidderUsername() : bid.getBidderId();
            String text = timeStr + "  |  " + formatMoney(bid.getAmount()) + " $  |  " + bidderStr;

            Label bidLabel = new Label(text);
            bidLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 12px; -fx-background-color: #111111; -fx-padding: 8 12; -fx-background-radius: 5;");
            bidLabel.setPrefWidth(Double.MAX_VALUE);
            bidList.getChildren().add(bidLabel);
        }
    }

    /**
     * Chuyển đổi mã trạng thái tiếng Anh sang tiếng Việt để hiển thị.
     *
     * @param status Mã trạng thái từ server (ví dụ: "FINISHED", "RUNNING").
     * @return Chuỗi trạng thái tiếng Việt tương ứng.
     */
    private String translateStatus(String status) {
        return switch (status) {
            case "FINISHED" -> "Đã kết thúc";
            case "PAID" -> "Đã thanh toán";
            case "CANCELED" -> "Đã hủy";
            case "RUNNING" -> "Đang diễn ra";
            case "PAYMENT_PENDING" -> "Chờ thanh toán";
            case "OPEN" -> "Sắp diễn ra";
            default -> status;
        };
    }

    /**
     * Định dạng số tiền double thành chuỗi có dấu phẩy phân cách hàng nghìn.
     *
     * @param value Số tiền cần định dạng.
     * @return Chuỗi đã định dạng (vd: "1,234,567").
     */
    private String formatMoney(double value) {
        return moneyFormat.format(new BigDecimal(String.valueOf(value)));
    }

    @FXML
    private void close() {
        ((Stage) auctionNameLabel.getScene().getWindow()).close();
    }
}
