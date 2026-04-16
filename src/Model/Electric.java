package Model;

public class Electric extends Item {
    protected double fee;
    protected String time_to_fee;
    public Electric(String name, String description, int startPrice, String sellerID, double fee, String  time_to_fee) {
        super(name, description, startPrice, sellerID);
        this.fee = fee;
        this.time_to_fee = time_to_fee;
    }
    public double getFee() {
        return fee;
    }
    public void setFee(double fee) {}
    public String getTime_to_fee() {
        return time_to_fee;
    }
    public void setTime_to_fee() {}


}
