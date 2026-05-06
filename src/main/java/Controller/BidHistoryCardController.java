package Controller;

import Model.Bid;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class BidHistoryCardController {

    @FXML private HBox cardRoot;
    @FXML private Label timeLabel;
    @FXML private Label amountLabel;
    @FXML private Label auctionLabel;

    private static final DecimalFormat moneyFormat;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        moneyFormat = new DecimalFormat("#,###", symbols);
    }

    public void setBid(Bid bid) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String timeStr = bid.getTimestamp().format(formatter);
        timeLabel.setText(timeStr);

        BigDecimal amount = new BigDecimal(String.valueOf(bid.getAmount()));
        amountLabel.setText(moneyFormat.format(amount) + " $");

        auctionLabel.setText("Phiên: " + bid.getAuctionId());
    }

    public HBox getRoot() {
        return cardRoot;
    }
}
