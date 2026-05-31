package Controller.utils;

import Model.AuctionSession;
import Model.SearchCriteria;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Builder thống nhất để xây dựng SearchCriteria từ các field UI.
 * Tránh duplicate code giữa TrangchuPaneController và AuctionRoomController.
 */
public final class SearchCriteriaBuilder {

    private SearchCriteriaBuilder() {}

    /**
     * Xây dựng SearchCriteria từ các控件 tìm kiếm.
     *
     * @param keywordField  TextField chứa từ khóa tìm kiếm.
     * @param categoryCombo ComboBox chứa danh mục (tiếng Việt).
     * @param statusCombo   ComboBox chứa trạng thái (tiếng Việt).
     * @param minPriceField TextField chứa giá tối thiểu.
     * @param maxPriceField TextField chứa giá tối đa.
     * @param sortCombo     ComboBox chứa kiểu sắp xếp.
     * @return SearchCriteria đã được thiết lập, hoặc {@code null} nếu không có tiêu chí nào.
     */
    public static SearchCriteria build(
            TextField keywordField,
            ComboBox<String> categoryCombo,
            ComboBox<String> statusCombo,
            TextField minPriceField,
            TextField maxPriceField,
            ComboBox<String> sortCombo) {

        SearchCriteria criteria = new SearchCriteria();

        if (keywordField != null) {
            String keyword = keywordField.getText().trim();
            if (!keyword.isEmpty()) {
                criteria.setKeyword(keyword);
            }
        }

        if (categoryCombo != null) {
            String category = categoryCombo.getValue();
            if (category != null && !category.equals("Tất cả")) {
                criteria.setCategory(CategoryMapper.toEnglish(category));
            }
        }

        if (statusCombo != null) {
            String status = statusCombo.getValue();
            if (status != null && !status.equals("Tất cả")) {
                List<AuctionSession.Status> statuses = CategoryMapper.mapStatus(status);
                if (!statuses.isEmpty()) {
                    criteria.setStatuses(statuses);
                }
            }
        }

        if (minPriceField != null) {
            String minPriceText = minPriceField.getText().trim();
            if (!minPriceText.isEmpty()) {
                try { criteria.setMinPrice(Double.parseDouble(minPriceText)); } catch (NumberFormatException ignored) {}
            }
        }

        if (maxPriceField != null) {
            String maxPriceText = maxPriceField.getText().trim();
            if (!maxPriceText.isEmpty()) {
                try { criteria.setMaxPrice(Double.parseDouble(maxPriceText)); } catch (NumberFormatException ignored) {}
            }
        }

        if (sortCombo != null) {
            String sort = sortCombo.getValue();
            if (sort != null) {
                criteria.setSortBy(mapSortKey(sort));
            }
        }

        return criteria;
    }

    private static String mapSortKey(String vietnameseSort) {
        return switch (vietnameseSort) {
            case "Cũ nhất" -> "oldest";
            case "Giá tăng dần" -> "price_asc";
            case "Giá giảm dần" -> "price_desc";
            case "Tên A-Z" -> "name";
            default -> "newest";
        };
    }

    /**
     * Reset tất cả các field tìm kiếm về giá trị mặc định.
     */
    public static void reset(
            TextField keywordField,
            ComboBox<String> categoryCombo,
            ComboBox<String> statusCombo,
            TextField minPriceField,
            TextField maxPriceField,
            ComboBox<String> sortCombo) {

        if (keywordField != null) keywordField.clear();
        if (categoryCombo != null) categoryCombo.getSelectionModel().select("Tất cả");
        if (statusCombo != null) statusCombo.getSelectionModel().select("Tất cả");
        if (minPriceField != null) minPriceField.clear();
        if (maxPriceField != null) maxPriceField.clear();
        if (sortCombo != null) sortCombo.getSelectionModel().select("Mới nhất");
    }

    /**
     * Khởi tạo các ComboBox tìm kiếm với giá trị mặc định.
     */
    public static void initCombos(
            ComboBox<String> categoryCombo,
            ComboBox<String> statusCombo,
            ComboBox<String> sortCombo) {

        if (categoryCombo != null) {
            categoryCombo.getItems().add("Tất cả");
            categoryCombo.getItems().addAll(CategoryMapper.getAllVietnameseNames());
            categoryCombo.getSelectionModel().select("Tất cả");
        }
        if (statusCombo != null) {
            statusCombo.getItems().add("Tất cả");
            statusCombo.getItems().addAll("Đang diễn ra", "Sắp diễn ra", "Chờ thanh toán", "Đã kết thúc", "Đã hủy");
            statusCombo.getSelectionModel().select("Tất cả");
        }
        if (sortCombo != null) {
            sortCombo.getItems().addAll("Mới nhất", "Cũ nhất", "Giá tăng dần", "Giá giảm dần", "Tên A-Z");
            sortCombo.getSelectionModel().select("Mới nhất");
        }
    }
}
