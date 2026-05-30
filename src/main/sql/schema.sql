-- Tạo database cho ứng dụng đấu giá
CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- Bảng users (tài khoản đăng nhập)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    is_seller BOOLEAN DEFAULT TRUE,
    is_bidder BOOLEAN DEFAULT TRUE,
    balance DECIMAL(15,2) DEFAULT 300000,
    avatar_path VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng items (vật phẩm đấu giá)
CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    start_price DECIMAL(15,2) NOT NULL,
    seller_id VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    image_path VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- Bảng auction_sessions (phiên đấu giá)
CREATE TABLE IF NOT EXISTS auction_sessions (
    id VARCHAR(50) PRIMARY KEY,
    item_id VARCHAR(50) NOT NULL,
    seller_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN',
    current_price DECIMAL(15,2),
    start_price DECIMAL(15,2) NOT NULL,
    highest_bidder_id VARCHAR(50),
    winner_id VARCHAR(50),
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    duration_minutes INT NOT NULL,
    min_increment DECIMAL(15,2) DEFAULT 1.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- Bảng bids (lịch sử đặt giá)
CREATE TABLE IF NOT EXISTS bids (
    id VARCHAR(50) PRIMARY KEY,
    auction_id VARCHAR(50) NOT NULL,
    bidder_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auction_sessions(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);

-- Index để tăng tốc tìm kiếm
CREATE INDEX idx_auction_session_item ON auction_sessions(item_id);
CREATE INDEX idx_auction_session_status ON auction_sessions(status);
CREATE INDEX idx_bid_auction ON bids(auction_id);
CREATE INDEX idx_item_seller ON items(seller_id);

-- Bảng chat_messages (tin nhắn chat trong phòng đấu giá)
CREATE TABLE IF NOT EXISTS chat_messages (
    id VARCHAR(50) PRIMARY KEY,
    auction_id VARCHAR(50) NOT NULL,
    sender_id VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auction_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng watchlist (danh sách theo dõi sản phẩm)
CREATE TABLE IF NOT EXISTS watchlist (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    auction_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_auction (user_id, auction_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (auction_id) REFERENCES auction_sessions(id) ON DELETE CASCADE
);

-- Index cho chat và watchlist để tăng tốc truy vấn
CREATE INDEX idx_chat_auction ON chat_messages(auction_id);
CREATE INDEX idx_watchlist_user ON watchlist(user_id);
