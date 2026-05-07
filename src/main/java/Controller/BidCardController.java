package Controller;

import Model.Bid;
import DAO.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class BidCardController {

    @FXML private HBox cardRoot;
    @FXML private Label bidderLabel;
    @FXML private Label amountLabel;
    @FXML private Label timeLabel;

    private static final DecimalFormat moneyFormat;
    private UserDAO userDAO = new UserDAO();

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        moneyFormat = new DecimalFormat("#,###", symbols);
    }

    public void setBid(Bid bid) {
        String username = userDAO.getUsernameById(bid.getBidderId());
        bidderLabel.setText(username);

        amountLabel.setText(moneyFormat.format(bid.getAmount()) + " $");

        String timeStr = bid.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        timeLabel.setText(timeStr);
    }

    public HBox getRoot() {
        return cardRoot;
    }
}
