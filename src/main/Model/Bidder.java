package Model;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private double balance;

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

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void addBalance(double amount) {
        this.balance += amount;
    }
}
