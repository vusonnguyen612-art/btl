# 🏆 Hệ Thống Đấu Giá Trực Tuyến

[![Java CI](https://github.com/vusonnguyen612-art/btl/actions/workflows/ci.yml/badge.svg)](https://github.com/vusonnguyen612-art/btl/actions/workflows/ci.yml)
[![Qodana](https://github.com/vusonnguyen612-art/btl/actions/workflows/qodana_code_quality.yml/badge.svg)](https://github.com/vusonnguyen612-art/btl/actions/workflows/qodana_code_quality.yml)

**Môn học:** Lập Trình Nâng Cao  
**Sinh viên thực hiện:**
- Đỗ Hải Đăng — MSSV: 25020113
- Nguyễn Vũ Sơn — MSSV: 25020352
- Lê Quang Nghĩa — MSSV: 25020292
- Nguyễn Duy Quang — MSSV: 25020330

  
-link báo cáo: https://drive.google.com/file/d/1jNMlYV-1TlZCKRZs2b7QegoEDIjpZXFH/view?usp=sharing
-link video: https://drive.google.com/file/d/1iTeP5XMywBPlTEgVGyPKDpn5wZeVkg01/view?usp=sharing
---

## Mục lục

1. [Mô tả dự án](#mô-tả-dự-án)
2. [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
3. [Công nghệ sử dụng](#công-nghệ-sử-dụng)
4. [Cài đặt & Chạy](#cài-đặt--chạy)
5. [Cấu trúc thư mục](#cấu-trúc-thư-mục)
6. [Các thành phần chính](#các-thành-phần-chính)
7. [Luồng xử lý nghiệp vụ](#luồng-xử-lý-nghiệp-vụ)
8. [Các tính năng](#các-tính-năng)
9. [API Messages (Giao thức mạng)](#api-messages-giao-thức-mạng)
10. [Cơ sở dữ liệu](#cơ-sở-dữ-liệu)
11. [Bảo mật & Xử lý lỗi](#bảo-mật--xử-lý-lỗi)
12. [Testing](#testing)

---

## Mô tả dự án

Đây là một hệ thống **đấu giá trực tuyến** (online auction) được xây dựng bằng Java, cho phép người dùng đăng ký tài khoản, tạo vật phẩm, tổ chức phiên đấu giá, và tham gia đặt giá theo thời gian thực qua kết nối TCP Socket.

**Phạm vi hệ thống:**
- Hỗ trợ 3 vai trò: **Admin**, **Seller** (người bán), **Bidder** (người mua)
- Quản lý vòng đời phiên đấu giá đầy đủ: OPEN → RUNNING → PAYMENT_PENDING → PAID / FINISHED / CANCELED
- Đấu giá thời gian thực với cơ chế **Sniper Protection** và **AutoBid (Second-Price)**
- Chat trực tiếp theo phiên, watchlist, thống kê, biểu đồ giá
- Quản trị viên: quản lý người dùng, vật phẩm, khóa/mở tài khoản, xem lịch sử đấu giá

Hệ thống sử dụng mô hình **Client — Server** qua giao thức TCP Socket:
- **Server** xử lý tất cả nghiệp vụ (đấu giá, thanh toán, AutoBid, chat, watchlist, admin)
- **Client** là ứng dụng JavaFX desktop kết nối tới Server

### Ví dụ luồng đơn giản

1. Người bán (Seller) đăng nhập → tạo vật phẩm → tạo phiên đấu giá → bắt đầu phiên
2. Người mua (Bidder) đăng nhập → xem danh sách phiên → đặt giá
3. Hết thời gian → người trả giá cao nhất thắng → thanh toán

---

## Kiến trúc hệ thống

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          CLIENT (JavaFX)                                  │
│  ┌──────────┐  ┌────────────┐  ┌──────────────┐  ┌──────────────────┐    │
│  │ LoginApp │  │ Controller │  │ FXML Views   │  │ NetworkService   │    │
│  │ (Launcher)│  │ (JavaFX)   │  │ (.fxml)      │  │ (Socket Client)  │    │
│  └──────────┘  └────────────┘  └──────────────┘  └────────┬─────────┘    │
│                                                            │              │
└────────────────────────────────────────────────────────────┼──────────────┘
                                                             │
                                                      TCP Socket
                                                     (port 8989)
                                                             │
┌────────────────────────────────────────────────────────────┼──────────────┐
│                          SERVER                            │              │
│  ┌────────────────────────────────────────────────────┐    │              │
│  │                AuctionServer                       │    │              │
│  │  ┌──────────────┐  ┌───────────────────────────┐  │    │              │
│  │  │ ClientHandler │  │ ClientHandler │ ...      │  │    │              │
│  │  │ (Thread #1)   │  │ (Thread #N)              │  │    │              │
│  │  └──────┬───────┘  └──────────┬────────────────┘  │    │              │
│  │         │                     │                    │    │              │
│  │         └─────────┬───────────┘                    │    │              │
│  │                   ▼                                │    │              │
│  │          ┌──────────────────┐                      │    │              │
│  │          │    AuctionDAO    │  (synchronized)       │    │              │
│  │          │  + UserDAO       │                      │    │              │
│  │          │  + ItemDAO       │                      │    │              │
│  │          │  + ChatDAO       │                      │    │              │
│  │          │  + WatchlistDAO  │                      │    │              │
│  │          │  + BidDAO        │                      │    │              │
│  │          │  + AuctionSessionDAO                    │    │              │
│  │          │  + AutoBidEngine │                      │    │              │
│  │          │  + DatabaseUtil  │                      │    │              │
│  │          └────────┬─────────┘                      │    │              │
│  └───────────────────┼────────────────────────────────┘    │              │
│                      ▼                                     │              │
│            ┌──────────────────┐                            │              │
│            │     MySQL DB     │                            │              │
│            │   (auction_db)   │                            │              │
│            └──────────────────┘                            │              │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|------------|-----------|
| Ngôn ngữ | Java 21 |
| Giao diện | JavaFX 21 (FXML + Scene Builder) |
| Cơ sở dữ liệu | MySQL 8+ |
| Connection Pool | HikariCP 5.1.0 |
| Build tool | Maven 3.9+ (kèm Maven Wrapper `./mvnw`) |
| Mạng | TCP Socket (ObjectOutputStream/InputStream) |
| Tunnel | Ngrok (tùy chọn, cho truy cập từ internet) |
| Testing | JUnit Jupiter 5.12 |
| Code Coverage | JaCoCo 0.8.14 (ngưỡng ≥30%) |
| Vulnerability Check | OWASP Dependency-Check 10.0.4 (opt-in) |
| Design Patterns | Singleton, Factory, Observer, DAO, Template Method |

---

## Cài đặt & Chạy

### Yêu cầu

| Thành phần | Phiên bản tối thiểu | Ghi chú |
|------------|---------------------|---------|
| **Java JDK** | 21+ | Tải từ [Adoptium](https://adoptium.net/) hoặc [Oracle](https://www.oracle.com/java/) |
| **MySQL** | 8.0+ | Chạy local trên cổng 3306 |
| **Maven** | 3.9+ | Hoặc dùng `mvnw` (Maven Wrapper) đã kèm trong project |
| **JavaFX SDK** | 21 | Maven tự tải qua plugin `javafx-maven-plugin` |

> **Lưu ý:** Tất cả lệnh dưới đây đều chạy được trên **Windows** (Git Bash / cmd), **Linux** và **macOS**. Chỉ cần Java 21+ và Maven 3.9+ (hoặc `./mvnw` / `mvnw.cmd`).

---

### 1. Tạo database

**Cách 1 — Dùng schema file (khuyên dùng):**

```bash
# Trên mọi OS
mysql -u root -p < src/main/sql/schema.sql
```

**Cách 2 — Thủ công:**

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS auction_db"
```

Hoặc dùng MySQL Workbench / phpMyAdmin.

---

### 2. Cấu hình kết nối DB

Sửa file `src/main/java/DAO/DatabaseUtil.java` nếu thông tin đăng nhập MySQL khác:

```java
config.setJdbcUrl("jdbc:mysql://localhost:3306/auction_db");
config.setUsername("root");
config.setPassword("123456789");
```

---

### 3. Build project

```bash
# Linux / macOS
./mvnw clean package -DskipTests

# Windows (Git Bash / cmd)
mvnw.cmd clean package -DskipTests

# Hoặc nếu đã cài Maven toàn cục (mọi OS)
mvn clean package -DskipTests
```

Kết quả: file `target/auction-system-1.0-SNAPSHOT.jar` (fat-JAR ~17MB, chứa tất cả dependencies).

---

### 4. Chạy Server

Server TCP lắng nghe trên cổng **8989**:

```bash
# Cách 1 — Dùng fat-JAR (khuyên dùng, chạy trên mọi OS)
java -cp target/auction-system-1.0-SNAPSHOT.jar Network.AuctionServer

# Cách 2 — Dùng Maven (mọi OS)
./mvnw exec:java -Dexec.mainClass="Network.AuctionServer"
# hoặc trên Windows
mvnw.cmd exec:java -Dexec.mainClass="Network.AuctionServer"

# Cách 3 — Dùng Maven toàn cục (mọi OS)
mvn exec:java -Dexec.mainClass="Network.AuctionServer"
```

Khi Server chạy thành công, bạn sẽ thấy:
```
Server started on port 8989
Server initialized with DAO pattern (MySQL)
```

> **Lưu ý:** Trên **Linux/macOS**, nếu port 8989 bị chiếm, kiểm tra bằng `lsof -i :8989` hoặc `netstat -an | grep 8989`. Trên **Windows**, dùng `netstat -ano | grep 8989`.

---

### 5. Chạy Client (JavaFX)

Mở một terminal **khác** và chạy:

```bash
# Cách 1 — Dùng Maven Wrapper (khuyên dùng, mọi OS)
./mvnw javafx:run

# Trên Windows
mvnw.cmd javafx:run

# Cách 2 — Dùng Maven toàn cục (mọi OS)
mvn javafx:run

# Cách 3 — Fat-JAR (chạy giao diện JavaFX, mọi OS)
java -jar target/auction-system-1.0-SNAPSHOT.jar
```

Client mặc định kết nối tới `localhost:8989`.

---

### 6. (Tùy chọn) Expose Server qua Internet

Dùng Ngrok để server có thể truy cập từ máy khác:

```bash
ngrok tcp 8989
```

Sao chép địa chỉ Ngrok (vd `0.tcp.ap.ngrok.io:12345`) và cập nhật trong `NetworkService.java`:

```java
// Server address
private static final String SERVER_HOST = "0.tcp.ap.ngrok.io";
private static final int SERVER_PORT = 12345;
```

---

### 7. (Tùy chọn) Migrate mật khẩu cũ

Nếu đã có dữ liệu cũ với mật khẩu plain text, chạy script một lần:

```bash
./mvnw exec:java -Dexec.mainClass="DAO.MigratePasswords"
```

Script này hash toàn bộ mật khẩu plain text bằng SHA-256 + salt.

---

### Tóm tắt thứ tự chạy

```
1. MySQL    → chạy sẵn trên localhost:3306 (hoặc mysql -u root -p < src/main/sql/schema.sql)
2. Build    → ./mvnw clean package -DskipTests
3. Server   → java -cp target/auction-system-1.0-SNAPSHOT.jar Network.AuctionServer
4. Client   → ./mvnw javafx:run         (mở terminal mới)
```

---

## Cấu trúc thư mục

```
btl/
├── pom.xml                              # Maven config
├── README.md                            # Bạn đang đọc đây
├── mvnw, mvnw.cmd                       # Maven Wrapper (chạy không cần cài Maven)
├── run-server.bat                       # Script Windows chạy server
├── auction_data.ser                     # File serialize cho in-memory mode
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── Launch.java                 # Entry point (reflection -> LoginApp)
│   │   │   ├── LoginApp.java               # JavaFX Application starter
│   │   │   │
│   │   │   ├── Controller/                 # JavaFX Controllers
│   │   │   │   ├── LoginController.java    # Đăng nhập & Đăng ký
│   │   │   │   ├── UserController.java     # Điều hướng chính (trang chủ, pane quản lý)
│   │   │   │   ├── AuctionRoomController.java
│   │   │   │   ├── AuctionCardController.java
│   │   │   │   ├── BidCardController.java
│   │   │   │   ├── BidHistoryCardController.java
│   │   │   │   ├── BidChartViewController.java
│   │   │   │   ├── CreateItemsController.java
│   │   │   │   ├── ItemCardController.java
│   │   │   │   ├── AdminPaneController.java        # ⭐ Admin: quản lý user/item
│   │   │   │   ├── TrangchuPaneController.java     # Trang chủ
│   │   │   │   ├── KhoPaneController.java          # Kho hàng
│   │   │   │   ├── CaidatPaneController.java       # Cài đặt
│   │   │   │   ├── NaptienPaneController.java      # Nạp tiền
│   │   │   │   ├── LichsudaugiaPaneController.java # Lịch sử đấu giá
│   │   │   │   ├── WatchlistPaneController.java    # Theo dõi
│   │   │   │   ├── ThongkePaneController.java      # Thống kê
│   │   │   │   └── utils/                          # ⭐ Tiện ích Controller
│   │   │   │       ├── UIUtils.java                #   UI helper (ScrollPane, ImageView...)
│   │   │   │       ├── ResponseUtils.java          #   Parse Message response
│   │   │   │       ├── SearchCriteriaBuilder.java  #   Builder tìm kiếm từ UI
│   │   │   │       ├── FormatUtils.java            #   Format tiền, thời gian
│   │   │   │       ├── CategoryMapper.java         #   Ánh xạ danh mục Việt-Anh
│   │   │   │       └── AlertUtils.java             #   Alert dialog dùng chung
│   │   │   │
│   │   │   ├── Service/                        # ⭐ Lớp Service (in-memory, cho test/offline)
│   │   │   │   ├── AuctionManager.java          #   Quản lý đấu giá in-memory (singleton)
│   │   │   │   ├── UserService.java             #   Service xử lý User
│   │   │   │   ├── ItemService.java             #   Service xử lý Item
│   │   │   │   └── DataService.java             #   Serialize/deserialize dữ liệu
│   │   │   │
│   │   │   ├── DAO/                          # Data Access Objects
│   │   │   │   ├── DatabaseUtil.java          #   HikariCP connection pool
│   │   │   │   ├── UserDAO.java               #   users table (login, register, balance...)
│   │   │   │   ├── ItemDAO.java               #   items table
│   │   │   │   ├── AuctionDAO.java            #   auction_sessions + bids (placeBid, finish...)
│   │   │   │   ├── AuctionSessionDAO.java     #   ⭐ CRUD cơ bản cho auction_sessions
│   │   │   │   ├── BidDAO.java                #   ⭐ Truy vấn bids (winner, lịch sử)
│   │   │   │   ├── ChatDAO.java               #   chat_messages
│   │   │   │   ├── WatchlistDAO.java          #   watchlist
│   │   │   │   └── MigratePasswords.java      #   ⭐ Script migration password (SHA-256)
│   │   │   │
│   │   │   ├── Model/                        # Domain models
│   │   │   │   ├── Entity.java (abstract)     #   ⭐ Lớp cơ sở (id, createdAt, equals/hashCode)
│   │   │   │   ├── User.java                  #   Người dùng
│   │   │   │   ├── Admin.java                 #   Quản trị viên
│   │   │   │   ├── Bidder.java                #   Người mua
│   │   │   │   ├── Seller.java                #   Người bán
│   │   │   │   ├── Item.java (abstract)       #   Vật phẩm
│   │   │   │   ├── Art.java, Books.java, Electronics.java, Fashion.java,
│   │   │   │   │   Furniture.java, Jewelry.java, Music.java, Sports.java,
│   │   │   │   │   Vehicle.java               #   9 danh mục con
│   │   │   │   ├── AuctionSession.java        #   Phiên đấu giá
│   │   │   │   ├── Bid.java                   #   Lượt đặt giá
│   │   │   │   ├── AutoBid.java               #   ⭐ Cấu hình tự động trả giá
│   │   │   │   ├── ChatMessage.java           #   ⭐ Tin nhắn chat
│   │   │   │   ├── SearchCriteria.java        #   ⭐ Tiêu chí tìm kiếm
│   │   │   │   └── AuctionObservable.java     #   ⭐ Observer pattern base
│   │   │   │
│   │   │   ├── Network/                      # Mạng & giao tiếp
│   │   │   │   ├── AuctionServer.java         #   Server TCP đa luồng
│   │   │   │   ├── AuctionClient.java         #   Client CLI (test)
│   │   │   │   ├── NetworkService.java        #   Client singleton (JavaFX)
│   │   │   │   ├── Message.java               #   Giao thức message
│   │   │   │   ├── MessageFactory.java        #   ⭐ Factory tạo message chuẩn
│   │   │   │   ├── AutoBidEngine.java         #   ⭐ Engine xử lý AutoBid
│   │   │   │   └── NgrokTunnel.java           #   Ngrok tunnel
│   │   │   │
│   │   │   ├── Factory/
│   │   │   │   ├── ItemFactory.java           # Factory pattern cho Item
│   │   │   │   └── UserFactory.java           # Factory pattern cho User
│   │   │   │
│   │   │   ├── Exception/
│   │   │   │   ├── AuctionClosedException.java
│   │   │   │   ├── InvalidBidException.java
│   │   │   │   ├── InsufficientBalanceException.java
│   │   │   │   ├── UnauthorizedException.java
│   │   │   │   ├── AuthenticationException.java
│   │   │   │   └── ItemNotFoundException.java
│   │   │   │
│   │   │   └── Observer/
│   │   │       └── AuctionObserver.java       # Observer pattern
│   │   │
│   │   ├── resources/                        # FXML & assets
│   │   │   ├── login.fxml
│   │   │   ├── signin.fxml
│   │   │   ├── auctionRoom.fxml
│   │   │   ├── CreateItems.fxml
│   │   │   ├── Indivisual.fxml               # Giao diện chính (chứa các pane)
│   │   │   ├── item_card.fxml
│   │   │   ├── auction_card.fxml
│   │   │   ├── bid_card.fxml
│   │   │   ├── bid_history_card.fxml
│   │   │   ├── bid_chart_view.fxml
│   │   │   └── panes/
│   │   │       ├── trangchu_pane.fxml
│   │   │       ├── kho_pane.fxml
│   │   │       ├── caidat_pane.fxml
│   │   │       ├── naptien_pane.fxml
│   │   │       ├── lichsudaugia_pane.fxml
│   │   │       ├── thongke_pane.fxml
│   │   │       ├── watchlist_pane.fxml
│   │   │       └── admin_pane.fxml           # ⭐ Admin panel
│   │   │
│   │   └── sql/
│   │       └── schema.sql                    # ⭐ Script tạo database & tables
│   │
│   └── test/java/test/
│       ├── AuctionManagerTest.java           # 34 tests
│       ├── AuctionSessionTest.java           # 26 tests
│       ├── ItemFactoryTest.java              # 19 tests
│       ├── UserFactoryTest.java              # 15 tests
│       ├── NewFeaturesTest.java              # 11 tests (DB integration)
│       ├── ExceptionTest.java                # 6 tests
│       └── ItemSubclassTest.java             # 4 tests
│
├── .github/workflows/
│   ├── ci.yml                               # GitHub Actions CI
│   └── qodana_code_quality.yml              # Qodana code quality
│
└── owasp-suppressions.xml                   # OWASP false-positive suppressions
```

> ⭐ = file/package mới bổ sung so với phiên bản đầu.

---

## Các thành phần chính

### Model — Lớp dữ liệu

Mỗi lớp model implements `Serializable` để truyền qua socket. `Entity` là lớp cơ sở trừu tượng chứa `id` và `createdAt`, cung cấp `equals()`/`hashCode()` chuẩn.

| Lớp | Vai trò |
|-----|---------|
| `Entity` (abstract) | Lớp cơ sở: id, createdAt, equals/hashCode, getSpecificInfo() |
| `User` | Người dùng: ID, username, password (hashed SHA-256), email, balance, roles |
| `Admin` | Kế thừa User, có quyền quản trị |
| `Bidder` | Người mua |
| `Seller` | Người bán |
| `Item` (abstract) | Vật phẩm: ID, tên, mô tả, giá khởi điểm, sellerId, category. 9 subclass: Art, Books, Electronics, Fashion, Furniture, Jewelry, Music, Sports, Vehicle |
| `AuctionSession` | Phiên đấu giá: trạng thái (OPEN → RUNNING → PAYMENT_PENDING/FINISHED → PAID/CANCELED), giá hiện tại, highestBidder, winner, thời gian, sniper protection |
| `Bid` | Một lượt đặt giá: auctionId, bidderId, amount, timestamp, isWinner |
| `AutoBid` | Cấu hình tự động trả giá: userId, auctionId, maxAmount, increment |
| `SearchCriteria` | Tiêu chí tìm kiếm: keyword, category, statuses, price range, sort, sellerId |
| `ChatMessage` | Tin nhắn trong phòng đấu giá (id, auctionId, senderId, senderName, message, timestamp) |
| `AuctionObservable` | Observable pattern base (sử dụng CopyOnWriteArrayList) |

### DAO — Truy xuất dữ liệu

Tất cả DAO đều dùng **HikariCP connection pool** (max 10 connections). Các method CRUD đều mở connection riêng qua `DatabaseUtil.getConnection()`.

| DAO | Bảng | Method chính |
|-----|------|-------------|
| `UserDAO` | `users` | register, login, authenticate, getBalance, addBalance, changePassword, resetPassword, blockUser, updateAvatarPath, getAllUsers, deleteUser |
| `ItemDAO` | `items` | save, update, findById, findBySellerId, findAll, delete |
| `AuctionDAO` | `auction_sessions`, `bids` | **placeBid** (synchronized + transaction), startAuction, finishAuction, processPayment, penalizeWinner, stopAuction, getBidHistory, searchAuctions, getAuctionByItemId |
| `AuctionSessionDAO` | `auction_sessions` | save, update, findById, findByStatus, findBySellerId, findAll |
| `BidDAO` | `bids` | getHighestBid, markAsWinner, getWinningBidsByUserId |
| `ChatDAO` | `chat_messages` | saveChatMessage, getChatHistory |
| `WatchlistDAO` | `watchlist` | addWatchlist, removeWatchlist, getWatchlist |

### Server — AuctionServer

Là một **TCP Server** đa luồng. Mỗi client kết nối được xử lý bởi 1 `ClientHandler` (extends Thread riêng).

**Cơ chế đồng bộ:**
- `AuctionDAO.placeBid()` — **`synchronized`** (chỉ 1 thread đặt giá tại 1 thời điểm)
- `AuctionDAO.finishAuction()` — **`synchronized`**
- `AuctionDAO.processPayment()` — **`synchronized`**
- `AutoBid` list — **`synchronized(autoBidLock)`**
- Transaction atomic cho INSERT + UPDATE bid

**Các scheduler chạy nền:**
- **Penalty timer** (30s interval): phạt người thắng quá hạn thanh toán 50,000đ
- **Watchlist timer** (10s interval): gửi notification khi phiên sắp kết thúc (≤5 phút)

**Các message type server xử lý (đầy đủ):**
- Đăng nhập/đăng ký: LOGIN, REGISTER
- Vật phẩm: CREATE_ITEM, GET_ITEMS, DELETE_ITEM
- Phiên đấu giá: CREATE_AUCTION, GET_AUCTIONS, GET_AUCTION, START_AUCTION, FINISH_AUCTION, CANCEL_AUCTION
- Đặt giá: PLACE_BID, PROCESS_PAYMENT, SET_AUTOBID, REMOVE_AUTOBID
- Tìm kiếm: SEARCH_AUCTIONS, GET_BID_HISTORY
- Chat: SEND_CHAT_MESSAGE, GET_CHAT_HISTORY
- Watchlist: ADD_WATCHLIST, REMOVE_WATCHLIST, GET_WATCHLIST
- Tài khoản: DEPOSIT, CHANGE_PASSWORD, UPDATE_AVATAR
- **Admin: GET_USERS, DELETE_USER, GET_USER_BID_HISTORY, BLOCK_USER, UNBLOCK_USER**

---

## Luồng xử lý nghiệp vụ

### 1. Đấu giá (Place Bid)

```
Client gửi PLACE_BID → Server.handlePlaceBid()
                              │
                              ▼
                    auctionDAO.placeBid()  [synchronized]
                              │
                    ┌─────────┴──────────┐
                    ▼                    ▼
            Đọc session từ DB    Kiểm tra:
            (findAuctionById)    • Session tồn tại?
                                 • Status = RUNNING?
                                 • Chưa hết giờ?
                                 • Không phải seller?
                                 • amount > currentPrice?
                                 • amount >= currentPrice + minIncrement?
                                 • Số dư >= amount?
                    │
                    ▼
            BEGIN TRANSACTION
              ├── INSERT INTO bids
              ├── UPDATE auction_sessions SET current_price, highest_bidder_id
              └── [Sniper Protection] Nếu ≤2 phút cuối → gia hạn +2 phút
            COMMIT
                    │
                    ▼
            processAutoBids() [nếu có AutoBid]
                    │
                    ▼
            Gửi response SUCCESS/ERROR
```

### 2. AutoBid (Tự động trả giá)

```
Khi 1 bid được đặt → processAutoBids() chạy

Cơ chế Second-Price (Vickrey):
1. Lấy danh sách AutoBid của phiên
2. Sắp xếp giảm dần theo maxAmount
3. Tìm người có maxAmount lớn nhất (≠ highestBidder)
4. Tính giá cần đặt:
   - Nếu có người thứ 2: maxNeeded = min(secondMax + increment, bestMax)
   - Nếu chỉ 1 người: bidAmount = currentPrice + increment
5. Đặt giá → lặp lại (tối đa 100 lần)
```

### 3. Vòng đời phiên đấu giá

```
                    ┌──────────┐
                    │   OPEN   │  ← Seller tạo phiên
                    └────┬─────┘
                         │ startAuction()
                         ▼
                    ┌──────────┐
           ┌───────│ RUNNING  │───────┐
           │       └──────────┘       │
           │  hết giờ              hết giờ
           │  (có highestBidder)   (không bid nào)
           ▼                        ▼
   ┌─────────────────┐      ┌──────────┐
   │ PAYMENT_PENDING │      │ FINISHED │
   └────────┬────────┘      └──────────┘
            │ processPayment() (trong 1 giờ)
            ▼
      ┌──────────┐
      │   PAID   │
      └──────────┘

Nếu quá 1 giờ không thanh toán → phạt 50,000đ → FINISHED.

Có thể hủy (CANCELED) bất kỳ lúc nào trước khi PAID.
```

### 4. Thanh toán

```
processPayment()  [synchronized + transaction]
├── SELECT current_price, highest_bidder_id, seller_id
├── UPDATE users SET balance = balance - amount  (người thắng)
├── UPDATE users SET balance = balance + amount  (người bán)
├── UPDATE auction_sessions SET status = 'PAID'
└── COMMIT (rollback nếu lỗi)
```

---

## Các tính năng

### ✅ Đăng ký / Đăng nhập
- Mật khẩu mã hóa SHA-256 + salt (`AuCtIoNaPpSaLt!#`)
- Phân quyền Seller/Bidder/Admin
- Số dư mặc định ban đầu: 0$
- Tự động tạo tài khoản Admin mặc định khi server khởi động
- Script migration `MigratePasswords` để hash mật khẩu cũ

### ✅ Quản lý vật phẩm (Item)
- 9 danh mục: Art, Books, Electronics, Fashion, Furniture, Jewelry, Music, Sports, Vehicle
- Factory Pattern (`ItemFactory`) để tạo Item
- Chỉ seller được tạo, sửa, xóa
- Xem kho hàng cá nhân

### ✅ Phiên đấu giá (Auction Session)
- Trạng thái: OPEN → RUNNING → PAYMENT_PENDING → PAID / FINISHED / CANCELED
- Tự động kết thúc khi hết thời gian
- Chỉ seller được start/stop
- Bước giá tối thiểu: 1.0

### ✅ Sniper Protection
- Nếu có bid trong **2 phút cuối** → tự động **gia hạn thêm 2 phút**
- Chống chiến thuật "sniping" (đặt giá vào giây cuối)

### ✅ AutoBid (Tự động trả giá)
- Người dùng cài đặt mức giá tối đa
- Hệ thống tự động đặt giá theo cơ chế Second-Price (Vickrey)
- Tự động gỡ nếu không đủ số dư
- Engine riêng (`AutoBidEngine`) tách khỏi ClientHandler

### ✅ Watchlist (Theo dõi)
- Thêm/xóa phiên yêu thích
- Nhận thông báo khi phiên sắp kết thúc (≤5 phút)

### ✅ Chat theo phiên
- Phòng chat riêng cho mỗi phiên đấu giá
- Lưu lịch sử tin nhắn
- Hiển thị tên người gửi

### ✅ Nạp tiền & Thanh toán
- Nạp tiền vào tài khoản
- Thanh toán tự động trừ số dư người thắng, cộng cho người bán
- Phạt 50,000đ nếu quá 1 giờ không thanh toán

### ✅ Tìm kiếm & Lọc
- Theo từ khóa, danh mục, trạng thái, khoảng giá, người bán
- Sắp xếp: giá tăng/giảm, mới nhất/cũ nhất, tên
- Builder pattern (`SearchCriteriaBuilder`) thống nhất giữa các Controller

### ✅ Thống kê & Lịch sử
- Xem lịch sử đấu giá đã tham gia
- Biểu đồ giá theo thời gian (`BidChartViewController`)
- Thống kê tổng quan (`ThongkePaneController`)

### ✅ Quản trị (Admin)
- **AdminPanel**: giao diện quản trị riêng (`admin_pane.fxml`)
- Xem danh sách tất cả người dùng (ID, username, email, role, balance, trạng thái block)
- Xem danh sách tất cả vật phẩm
- **Block/Unblock** người dùng
- **Xóa** người dùng (không xóa được admin)
- **Xóa** vật phẩm
- Xem lịch sử đấu giá của từng người dùng
- 3-layer access control: FXML visibility + event guard + server-side admin check

### ✅ Bảo mật nâng cao
- Mật khẩu: SHA-256 + salt, không lưu plain text
- Connection pool: HikariCP, không lộ credentials
- Admin check phía server: không thể leo thang qua REGISTER
- Chặn seller tự đặt giá sản phẩm của mình
- Transaction atomic cho đặt giá và thanh toán

---

## API Messages (Giao thức mạng)

Client và Server giao tiếp qua **Message** — một lớp Serializable chứa:

```java
class Message {
    Type type;          // Loại message
    String auctionId;   // ID phiên
    String itemId;      // ID vật phẩm
    String senderId;    // Người gửi
    Object data;        // Dữ liệu đính kèm
    String content;     // Nội dung text
    List<Message> notifications;  // Notification kèm theo
}
```

### Message types

| Message type | Chiều | Mô tả |
|-------------|-------|-------|
| `LOGIN` | C→S | Đăng nhập |
| `REGISTER` | C→S | Đăng ký |
| `GET_AUCTIONS` | C→S | Lấy danh sách phiên |
| `GET_AUCTION` | C→S | Lấy chi tiết 1 phiên |
| `CREATE_AUCTION` | C→S | Tạo phiên mới |
| `START_AUCTION` | C→S | Bắt đầu phiên |
| `PLACE_BID` | C→S | Đặt giá |
| `FINISH_AUCTION` | C→S | Kết thúc phiên |
| `CANCEL_AUCTION` | C→S | Hủy phiên |
| `GET_ITEMS` | C→S | Lấy danh sách vật phẩm |
| `CREATE_ITEM` | C→S | Tạo vật phẩm |
| `DELETE_ITEM` | C→S | Xóa vật phẩm (admin) |
| `SET_AUTOBID` | C→S | Cài AutoBid |
| `REMOVE_AUTOBID` | C→S | Gỡ AutoBid |
| `PROCESS_PAYMENT` | C→S | Thanh toán |
| `DEPOSIT` | C→S | Nạp tiền |
| `SEARCH_AUCTIONS` | C→S | Tìm kiếm |
| `GET_BID_HISTORY` | C→S | Lịch sử giá |
| `CHANGE_PASSWORD` | C→S | Đổi mật khẩu |
| `UPDATE_AVATAR` | C→S | Cập nhật avatar |
| `SEND_CHAT_MESSAGE` | C→S | Gửi chat |
| `GET_CHAT_HISTORY` | C→S | Lịch sử chat |
| `ADD_WATCHLIST` | C→S | Thêm theo dõi |
| `REMOVE_WATCHLIST` | C→S | Bỏ theo dõi |
| `GET_WATCHLIST` | C→S | DS theo dõi |
| `GET_USERS` | C→S | DS người dùng (admin) |
| `DELETE_USER` | C→S | Xóa người dùng (admin) |
| `GET_USER_BID_HISTORY` | C→S | LS đấu giá user (admin) |
| `BLOCK_USER` | C→S | Chặn người dùng (admin) |
| `UNBLOCK_USER` | C→S | Mở chặn (admin) |
| `NOTIFICATION` | S→C | Thông báo |
| `SUCCESS` | S→C | Thành công |
| `ERROR` | S→C | Thất bại |

---

## Cơ sở dữ liệu

### Tạo database

```bash
mysql -u root -p < src/main/sql/schema.sql
```

File `schema.sql` tạo đầy đủ 6 bảng: `users`, `items`, `auction_sessions`, `bids`, `chat_messages`, `watchlist` kèm indexes.

### Các bảng

```sql
-- Người dùng
users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) UNIQUE,
    password VARCHAR(255),             -- SHA-256 hash (64 hex chars)
    email VARCHAR(255),
    balance DECIMAL(15,2) DEFAULT 0,
    is_seller BOOLEAN DEFAULT TRUE,
    is_bidder BOOLEAN DEFAULT TRUE,
    is_blocked BOOLEAN DEFAULT FALSE,  -- ⭐ Admin block
    avatar_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- Vật phẩm
items (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    start_price DOUBLE,
    seller_id VARCHAR(50),
    category VARCHAR(50),
    image_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id)
)

-- Phiên đấu giá
auction_sessions (
    id VARCHAR(50) PRIMARY KEY,
    item_id VARCHAR(50),
    seller_id VARCHAR(50),
    status ENUM('OPEN','RUNNING','PAYMENT_PENDING','FINISHED','PAID','CANCELED'),
    current_price DOUBLE,
    start_price DOUBLE,
    highest_bidder_id VARCHAR(50),
    winner_id VARCHAR(50),
    start_time DATETIME,
    end_time DATETIME,
    duration_minutes BIGINT,
    min_increment DOUBLE DEFAULT 1.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (seller_id) REFERENCES users(id)
)

-- Lịch sử đặt giá
bids (
    id VARCHAR(50) PRIMARY KEY,
    auction_id VARCHAR(50),
    bidder_id VARCHAR(50),
    amount DOUBLE,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auction_sessions(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
)

-- Tin nhắn chat
chat_messages (
    id VARCHAR(50) PRIMARY KEY,
    auction_id VARCHAR(50),
    sender_id VARCHAR(50),
    message TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auction_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4

-- Danh sách theo dõi
watchlist (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50),
    auction_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_auction (user_id, auction_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (auction_id) REFERENCES auction_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
```

### Indexes

```sql
CREATE INDEX idx_auction_session_item ON auction_sessions(item_id);
CREATE INDEX idx_auction_session_status ON auction_sessions(status);
CREATE INDEX idx_bid_auction ON bids(auction_id);
CREATE INDEX idx_item_seller ON items(seller_id);
CREATE INDEX idx_chat_auction ON chat_messages(auction_id);
CREATE INDEX idx_watchlist_user ON watchlist(user_id);
```

---

## Bảo mật & Xử lý lỗi

### Các lỗi đã được xử lý

| Vấn đề | Giải pháp |
|--------|-----------|
| **Race condition bidding** | `synchronized` trên `AuctionDAO.placeBid()` |
| **Mất dữ liệu khi crash** | Transaction atomic (`commit`/`rollback`) cho INSERT+UPDATE bid |
| **Xung đột finishAuction + placeBid** | `synchronized` trên cả 2 method |
| **Xung đột thanh toán + kết thúc** | `synchronized` trên cả `finishAuction` và `processPayment` |
| **Password plain text** | SHA-256 + salt (`AuCtIoNaPpSaLt!#`) |
| **AutoBid inconsistent** | `processAutoBids` chạy trong `synchronized(autoBidLock)` |
| **NullPointer item bị xóa** | Fallback placeholder item |
| **Trùng ID auction** | Timestamp + random suffix |
| **Break-on-ERROR khiến client mất kết nối** | Client handler giữ kết nối sau lỗi, gửi ERROR message thay vì throw exception |
| **Leo thang ADMIN qua REGISTER** | Server-side: chỉ `seedDefaultAdmin()` tạo được admin, REGISTER luôn tạo user thường |
| **Cột password VARCHAR(64) bị tràn** | Mở rộng lên VARCHAR(255) để hỗ trợ hash dài |
| **Null AutoBid data** | Null check trước khi xử lý |

### Các exception nghiệp vụ

| Exception | Khi nào xảy ra |
|-----------|----------------|
| `AuthenticationException` | Sai username/password |
| `AuctionClosedException` | Phiên không tồn tại, chưa bắt đầu, hoặc đã kết thúc |
| `InvalidBidException` | Giá đặt ≤ giá hiện tại hoặc không đủ bước giá tối thiểu |
| `InsufficientBalanceException` | Số dư không đủ |
| `UnauthorizedException` | Seller tự đặt giá sản phẩm của mình |
| `ItemNotFoundException` | Vật phẩm không tồn tại |

### Các biện pháp bảo mật khác

- **Admin check 3 lớp**: FXML ẩn button + event guard + server-side `isAdmin()` kiểm tra
- **Không thể tạo admin qua REGISTER**: chỉ seed tự động khi server khởi động
- **Chặn seller tự bid**: kiểm tra `senderId != sellerId`
- **Block user**: user bị block không thể đăng nhập (kiểm tra `is_blocked` trong `authenticate`)

---

## Testing

Dự án có **115 tests** (JUnit 5), chia làm 7 test class:

| Test class | Mô tả | Số test |
|-----------|-------|---------|
| `AuctionManagerTest` | Quản lý đấu giá in-memory | 34 |
| `AuctionSessionTest` | Vòng đời phiên đấu giá (state machine) | 26 |
| `ItemFactoryTest` | Factory tạo vật phẩm + subclass | 19 |
| `UserFactoryTest` | Factory tạo user + password validation | 15 |
| `NewFeaturesTest` | Tính năng mới (DB integration) | 11 |
| `ExceptionTest` | Exception nghiệp vụ | 6 |
| `ItemSubclassTest` | Kế thừa Item | 4 |

Trong đó ~30 test là **negative tests** (kiểm thử biên, ngoại lệ, trạng thái không hợp lệ).

Toàn bộ test chạy được qua Maven (không cần DB cho unit test, cần MySQL cho integration test):

```bash
# Chỉ chạy unit test (không cần DB)
./mvnw test -Dtest='!NewFeaturesTest'

# Chạy tất cả (cần MySQL)
./mvnw test
```

Với code coverage (JaCoCo):
```bash
./mvnw verify
# Báo cáo HTML tại target/site/jacoco/index.html
```

Kiểm tra lỗ hổng bảo mật dependencies (opt-in):
```bash
./mvnw verify -P vulnerability-check
# Báo cáo HTML tại target/dependency-check-report.html
```

---

## Ghi chú phát triển

### Kiến trúc đồng bộ (Concurrency)

Hệ thống dùng 3 cơ chế đồng bộ chính:

1. **`synchronized` method** — `AuctionDAO.placeBid()`, `finishAuction()`, `processPayment()`
2. **`synchronized` block** — `autoBidLock` cho danh sách AutoBid, `auctionDAO` cho stopAuction
3. **Database transaction** — `setAutoCommit(false)` + `commit()`/`rollback()` cho ghi dữ liệu

### Các design pattern sử dụng

| Pattern | Vị trí |
|---------|--------|
| **Singleton** | `NetworkService`, `AuctionManager`, `DatabaseUtil` (HikCP datasource) |
| **Factory** | `ItemFactory`, `UserFactory`, `MessageFactory` |
| **Observer** | `AuctionObserver`, `AuctionObservable`, `AuctionSession.notify*()` |
| **DAO** | `*DAO` classes |
| **Template Method** | `Entity.getSpecificInfo()` (abstract), `Item.getSpecificInfo()` |
| **Builder** | `SearchCriteriaBuilder` |

### Hạn chế & Cải tiến có thể

- **Password hash**: Hiện dùng SHA-256, có thể nâng lên bcrypt/argon2
- **Connection pool**: Mỗi DAO method mở connection riêng, có thể tối ưu reuse connection
- **AutoBid loop**: Giới hạn 100 iterations, có thể thêm cơ chế exponential backoff
- **Penalty timer**: 1 giờ hardcode, có thể đưa vào config
- **In-memory mode**: `AuctionManager` singleton chỉ dùng cho test, không đồng bộ với DB
- **Admin audit log**: Chưa có log hành động admin
- **Real-time push**: Hiện tại client phải gửi request để nhận notification, có thể nâng lên WebSocket hoặc Server-Sent Events
