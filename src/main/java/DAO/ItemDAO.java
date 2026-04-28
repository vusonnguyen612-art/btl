package DAO;

import Model.Item;
import Model.Art;
import Model.Electronics;
import Model.Vehicle;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAO {
    
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
            default:
                item = new Art(id, name, desc, price, sellerId);
        }
        item.setImagePath(imagePath);
        return item;
    }
}