package Model;

import java.math.BigDecimal;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private BigDecimal balance;

    public Bidder(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    @Override
    public boolean isBidder() {
        return true;
    }

    @Override
    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
