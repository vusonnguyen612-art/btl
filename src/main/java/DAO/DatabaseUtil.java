package DAO;

import java.sql.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Tiện ích quản lý kết nối MySQL và đóng tài nguyên. */
public class DatabaseUtil {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/auction_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456789";
    private static final Set<Connection> activeConnections = ConcurrentHashMap.newKeySet();

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL Driver not found", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseUtil::closeAllConnections));
    }

    /** Lấy kết nối mới từ DriverManager và ghi nhận vào activeConnections. */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        activeConnections.add(conn);
        return conn;
    }

    /** Đóng một hoặc nhiều tài nguyên (Connection, Statement, ResultSet…). */
    public static void close(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    if (resource instanceof Connection) {
                        activeConnections.remove(resource);
                    }
                    resource.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** Đóng tất cả kết nối đang hoạt động (dùng trong shutdown hook). */
    public static void closeAllConnections() {
        for (Connection conn : activeConnections) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        activeConnections.clear();
    }
}
