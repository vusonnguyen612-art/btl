package Model;

import java.io.Serializable;
import java.util.List;

/**
 * Lớp chứa các tiêu chí tìm kiếm vật phẩm đấu giá.
 * Dùng để lọc danh sách vật phẩm theo keyword, category, statuses, khoảng giá, sellerId và sortBy.
 *
 * Các trường: keyword, category, statuses, minPrice, maxPrice, sellerId, sortBy.
 */
public class SearchCriteria implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;
    private String category;
    private List<AuctionSession.Status> statuses;
    private Double minPrice;
    private Double maxPrice;
    private String sellerId;
    private String sortBy;

    /** Khởi tạo SearchCriteria rỗng, tất cả các trường đều null. */
    public SearchCriteria() {}

    /** @return từ khóa tìm kiếm */
    public String getKeyword() { return keyword; }
    /** @param keyword từ khóa tìm kiếm mới */
    public void setKeyword(String keyword) { this.keyword = keyword; }

    /** @return danh mục vật phẩm cần lọc */
    public String getCategory() { return category; }
    /** @param category danh mục vật phẩm mới */
    public void setCategory(String category) { this.category = category; }

    /** @return danh sách trạng thái phiên cần lọc */
    public List<AuctionSession.Status> getStatuses() { return statuses; }
    /** @param statuses danh sách trạng thái phiên mới */
    public void setStatuses(List<AuctionSession.Status> statuses) { this.statuses = statuses; }

    /** @return giá tối thiểu (có thể null) */
    public Double getMinPrice() { return minPrice; }
    /** @param minPrice giá tối thiểu mới */
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }

    /** @return giá tối đa (có thể null) */
    public Double getMaxPrice() { return maxPrice; }
    /** @param maxPrice giá tối đa mới */
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }

    /** @return ID người bán cần lọc */
    public String getSellerId() { return sellerId; }
    /** @param sellerId ID người bán mới */
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    /** @return trường sắp xếp (price, date...) */
    public String getSortBy() { return sortBy; }
    /** @param sortBy trường sắp xếp mới */
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
}
