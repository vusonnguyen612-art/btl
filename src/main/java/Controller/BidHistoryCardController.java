package Controller;

import Controller.utils.FormatUtils;
import Model.Bid;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** Controller cho card lịch sử đấu giá (hiển thị thời gian, số tiền, phiên). */
public class BidHistoryCardController {

    @FXML private HBox cardRoot;
    @FXML private Label timeLabel;
    @FXML private Label amountLabel;
    @FXML private Label auctionLabel;

    /** Gán dữ liệu bid và hiển thị lên card. */
    public void setBid(Bid bid) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String timeStr = bid.getTimestamp().format(formatter);
        timeLabel.setText(timeStr);

        BigDecimal amount = new BigDecimal(String.valueOf(bid.getAmount()));
        amountLabel.setText(FormatUtils.formatMoney(amount) + " $");

        auctionLabel.setText("Phiên: " + bid.getAuctionId());
    }

    /** @return root HBox của card */
    public HBox getRoot() {
        return cardRoot;
    }
}
