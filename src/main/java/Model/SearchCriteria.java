package Model;

import java.io.Serializable;
import java.util.List;

public class SearchCriteria implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;
    private String category;
    private List<AuctionSession.Status> statuses;
    private Double minPrice;
    private Double maxPrice;
    private String sellerId;
    private String sortBy;

    public SearchCriteria() {}

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<AuctionSession.Status> getStatuses() { return statuses; }
    public void setStatuses(List<AuctionSession.Status> statuses) { this.statuses = statuses; }

    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }

    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
}
