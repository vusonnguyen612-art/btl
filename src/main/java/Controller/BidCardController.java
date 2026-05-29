package Controller;

import Controller.utils.FormatUtils;
import Model.Bid;
import DAO.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;

/** Controller cho card hiển thị một lượt đặt giá (bidder, amount, time) trong phòng đấu giá. */
public class BidCardController {

    @FXML private HBox cardRoot;
    @FXML private Label bidderLabel;
    @FXML private Label amountLabel;
    @FXML private Label timeLabel;

    private UserDAO userDAO = new UserDAO();

    /** Gán dữ liệu bid: hiển thị tên bidder, số tiền, thời gian. */
    public void setBid(Bid bid) {
        String username = userDAO.getUsernameById(bid.getBidderId());
        bidderLabel.setText(username);

        amountLabel.setText(FormatUtils.formatMoney(bid.getAmount()) + " $");

        String timeStr = bid.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        timeLabel.setText(timeStr);
    }

    /** @return root HBox của card */
    public HBox getRoot() {
        return cardRoot;
    }
}
