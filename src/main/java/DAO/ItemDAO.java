package DAO;

import Model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO cho bảng items: thêm, sửa, xóa, tìm kiếm vật phẩm.
 * <p>Tự động ánh xạ danh mục (category) vào các subclass tương ứng (Art, Electronics, Vehicle, ...)
 * thông qua {@link #mapResultSetToItem(ResultSet)}.</p>
 */
public class ItemDAO {
    
    /**
     * Lưu vật phẩm mới vào database.
     *
     * @param item Đối tượng {@link Item} cần lưu.
     * @return {@code true} nếu lưu thành công, ngược lại {@code false}.
     */
    public boolean save(Item item) {
        String sql = "INSERT INTO items (id, name, description, start_price, seller_id, category, image_path) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getStartPrice());
            stmt.setString(5, item.getSellerId());
            stmt.setString(6, item.getCategory());
            stmt.setString(7, item.getImagePath());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Cập nhật thông tin vật phẩm (name, description, startPrice, imagePath).
     *
     * @param item Đối tượng {@link Item} chứa dữ liệu cập nhật.
     * @return {@code true} nếu cập nhật thành công, ngược lại {@code false}.
     */
    public boolean update(Item item) {
        String sql = "UPDATE items SET name = ?, description = ?, start_price = ?, image_path = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getStartPrice());
            stmt.setString(4, item.getImagePath());
            stmt.setString(5, item.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Tìm vật phẩm theo ID.
     *
     * @param id ID của vật phẩm cần tìm.
     * @return {@link Optional} chứa {@link Item} nếu tìm thấy, ngược lại {@link Optional#empty()}.
     */
    public Optional<Item> findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    /**
     * Tìm tất cả vật phẩm của một người bán.
     *
     * @param sellerId ID của người bán.
     * @return Danh sách các vật phẩm thuộc về người bán đó.
     */
    public List<Item> findBySellerId(String sellerId) {
        String sql = "SELECT * FROM items WHERE seller_id = ?";
        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
    
    /**
     * Lấy tất cả vật phẩm trong hệ thống.
     *
     * @return Danh sách toàn bộ vật phẩm.
     */
    public List<Item> findAll() {
        String sql = "SELECT * FROM items";
        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
    
    /**
     * Xóa vật phẩm theo ID.
     *
     * @param id ID của vật phẩm cần xóa.
     * @return {@code true} nếu xóa thành công, ngược lại {@code false}.
     */
    public boolean delete(String id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Ánh xạ một dòng dữ liệu từ ResultSet thành đối tượng {@link Item} phù hợp với danh mục.
     * Hỗ trợ 9 danh mục: ART, ELECTRONICS, VEHICLE, FASHION, BOOKS, SPORTS, JEWELRY, MUSIC, FURNITURE.
     *
     * @param rs ResultSet đang trỏ tới dòng dữ liệu.
     * @return Đối tượng {@link Item} thuộc subclass tương ứng.
     * @throws SQLException Nếu có lỗi đọc dữ liệu.
     */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        String category = rs.getString("category");
        String id = rs.getString("id");
        String name = rs.getString("name");
        String desc = rs.getString("description");
        double price = rs.getDouble("start_price");
        String sellerId = rs.getString("seller_id");
        String imagePath = rs.getString("image_path");
        
        Item item;
        switch (category) {
            case "ART":
                item = new Art(id, name, desc, price, sellerId);
                break;
            case "ELECTRONICS":
                item = new Electronics(id, name, desc, price, sellerId);
                break;
            case "VEHICLE":
                item = new Vehicle(id, name, desc, price, sellerId);
                break;
            case "FASHION":
                item = new Fashion(id, name, desc, price, sellerId);
                break;
            case "BOOKS":
                item = new Books(id, name, desc, price, sellerId);
                break;
            case "SPORTS":
                item = new Sports(id, name, desc, price, sellerId);
                break;
            case "JEWELRY":
                item = new Jewelry(id, name, desc, price, sellerId);
                break;
            case "MUSIC":
                item = new Music(id, name, desc, price, sellerId);
                break;
            case "FURNITURE":
                item = new Furniture(id, name, desc, price, sellerId);
                break;
            default:
                item = new Art(id, name, desc, price, sellerId);
        }
        item.setImagePath(imagePath);
        return item;
    }
}
