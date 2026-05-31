package Controller;

import Controller.utils.FormatUtils;
import Controller.utils.ResponseUtils;
import Model.AuctionSession;
import Model.Bid;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BidChartViewController {

    @FXML private Label auctionNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label priceLabel;
    @FXML private Label endTimeLabel;
    @FXML private VBox chartContainer;
    @FXML private VBox bidList;

    private NetworkService networkService = NetworkService.getInstance();
    private AuctionSession auction;

    /** Gán phiên đấu giá cần hiển thị biểu đồ và tải dữ liệu liên quan. */
    public void setAuction(AuctionSession auction) {
        this.auction = auction;
        loadData();
    }

    /** Tải dữ liệu phiên đấu giá từ server: thông tin phiên, danh sách đặt giá, rồi xây dựng biểu đồ và danh sách bid. */
    private void loadData() {
        if (auction == null) return;

        if (auction.getItem() != null) {
            auctionNameLabel.setText(auction.getItem().getName());
        }
        statusLabel.setText("Trạng thái: " + translateStatus(auction.getStatus().name()));
        priceLabel.setText("Giá cuối: " + FormatUtils.formatMoney(auction.getCurrentPrice()) + " $");
        if (auction.getEndTime() != null) {
            endTimeLabel.setText("Kết thúc: " + auction.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        }

        List<Bid> bids = ResponseUtils.extractList(networkService.getBidHistory(auction.getId()));

        buildChart(bids);
        buildBidList(bids);
    }

    /** Xây dựng biểu đồ đường thể hiện lịch sử giá đặt theo thời gian. */
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

    /** Xây dựng danh sách hiển thị chi tiết từng lượt đặt giá (thời gian, số tiền, người đặt). */
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
            String text = timeStr + "  |  " + FormatUtils.formatMoney(bid.getAmount()) + " $  |  " + bidderStr;

            Label bidLabel = new Label(text);
            bidLabel.setStyle("-fx-text-fill: #eacd8f; -fx-font-size: 12px; -fx-background-color: #111111; -fx-padding: 8 12; -fx-background-radius: 5;");
            bidLabel.setPrefWidth(Double.MAX_VALUE);
            bidList.getChildren().add(bidLabel);
        }
    }

    /** Dịch trạng thái phiên đấu giá từ tiếng Anh sang tiếng Việt. */
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

    /** Đóng cửa sổ popup hiển thị biểu đồ. */
    @FXML
    private void close() {
        ((Stage) auctionNameLabel.getScene().getWindow()).close();
    }
}
