# HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN

Hệ thống đấu giá trực tuyến Java với Client-Server architecture, áp dụng các Design Patterns (Singleton, Factory, Observer) và xử lý đồng thời (Concurrency).

## CẤU TRÚC PROJECT

```
btl/
├── src/
│   ├── Exception/           # Custom Exceptions
│   ├── Model/               # Data Models
│   ├── Factory/             # Factory Pattern
│   ├── Observer/            # Observer Pattern
│   ├── Service/             # Business Logic (Singleton)
│   └── Network/             # Socket Communication
├── test/                    # JUnit Tests
└── lib/                     # JUnit Library
```

## SƠ ĐỒ LỚP CHI TIẾT

### 1. EXCEPTION PACKAGE

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                    Exception Package                                               │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                    │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────┐ │
│  │ InvalidBidException     │  │ AuctionClosedException  │  │ AuthenticationException│
│  ├─────────────────────────┤  ├─────────────────────────┤  ├─────────────────────┤ │
│  │ - bidAmount: double     │  │ - auctionId: String    │  │ - username: String   │ │
│  │ - currentPrice: double │  └─────────────────────────┘  └─────────────────────┘ │
│  └─────────────────────────┘                                                         │
│                                                                                    │
│  ┌─────────────────────────┐  ┌─────────────────────────┐                           │
│  │ ItemNotFoundException   │  │ UnauthorizedException   │                           │
│  ├─────────────────────────┤  ├─────────────────────────┤                           │
│  │ - itemId: String       │  │ - userId: String       │                           │
│  └─────────────────────────┘  │ - action: String       │                           │
│                               └─────────────────────────┘                           │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### 2. MODEL PACKAGE - USER HIERARCHY

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                              USER HIERARCHY                                         │
└────────────────────────────────────────────────────────────────────────────────────┘

                         ┌───────────────────────────────────────┐
                         │     <<abstract>> User                 │
                         ├───────────────────────────────────────┤
                         │ # id: String                         │
                         │ # username: String                   │
                         │ # password: String                   │
                         │ # email: String                      │
                         ├───────────────────────────────────────┤
                         │ + getRole(): String {abstract}        │
                         │ + isAdmin(): boolean                 │
                         │ + isSeller(): boolean                │
                         │ + isBidder(): boolean                │
                         └───────────────────────────────────────┘
                                        ▲
                                        │ extends
              ┌─────────────────────────┼─────────────────────────┐
              │                         │                         │
              ▼                         ▼                         ▼
┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
│        Bidder           │  │        Seller          │  │         Admin           │
├─────────────────────────┤  ├─────────────────────────┤  ├─────────────────────────┤
│ - balance: double       │  │ - storeName: String    │  │ - adminLevel: String    │
├─────────────────────────┤  ├─────────────────────────┤  ├─────────────────────────┤
│ + getBalance()          │  │ + getStoreName()        │  │ + getAdminLevel()       │
│ + setBalance()         │  │ + setStoreName()        │  │ + setAdminLevel()       │
│ + addBalance()         │  │                         │  │                         │
│ + isBidder(): true     │  │ + isSeller(): true     │  │ + isAdmin(): true      │
│ + getRole(): "BIDDER"  │  │ + getRole(): "SELLER"  │  │ + getRole(): "ADMIN"   │
└─────────────────────────┘  └─────────────────────────┘  └─────────────────────────┘
```

### 3. MODEL PACKAGE - ITEM HIERARCHY

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                              ITEM HIERARCHY                                        │
└────────────────────────────────────────────────────────────────────────────────────┘

                         ┌───────────────────────────────────────┐
                         │     <<abstract>> Item                 │
                         ├───────────────────────────────────────┤
                         │ # id: String                         │
                         │ # name: String                       │
                         │ # description: String               │
                         │ # startPrice: double                 │
                         │ # sellerId: String                   │
                         │ # category: String                   │
                         │ - imagePath: String                  │
                         ├───────────────────────────────────────┤
                         │ + getSpecificInfo(): String {abstract}│
                         │ + getters/setters...                  │
                         └───────────────────────────────────────┘
                                        ▲
                                        │ extends
    ┌────────────────────────────────────┼────────────────────────────────────┐
    │                                    │                                    │
    ▼                                    ▼                                    ▼
┌────────────────────────────────┐ ┌────────────────────────────────┐ ┌────────────────────────────┐
│         Electronics            │ │            Art                  │ │          Vehicle            │
├────────────────────────────────┤ ├────────────────────────────────┤ ├────────────────────────────┤
│ - brand: String               │ │ - artist: String               │ │ - brand: String            │
│ - warrantyMonths: int        │ │ - yearCreated: int            │ │ - model: String            │
│ - model: String              │ │ - medium: String              │ │ - year: int                │
│ - condition: String          │ │ - style: String               │ │ - mileage: int             │
├────────────────────────────────┤ │ - isAuthenticated: boolean    │ │ - fuelType: String         │
│ + getSpecificInfo(): String   │ ├────────────────────────────────┤ │ - transmission: String      │
│   "Brand: Apple, Model: M3,  │ │ + getSpecificInfo(): String   │ │ - color: String            │
│   Warranty: 24 months,       │ │   "Artist: Van Gogh, Year:    │ │ - condition: String        │
│   Condition: New"            │ │   1889, Medium: Oil..."       │ ├────────────────────────────┤
└────────────────────────────────┘ └────────────────────────────────┘ │ + getSpecificInfo(): String  │
                                                                      └────────────────────────────┘
```

### 4. AUCTION SESSION - STATE DIAGRAM

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                         AUCTION SESSION & STATUS FLOW                               │
└────────────────────────────────────────────────────────────────────────────────────┘

                         ┌───────────────────────────────────────┐
                         │         AuctionSession                │
                         ├───────────────────────────────────────┤
                         │ FIELDS:                              │
                         │ - id: String                         │
                         │ - item: Item                          │
                         │ - sellerId: String                    │
                         │ - status: Status {enum}               │
                         │ - currentPrice: double                │
                         │ - startPrice: double                 │
                         │ - highestBidderId: String            │
                         │ - winnerId: String                   │
                         │ - startTime: LocalDateTime          │
                         │ - endTime: LocalDateTime             │
                         │ - durationMinutes: long              │
                         │ - bidHistory: List<Bid>              │
                         │ - minIncrement: double               │
                         │                                          │
                         │ TRANSIENT (NOT SERIALIZED):           │
                         │ - observers: List<AuctionObserver>   │
                         │ - scheduler: ScheduledExecutor       │
                         │ - autoCloseTask: ScheduledFuture     │
                         ├───────────────────────────────────────┤
                         │ METHODS:                             │
                         │ + placeBid(bidderId, amount)         │
                         │ + start()                            │
                         │ + finish()                            │
                         │ + cancel(reason)                     │
                         │ + processPayment(winnerId, amount)   │
                         │ + addObserver(observer)              │
                         │ + removeObserver(observer)            │
                         │ + isOpen/isRunning/isFinished...     │
                         └───────────────────────────────────────┘


┌────────────────────────────────────────────────────────────────────────────────────┐
│                              STATUS TRANSITIONS                                    │
└────────────────────────────────────────────────────────────────────────────────────┘

        ┌────────────────────────────────────────────────────────────────────────┐
        │                                                                   │
        │                              ┌─────┐                                  │
        │                              │OPEN │                                  │
        │                              └──┬──┘                                  │
        │                                 │                                     │
        │                                 │ start()                            │
        │                                 ▼                                     │
        │                         ┌─────────────┐                              │
        │                         │  RUNNING    │                              │
        │                         └──────┬──────┘                              │
        │                                │                                     │
        │              ┌────────────────┼────────────────┐                    │
        │              │                 │                │                    │
        │              │    Timer Expires│     cancel()   │                    │
        │              ▼                 ▼                ▼                    │
        │        ┌───────────┐    ┌──────────┐    ┌───────────┐              │
        │        │ FINISHED  │    │ FINISHED │    │ CANCELED  │              │
        │        └─────┬─────┘    └──────────┘    └───────────┘              │
        │              │                                                        │
        │              │ processPayment()                                      │
        │              ▼                                                        │
        │        ┌──────────┐                                                 │
        │        │   PAID   │                                                 │
        │        └──────────┘                                                 │
        │                                                                      │
        └──────────────────────────────────────────────────────────────────────┘


┌────────────────────────────────────────────────────────────────────────────────────┐
│                              BID CLASS                                            │
└────────────────────────────────────────────────────────────────────────────────────┘

    ┌───────────────────────────────────────┐
    │              Bid                       │
    ├───────────────────────────────────────┤
    │ - id: String                         │
    │ - auctionId: String                   │
    │ - bidderId: String                    │
    │ - amount: double                      │
    │ - timestamp: LocalDateTime           │
    └───────────────────────────────────────┘
```

### 5. DESIGN PATTERNS

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                                 DESIGN PATTERNS                                     │
└────────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  1. SINGLETON PATTERN                    │
│     AuctionManager                        │
├──────────────────────────────────────────┤
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ AuctionManager                      │  │
│  │ : <<Singleton>>                    │  │
│  ├────────────────────────────────────┤  │
│  │ - instance: AuctionManager {static}│  │
│  │ - users: Map<String, User>          │  │
│  │ - items: Map<String, Item>          │  │
│  │ - auctions: Map<String, AuctionSession>│
│  │ - globalObservers: List<Observer>  │  │
│  │ - lock: ReentrantReadWriteLock      │  │
│  ├────────────────────────────────────┤  │
│  │ + getInstance() {static, sync}    │──┼──► Returns singleton instance
│  │ + resetInstance() {static, sync}  │  │
│  │ + createAuction(itemId, duration)   │  │
│  │ + placeBid(auctionId, bidderId, $) │  │
│  │ + authenticate(username, password) │  │
│  │ + saveData() / loadData()         │  │
│  └────────────────────────────────────┘  │
│                                          │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  2. FACTORY METHOD PATTERN               │
│     ItemFactory & UserFactory             │
├──────────────────────────────────────────┤
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ ItemFactory                        │  │
│  ├────────────────────────────────────┤  │
│  │ + createElectronics(...) → Item   │  │
│  │ + createArt(...) → Item           │  │
│  │ + createVehicle(...) → Item        │  │
│  │ + createItem(category, ...) → Item │──┼──► Creates by category
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ UserFactory                        │  │
│  ├────────────────────────────────────┤  │
│  │ + createBidder(username, pass)     │  │
│  │ + createSeller(username, pass)     │  │
│  │ + createAdmin(username, pass)       │  │
│  │ + createUser(role, ...) → User     │──┼──► Creates by role
│  └────────────────────────────────────┘  │
│                                          │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  3. OBSERVER PATTERN                      │
│     AuctionObserver                       │
├──────────────────────────────────────────┤
│                                          │
│        ┌───────────────────┐             │
│        │ AuctionObserver   │◄────┐       │
│        │  <<interface>>    │     │       │
│        ├───────────────────┤     │       │
│        │+onBidPlaced()     │     │       │
│        │+onAuctionStarted()│     │       │
│        │+onAuctionFinished │     │       │
│        │+onAuctionCanceled │     │       │
│        │+onStatusChanged() │     │       │
│        └───────────────────┘     │       │
│               ▲                  │       │
│               │ implements        │       │
│        ┌──────┴──────┐          │       │
│        │             │          │       │
│        ▼             ▼          │       │
│  ┌───────────┐  ┌──────────┐    │       │
│  │ServerObser│  │UIControl │    │       │
│  │ (Prints)  │  │(Updates) │────┘       │
│  └───────────┘  └──────────┘            │
│                                          │
│  AuctionSession.notify*() → Observer     │
│                                          │
└──────────────────────────────────────────┘
```

### 6. NETWORK LAYER

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                                 NETWORK LAYER                                      │
└────────────────────────────────────────────────────────────────────────────────────┘

   ┌─────────────────────────────────┐        ┌─────────────────────────────────┐
   │         AuctionClient           │        │         AuctionServer          │
   ├─────────────────────────────────┤        ├─────────────────────────────────┤
   │ - Socket socket                 │◄──────►│ - ServerSocket serverSocket    │
   │ - ObjectOutputStream output    │        │ - AuctionManager manager       │
   │ - ObjectInputStream input      │        ├─────────────────────────────────┤
   ├─────────────────────────────────┤        │ + start()                      │
   │ + connect()                     │        │ + stop()                       │
   │ + disconnect()                  │        │                                 │
   │ + sendMessage(msg)             │        │ [ClientHandler Thread]         │
   │ + login/user/pass              │        │ + processMessage(msg)          │
   │ + register(role/user/pass)      │        │ + handleLogin()                │
   │ + getAuctions()                │        │ + handlePlaceBid()             │
   │ + getAuction(id)               │        │ + handleCreateAuction()        │
   │ + createAuction(itemId, mins)   │        │ + handleFinishAuction()        │
   │ + startAuction(id)             │        │ + ...                          │
   │ + placeBid(auctionId, amount) │        │                                 │
   │ + finishAuction(id)            │        │ [ServerAuctionObserver]        │
   │ + ...                          │        │ + onBidPlaced() → log         │
   └─────────────────────────────────┘        └─────────────────────────────────┘

   ┌────────────────────────────────────────────────────────────────────────────┐
   │                              Message (Serializable)                         │
   ├────────────────────────────────────────────────────────────────────────────┤
   │  Type Enum:                                                                 │
   │  LOGIN, LOGOUT, REGISTER, GET_AUCTIONS, GET_AUCTION, CREATE_AUCTION,      │
   │  START_AUCTION, PLACE_BID, FINISH_AUCTION, CANCEL_AUCTION,                │
   │  GET_ITEMS, CREATE_ITEM, UPDATE_ITEM, DELETE_ITEM, GET_USERS,               │
   │  NOTIFICATION, ERROR, SUCCESS                                               │
   ├────────────────────────────────────────────────────────────────────────────┤
   │  Fields:                                                                   │
   │  - type: Type                        - auctionId: String                   │
   │  - senderId: String                  - itemId: String                      │
   │  - content: String                   - data: Object                        │
   │  - timestamp: long                                                       │
   └────────────────────────────────────────────────────────────────────────────┘
```

## CÁCH HOẠT ĐỘNG CHI TIẾT

### 1. QUY TRÌNH ĐẤU GIÁ

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         VÒNG ĐỜI PHIÊN ĐẤU GIÁ                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

   SELLER                                    SYSTEM                           BIDDER
     │                                         │                                │
     │  1. Tạo Item (Electronics/Art/Vehicle)  │                                │
     │────────────────────────────────────────►│                                │
     │                                         │                                │
     │  2. Tạo AuctionSession (OPEN)          │                                │
     │────────────────────────────────────────►│                                │
     │                                         │                                │
     │  3. Start Auction (OPEN → RUNNING)     │                                │
     │────────────────────────────────────────►│                                │
     │                                         │                                │
     │                                         │◄─────────── 4. Place Bid ─────│
     │                                         │            (validate)          │
     │                                         │◄─────────── 5. Place Bid ─────│
     │                                         │            (validate)          │
     │                                         │◄─────────── 6. Place Bid ─────│
     │                                         │            (validate)          │
     │                                         │                                │
     │                                         │◄─── 7. Timer Expires ─────────│
     │                                         │     (AUTO: RUNNING → FINISHED)│
     │                                         │                                │
     │◄──────────── 8. Winner/Price ───────────│                                │
     │                                         │                                │
     │  9. Process Payment (FINISHED → PAID)  │                                │
     │────────────────────────────────────────►│                                │
     │                                         │                                │
     ▼                                         ▼                                ▼
```

### 2. XỬ LÝ ĐẶT GIÁ (Place Bid)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         PLACE BID SEQUENCE                                      │
└─────────────────────────────────────────────────────────────────────────────────┘

   Bidder                      AuctionSession                    AuctionObserver
      │                              │                                    │
      │ placeBid(bidderId, amount)   │                                    │
      │─────────────────────────────►│                                    │
      │                              │                                    │
      │                              │ 1. Kiểm tra status == RUNNING?     │
      │                              │    ❌ Không → AuctionClosedException│
      │                              │                                    │
      │                              │ 2. Kiểm tra thời gian?             │
      │                              │    ❌ Hết hạn → AuctionClosedException│
      │                              │                                    │
      │                              │ 3. Kiểm tra amount > currentPrice? │
      │                              │    ❌ Không → InvalidBidException  │
      │                              │                                    │
      │                              │ 4. Kiểm tra amount >= minIncrement │
      │                              │    ❌ Không → InvalidBidException  │
      │                              │                                    │
      │                              │ 5. ✅ Hợp lệ:                      │
      │                              │    - currentPrice = amount         │
      │                              │    - highestBidderId = bidderId    │
      │                              │    - Thêm vào bidHistory          │
      │                              │                                    │
      │                              │ 6. notifyBidPlaced()              │
      │                              │────────────────────────────────────►│
      │                              │                                    │
      │  ✅ "Bid placed successfully" │                                    │
      │◄─────────────────────────────│                                    │
      │                              │                                    │
      ▼                              ▼                                    ▼
```

### 3. CONCURRENCY (XỬ LÝ ĐỒNG THỜI)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    CONCURRENCY VỚI REENTRANTREADWRITELOCK                       │
└─────────────────────────────────────────────────────────────────────────────────┘

                    AuctionManager
                    ┌─────────────────────────────────────────┐
                    │  ReentrantReadWriteLock                 │
                    │  ┌───────────────────────────────────┐  │
                    │  │         LOCK HIERARCHY             │  │
                    │  │                                   │  │
                    │  │  WRITE LOCK (exclusive)           │  │
                    │  │  ┌─────────────────────────────┐  │  │
                    │  │  │ • placeBid()               │  │  │
                    │  │  │ • createAuction()          │  │  │
                    │  │  │ • startAuction()           │  │  │
                    │  │  │ • finishAuction()          │  │  │
                    │  │  │ • addUser()                │  │  │
                    │  │  │ • addItem()                │  │  │
                    │  │  │ • updateItem()             │  │  │
                    │  │  │ • deleteItem()            │  │  │
                    │  │  └─────────────────────────────┘  │  │
                    │  │              OR                  │  │
                    │  │  READ LOCK (shared)              │  │
                    │  │  ┌─────────────────────────────┐  │  │
                    │  │  │ • getAuction()             │  │  │
                    │  │  │ • getItem()               │  │  │
                    │  │  │ • getAllItems()           │  │  │
                    │  │  │ • getAllAuctions()        │  │  │
                    │  │  │ • authenticate()          │  │  │
                    │  │  └─────────────────────────────┘  │  │
                    │  └───────────────────────────────────┘  │
                    └─────────────────────────────────────────┘

    Thread A (placeBid)                    Thread B (placeBid)
         │                                      │
         │ writeLock.lock()                     │
         │◄────────────────────────────────────►│ ❌ BLOCKED
         │                                      │
         │ [Critical Section]                    │
         │ - Update currentPrice                 │
         │ - Add to bidHistory                  │
         │                                      │
         │ writeLock.unlock()                   │
         │──────────────────────────────────────►│ writeLock.lock()
         │                                      │ [Critical Section]
         │                                      │ ...
```

### 4. REALTIME NOTIFICATION (OBSERVER PATTERN)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    OBSERVER PATTERN - REALTIME                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

        ┌─────────────────┐
        │ AuctionSession  │
        │                 │
        │ List<Observer> │──────┐
        └─────────────────┘       │
               │                  │ notify
               │                  ▼
               │         ┌───────────────────┐
               │         │ AuctionObserver    │
               │         │  <<interface>>    │
               │         ├───────────────────┤
               │         │ + onBidPlaced()    │
               │         │ + onAuctionStarted │
               │         │ + onAuctionFinished│
               │         │ + onAuctionCanceled│
               │         └───────────────────┘
               │                  ▲
               │                  │ implements
               │         ┌────────┴────────┐
               │         │                 │
               │         ▼                 ▼
    ┌──────────┴───┐           ┌───────────┴──────┐
    │ServerObserver│           │  UI Controller  │
    │ (Print logs) │           │ (Update view)   │
    └──────────────┘           └──────────────────┘


    FLOW: Khi có bid mới
    ─────────────────────
    1. Bidder gọi placeBid()
    2. AuctionSession.placeBid() ✅ Thành công
    3. notifyBidPlaced() được gọi
    4. Duyệt observers:
       - ServerObserver: In log "New bid: 1100.0"
       - UI Controller: Cập nhật giao diện real-time
```

### 5. SOCKET COMMUNICATION

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    CLIENT-SERVER COMMUNICATION                                    │
└─────────────────────────────────────────────────────────────────────────────────┘

   CLIENT                                          SERVER
   ┌──────────┐                                   ┌──────────────────┐
   │ Auction  │                                   │ AuctionServer    │
   │ Client   │                                   │                  │
   └────┬─────┘                                   └────────┬─────────┘
        │                                                  │
        │  1. CONNECT (Socket)                            │
        │────────────────────────────────────────────────►│
        │                                                  │
        │  2. SEND Message (ObjectOutputStream)           │
        │  ┌────────────────────────────────────┐         │
        │  │ Message {                           │         │
        │  │   type: PLACE_BID,                  │         │
        │  │   auctionId: "AUC00001",            │         │
        │  │   data: 1500.0                       │         │
        │  │ }                                   │         │
        │  └────────────────────────────────────┘         │
        │────────────────────────────────────────────────►│
        │                                                  │
        │                          ┌──────────────────────┴────┐
        │                          │ ClientHandler.processMsg() │
        │                          │  • validate permissions    │
        │                          │  • call AuctionManager     │
        │                          │  • handle exceptions       │
        │                          └──────────────────────┬─────┘
        │                                                  │
        │  3. RECEIVE Response (ObjectInputStream)         │
        │  ┌────────────────────────────────────┐          │
        │  │ Message {                           │          │
        │  │   type: SUCCESS,                    │          │
        │  │   content: "Bid placed: 1500.0"    │          │
        │  │ }                                   │          │
        │  └────────────────────────────────────┘          │
        │◄─────────────────────────────────────────────────│
        │                                                  │
        │  4. DISCONNECT                                   │
        │────────────────────────────────────────────────►│
        ▼                                                  ▼
```

### 6. SERIALIZATION (LƯU TRỮ DỮ LIỆU)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PERSISTENCE VỚI SERIALIZATION                                │
└─────────────────────────────────────────────────────────────────────────────────┘

    AuctionManager (Singleton) ◄─────── Serializable
          │                                    │
          │                                    │ serialVersionUID = 1L
          │                                    │
          │    ┌───────────────────────────────┤
          │    │ FIELDS ĐƯỢC LƯU:            │
          │    │ • users (Map)                │
          │    │ • items (Map)                │
          │    │ • auctions (Map)             │
          │    │ • globalObservers (List)     │
          │    │ • auctionCounter (int)       │
          │    │                               │
          │    │ FIELDS KHÔNG LƯU:            │
          │    │ • lock (transient)           │
          │    │ • scheduler (transient)      │
          │    │ • observers (transient)       │
          │    │ • autoCloseTask (transient)  │
          │    └───────────────────────────────┘
          │                                    │
          │                                    ▼
          │                          ┌─────────────────────┐
          │                          │ auction_data.ser    │
          │                          │ (Binary File)       │
          │                          └─────────────────────┘

    saveData()                              loadData()
    ─────────                               ─────────
    1. acquire readLock                     1. read from file
    2. ObjectOutputStream                   2. ObjectInputStream
    3. writeObject(this)                    3. cast to AuctionManager
    4. release readLock                      4. assign to instance
```

## TỔNG KẾT CÁC THÀNH PHẦN

| Package | Class | Chức năng |
|---------|-------|-----------|
| **Exception** | InvalidBidException | Giá thầu không hợp lệ |
| | AuctionClosedException | Đấu giá đã đóng |
| | AuthenticationException | Lỗi đăng nhập |
| | ItemNotFoundException | Không tìm thấy sản phẩm |
| | UnauthorizedException | Không có quyền thực hiện |
| **Model** | User (abstract) | Lớp cha người dùng |
| | Bidder | Người đấu giá |
| | Seller | Người bán |
| | Admin | Quản trị viên |
| | Item (abstract) | Lớp cha sản phẩm |
| | Electronics | Sản phẩm điện tử |
| | Art | Sản phẩm nghệ thuật |
| | Vehicle | Phương tiện |
| | Bid | Thông tin đặt giá |
| | AuctionSession | Phiên đấu giá với trạng thái |
| **Factory** | ItemFactory | Tạo Item theo loại (Factory Method) |
| | UserFactory | Tạo User theo vai trò (Factory Method) |
| **Observer** | AuctionObserver | Interface observer (Observer Pattern) |
| **Service** | AuctionManager | Singleton quản lý toàn bộ hệ thống |
| **Network** | AuctionServer | Socket server xử lý client |
| | AuctionClient | Socket client kết nối server |
| | Message | Giao tiếp client-server (Serializable) |

## DESIGN PATTERNS ĐÃ ÁP DỤNG

| Pattern | Class | Mục đích |
|---------|-------|----------|
| **Singleton** | AuctionManager | Đảm bảo chỉ có 1 instance duy nhất |
| **Factory Method** | ItemFactory, UserFactory | Tạo đối tượng theo loại/categor |
| **Observer** | AuctionObserver | Thông báo realtime khi có bid mới |

## CHẠY PROJECT

### Compile
```bash
javac -d out -sourcepath src src/Exception/*.java src/Model/*.java src/Factory/*.java src/Observer/*.java src/Service/*.java src/Network/*.java
```

### Run Server
```bash
java -cp out Network.AuctionServer
```

### Run Client
```bash
java -cp out Network.AuctionClient
```

### Chạy Tests
```bash
# Compile tests
javac -cp "out;lib/junit-platform-console-standalone-1.10.0.jar" -d test/out test/*.java

# Run tests
java -cp "out;test/out;lib/junit-platform-console-standalone-1.10.0.jar" org.junit.platform.console.ConsoleLauncher --scan-class-path test/out
```

## CÁC TRẠNG THÁI PHIÊN ĐẤU GIÁ

```
OPEN ──────► RUNNING ──────► FINISHED ──────► PAID
  │              │               │
  │              │               │
  └──────────────┴──► CANCELED ◄──┘
```

- **OPEN**: Phiên đấu giá được tạo, chờ bắt đầu
- **RUNNING**: Phiên đấu giá đang diễn ra, nhận bid
- **FINISHED**: Hết thời gian hoặc bị kết thúc thủ công
- **PAID**: Người thắng đã thanh toán
- **CANCELED**: Phiên bị hủy bỏ
- cần bổ sung
- 