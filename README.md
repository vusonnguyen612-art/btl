# Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

## Luồng Chạy Chi Tiết

### 1. Khởi động ứng dụng
```
Launch.main() → Class.forName("LoginApp") → LoginApp.main() → launch(args)
```

### 2. Khởi tạo kết nối mạng
```
LoginApp.start() → NetworkService.getInstance() → connect() 
→ Socket("0.tcp.ap.ngrok.io", 20782)
→ Khởi tạo ObjectOutputStream/ObjectInputStream
```

### 3. Đăng nhập (Login Flow)
```
User nhập username/password → logincontroller → NetworkService.login()
→ Tạo Message(Type.LOGIN, data=username, content=password)
→ Gửi qua socket → AuctionServer.ClientHandler.processMessage()
→ handleLogin() → UserDAO.authenticate()
→ Truy vấn MySQL: SELECT * FROM users WHERE username=? AND password=?
→ Trả về Message(SUCCESS, data=User) hoặc Message(ERROR)
```

### 4. Đăng ký (Register Flow)
```
User nhập username/password → NetworkService.register()
→ UserFactory.getPasswordError() kiểm tra password
→ UserFactory.createUser() → UserDAO.register()
→ INSERT INTO users → Trả về Message(SUCCESS)
```

### 5. Tạo Item (Seller)
```
Seller tạo item → ItemFactory.createItem(category, ...)
→ Tạo Electronics/Art/Vehicle → NetworkService.createItem(item)
→ Message(Type.CREATE_ITEM, data=Item) → Server → ItemDAO.save()
→ INSERT INTO items
```

### 6. Tạo phiên đấu giá (Create Auction)
```
Seller chọn item → NetworkService.createAuction(itemId, duration)
→ Message(Type.CREATE_AUCTION) → Server:
  → ItemDAO.findById(itemId) → lấy Item
  → Tạo AuctionSession(id, item, sellerId, startPrice, duration)
  → AuctionDAO.saveAuction() → INSERT INTO auction_sessions
```

### 7. Bắt đầu đấu giá (Start Auction)
```
Seller start → NetworkService.startAuction(auctionId)
→ Server: AuctionDAO.startAuction()
→ UPDATE status='RUNNING', start_time=NOW(), end_time=NOW()+duration
→ AuctionSession.start() → notifyAuctionStarted() → scheduleAutoClose()
```

### 8. Đặt giá (Place Bid)
```
Bidder đặt giá → NetworkService.placeBid(auctionId, amount)
→ Message(Type.PLACE_BID) → Server:
  → AuctionDAO.placeBid() → INSERT INTO bids
  → UPDATE auction_sessions SET current_price=?, highest_bidder_id=?
  → AuctionSession.placeBid() → validate → notifyBidPlaced()
```

### 9. Kết thúc đấu giá (Auto/Manual)
```
Hết thời gian: scheduleAutoClose() → AuctionSession.finish()
→ UPDATE status='FINISHED', winner_id=highest_bidder_id
→ notifyAuctionFinished(winnerId, finalPrice)

Hoặc Manual: NetworkService.finishAuction() → Server finishAuction()
```

---

## Sơ Đồ Lớp (Class Diagram)

### Tổng Quan Client-Server
```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT-SERVER AUCTION SYSTEM                     │
└─────────────────────────────────────────────────────────────────────────────┘

┌───────────────────┐           ┌──────────────────────┐
│     Launch        │──────────>│      LoginApp        │
│   (main entry)    │  invokes  │   extends Application│
└───────────────────┘           └──────────┬───────────┘
                                          │ uses
                                          ▼
                        ┌─────────────────────────────────┐
                        │    NetworkService (Singleton)   │
                        │ ───────────────────────────── │
                        │ - socket: Socket               │
                        │ - output: ObjectOutputStream   │
                        │ - input: ObjectInputStream     │
                        │ - currentUser: User            │
                        │ ───────────────────────────── │
                        │ + connect(): boolean           │
                        │ + disconnect()                 │
                        │ + sendMessage(msg): Message    │
                        │ + login(u,p): Message          │
                        │ + register(u,p): Message       │
                        │ + getAuctions(): Message       │
                        │ + placeBid(auctionId,amt):Msg  │
                        │ + createItem(item): Message    │
                        └─────────────────────────────────┘
                                          │ sends/receives
                                          ▼
                        ┌─────────────────────────────────┐
                        │         Message (Serializable)  │
                        │ ───────────────────────────── │
                        │ + Type: enum {LOGIN,REGISTER,  │
                        │   GET_AUCTIONS, CREATE_AUCTION,│
                        │   PLACE_BID, SUCCESS, ERROR...}│
                        │ - type: Type                   │
                        │ - senderId: String             │
                        │ - auctionId: String            │
                        │ - itemId: String               │
                        │ - content: String              │
                        │ - data: Object                 │
                        │ - timestamp: long              │
                        └─────────────────────────────────┘
```

### Server Side
```text
═══════════════════════════════════════════════════════════════════════════════

                        SERVER SIDE
┌────────────────────────────────────────────────────────────────────────────┐
│                         AuctionServer                                     │
│ ──────────────────────────────────────────────────────────────────────────│
│ - serverSocket: ServerSocket                                             │
│ - port: int                                                              │
│ - userDAO: UserDAO                                                       │
│ - itemDAO: ItemDAO                                                       │
│ - auctionDAO: AuctionDAO                                                 │
│ ──────────────────────────────────────────────────────────────────────────│
│ + start()                                                                 │
│ + stop()                                                                  │
│                                                                           │
│   Inner Class: ClientHandler extends Thread                              │
│   ───────────────────────────────────────────────────────────────────────│
│   - socket: Socket                                                       │
│   - currentUser: User                                                    │
│   + run()                                                                │
│   - processMessage(msg): Message                                         │
│     ├─ handleLogin() → UserDAO.authenticate()                            │
│     ├─ handleRegister() → UserFactory + UserDAO.register()               │
│     ├─ handleCreateItem() → ItemFactory + ItemDAO.save()                 │
│     ├─ handleCreateAuction() → AuctionSession + AuctionDAO               │
│     ├─ handlePlaceBid() → AuctionDAO.placeBid()                          │
│     └─ handleGetAuctions() → AuctionDAO.findAllAuctions()                │
└────────────────────────────────────────────────────────────────────────────┘
```

### Model Layer
```text
═══════════════════════════════════════════════════════════════════════════════

MODEL LAYER (Entity Classes)
┌─────────────────────────────────────────────────────────────────────────────┐
│                         User (Serializable)                                │
│ ──────────────────────────────────────────────────────────────────────────│
│ - id: String                    # Base class for all users                │
│ - username: String                                                       │
│ - password: String                                                       │
│ - email: String                                                          │
│ - isSeller: boolean                                                      │
│ - isBidder: boolean                                                      │
│ - balance: BigDecimal                                                    │
│ ──────────────────────────────────────────────────────────────────────────│
│ + getRole(): String          + isSeller(): boolean                       │
│ + isBidder(): boolean         + getBalance(): BigDecimal                 │
└──────────────┬──────────────────────────────────┬─────────────────────────┘
               │ extends                           │ extends
               ▼                                  ▼
┌──────────────────────────┐        ┌──────────────────────────┐
│       Bidder             │        │        Seller            │
│  (isBidder=true)         │        │   (isSeller=true)        │
│  + getRole(): "BIDDER"   │        │   + getRole(): "SELLER"  │
│  + addBalance(amount)     │        │   - storeName: String    │
└──────────────────────────┘        └──────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                      Item (abstract, Serializable)                          │
│ ──────────────────────────────────────────────────────────────────────────│
│ - id: String                     # Abstract base for auction items         │
│ - name: String                                                             │
│ - description: String                                                      │
│ - startPrice: double                                                       │
│ - sellerId: String                                                         │
│ - category: String (protected)                                             │
│ - imagePath: String                                                        │
│ ──────────────────────────────────────────────────────────────────────────│
│ + getSpecificInfo(): String (abstract)                                     │
└──────────────┬──────────────────────────────────┬──────────────────┬────────┘
               │ extends                           │ extends        │ extends
               ▼                                  ▼                ▼
┌──────────────────────┐  ┌──────────────────────┐  ┌─────────────────────────┐
│   Electronics        │  │       Art             │  │      Vehicle            │
│ - brand: String      │  │ - artist: String     │  │ - brand: String        │
│ - warrantyMonths: int│  │ - yearCreated: int   │  │ - model: String        │
│ - model: String      │  │ - medium: String    │  │ - year: int            │
│ - condition: String  │  │ - style: String     │  │ - mileage: int         │
└──────────────────────┘  └──────────────────────┘  │ - fuelType: String     │
                                                    │ - transmission: String │
                                                    │ - color: String        │
                                                    │ - condition: String    │
                                                    └─────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                   AuctionSession (Serializable)                            │
│ ──────────────────────────────────────────────────────────────────────────│
│ - id: String                     # Represents an auction session          │
│ - item: Item                                                                │
│ - sellerId: String                                                          │
│ - status: Status (OPEN/RUNNING/FINISHED/PAID/CANCELED)                     │
│ - currentPrice: double                                                      │
│ - startPrice: double                                                        │
│ - highestBidderId: String                                                   │
│ - winnerId: String                                                          │
│ - startTime: LocalDateTime                                                  │
│ - endTime: LocalDateTime                                                    │
│ - durationMinutes: long                                                     │
│ - bidHistory: List<Bid>                                                     │
│ - observers: List<AuctionObserver> (transient)                              │
│ - scheduler: ScheduledExecutorService (transient)                           │
│ - minIncrement: double                                                      │
│ ──────────────────────────────────────────────────────────────────────────│
│ + start()                        + placeBid(bidderId, amount)              │
│ + finish()                      + cancel(reason)                           │
│ + processPayment(winnerId,amt):boolean                                     │
│ + addObserver(observer)           + removeObserver(observer)                │
│ - notifyBidPlaced()                - notifyAuctionStarted()                 │
│ - notifyAuctionFinished()          - notifyAuctionCanceled()               │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                        Bid (Serializable)                                   │
│ ──────────────────────────────────────────────────────────────────────────│
│ - id: String                      # Represents a single bid                │
│ - auctionId: String                                                         │
│ - bidderId: String                                                         │
│ - bidderUsername: String                                                   │
│ - amount: double                                                           │
│ - itemName: String                                                         │
│ - timestamp: LocalDateTime                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### DAO Layer
```text
═══════════════════════════════════════════════════════════════════════════════

DAO LAYER (Data Access Objects)
┌─────────────────────────────────────────────────────────────────────────────┐
│                       DatabaseUtil                                         │
│ ──────────────────────────────────────────────────────────────────────────│
│ - DB_URL: String = "jdbc:mysql://localhost:3306/auction_db"               │
│ - DB_USER: String = "root"                                                 │
│ - DB_PASSWORD: String = "123456789"                                         │
│ ──────────────────────────────────────────────────────────────────────────│
│ + getConnection(): Connection                                               │
│ + close(resources...)                                                       │
│ + closeAllConnections()                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
           │ uses
     ┌─────┴─────┬──────────────┬──────────────┐
     ▼            ▼              ▼              ▼
┌──────────┐ ┌───────────┐ ┌──────────────┐ ┌──────────────┐
│ UserDAO  │ │  ItemDAO  │ │ AuctionDAO   │ │AuctionSesDAO │
│──────────│ │───────────│ │──────────────│ │──────────────│
│+register()│ │+findAll() │ │+placeBid()   │ │+save()      │
│+login()   │ │+findById()│ │+startAuction│ │+findAll()   │
│+authentic.│ │+save()    │ │+finishAuct. │ │+findById()  │
│+findById()│ │           │ │+cancelAuct. │ │+getBidHist. │
│+getBalance│ │           │ │+findRunning()│ │+getUserBids│
│+updateBal.│ │           │ │+findOpen()   │ │              │
└──────────┘ └───────────┘ └──────────────┘ └──────────────┘
```

### Factory & Observer Pattern
```text
═══════════════════════════════════════════════════════════════════════════════

FACTORY PATTERN
┌──────────────────────────────┐    ┌──────────────────────────────┐
│       UserFactory           │    │       ItemFactory            │
│─────────────────────────────│    │─────────────────────────────│
│+isValidPassword(): boolean  │    │+createElectronics(): Item   │
│+getPasswordError(): String  │    │+createArt(): Item           │
│+createUser(): User          │    │+createVehicle(): Item        │
│  (id, username, password)   │    │+createItem(cat,...): Item   │
└──────────────────────────────┘    └──────────────────────────────┘

OBSERVER PATTERN
┌─────────────────────────────────────────────────────────────────────────────┐
│                    AuctionObserver (interface)                              │
│─────────────────────────────────────────────────────────────────────────────│
│ + onBidPlaced(auctionId, bidderId, amount)                                 │
│ + onAuctionStarted(auctionId)                                              │
│ + onAuctionFinished(auctionId, winnerId, finalPrice)                       │
│ + onAuctionCanceled(auctionId, reason)                                     │
│ + onAuctionStatusChanged(auctionId, oldStatus, newStatus)                  │
└─────────────────────────────────────────────────────────────────────────────┘
                                    △ (implemented by)
                                    │
                    ┌───────────────┴───────────────┐
                    │   (Concrete Observers in UI)   │
                    │   e.g., AuctionRoomController  │
                    └───────────────────────────────┘
```

### Exception Hierarchy
```text
═══════════════════════════════════════════════════════════════════════════════

EXCEPTION HIERARCHY
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Exception Package                                        │
│─────────────────────────────────────────────────────────────────────────────│
│ - AuthenticationException (username/password invalid)                      │
│ - InvalidBidException (bid amount invalid)                                 │
│ - AuctionClosedException (auction not running)                             │
│ - UnauthorizedException (no permission)                                    │
│ - ItemNotFoundException (item not found)                                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Controller Layer
```text
═══════════════════════════════════════════════════════════════════════════════

CONTROLLER LAYER (JavaFX MVC)
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Controller Package                                     │
│─────────────────────────────────────────────────────────────────────────────│
│ - logincontroller       → Xử lý đăng nhập/đăng ký                         │
│ - UserController        → Quản lý user info                                │
│ - CreateItemsController → Tạo item mới (Seller)                            │
│ - AuctionCardController → Hiển thị card phiên đấu giá                      │
│ - AuctionRoomController → Phòng đấu giá chi tiết (Observer)                │
│ - BidCardController     → Hiển thị thông tin bid                           │
│ - BidHistoryCardController → Lịch sử bid                                  │
│ - ItemCardController    → Hiển thị thông tin item                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Tóm Tắt Kiến Trúc

| Thành phần | Công nghệ | Mô tả |
|-----------|----------|-------|
| **Frontend** | JavaFX 21 | Login, Auction Room, Bid History UI |
| **Backend** | Java Socket | Server multi-threaded, Client handler |
| **Giao tiếp** | Object Stream | Message object serialization |
| **Database** | MySQL 8.4 | users, items, auction_sessions, bids |
| **Pattern** | DAO, Factory, Observer, Singleton | Design patterns |
| **Build** | Maven | Dependency management |
