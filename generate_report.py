#!/usr/bin/env python3
"""Generate academic PDF report — Times New Roman, 13pt body, ≤6 A4 pages."""

from fpdf import FPDF
import os

FONT_DIR = 'C:/Windows/Fonts'

class Report(FPDF):
    def __init__(self):
        super().__init__('P', 'mm', 'A4')
        self.set_auto_page_break(True, 14)
        self.set_margin(18)

        # Register Times New Roman
        self.add_font('TNR', '',  os.path.join(FONT_DIR, 'times.ttf'))
        self.add_font('TNR', 'B', os.path.join(FONT_DIR, 'timesbd.ttf'))
        self.add_font('TNR', 'I', os.path.join(FONT_DIR, 'timesi.ttf'))
        self.add_font('TNR', 'BI', os.path.join(FONT_DIR, 'timesbi.ttf'))

    def header(self):
        if self.page_no() <= 1:
            return
        self.set_font('TNR', 'I', 9)
        self.set_text_color(100, 100, 100)
        self.cell(0, 5, 'Hệ thống Đấu Giá Trực Tuyến — Báo cáo Bài tập Lớn', align='L')
        self.ln(1)
        self.set_draw_color(180, 180, 180)
        self.line(self.l_margin, self.get_y(), self.w - self.r_margin, self.get_y())
        self.ln(4)

    def footer(self):
        if self.page_no() <= 1:
            return
        self.set_y(-13)
        self.set_font('TNR', 'I', 9)
        self.set_text_color(130, 130, 130)
        self.cell(0, 8, str(self.page_no()), align='C')

    # ── Text helpers ─────────────────────────────────────────

    def h1(self, title):
        """PHẦN X: TÊN CHƯƠNG — 14pt Bold ALL CAPS"""
        self.set_font('TNR', 'B', 14)
        self.set_text_color(0, 0, 0)
        self.cell(0, 7, title.upper())
        self.ln(9)

    def h2(self, title):
        """Tiêu đề mục lớn — 13pt Bold"""
        self.set_font('TNR', 'B', 13)
        self.set_text_color(30, 30, 30)
        self.cell(0, 6.5, title)
        self.ln(7)

    def h3(self, title):
        """Tiêu đề mục nhỏ — 13pt Bold Italic"""
        self.set_font('TNR', 'BI', 13)
        self.set_text_color(50, 50, 50)
        self.cell(0, 6.5, title)
        self.ln(7)

    def body(self, text):
        """Body text — 13pt, line_h=5.5mm"""
        self.set_font('TNR', '', 13)
        self.set_text_color(0, 0, 0)
        self.multi_cell(self.w - self.l_margin - self.r_margin, 5.3, text, align='J')
        self.ln(1.2)

    def bullet(self, text, indent=6):
        self.set_x(self.l_margin + indent)
        self.set_font('TNR', '', 13)
        self.set_text_color(0, 0, 0)
        w = self.w - self.r_margin - self.get_x()
        self.multi_cell(w, 5.3, f'− {text}', align='J')
        self.ln(0.5)

    def caption(self, text):
        """Caption / footnote — 10pt Italic"""
        self.set_font('TNR', 'I', 10)
        self.set_text_color(80, 80, 80)
        self.multi_cell(self.w - self.l_margin - self.r_margin, 4.2, text, align='C')
        self.ln(2)

    # ── Architecture Diagram ─────────────────────────────────

    def draw_arch_diagram(self):
        """Compact architecture diagram with Times New Roman labels."""
        if self.get_y() > 180:
            self.add_page()
        y0 = self.get_y()
        xm = self.l_margin
        w = self.w - xm - self.r_margin

        # Client box
        cy = y0
        ch = 26
        self.set_fill_color(230, 240, 250)
        self.set_draw_color(70, 130, 180)
        self.rect(xm, cy, w, ch, style='DF')
        self.set_font('TNR', 'B', 10)
        self.set_text_color(70, 130, 180)
        self.set_xy(xm, cy + 1)
        self.cell(w, 5, 'CLIENT (JavaFX)', align='C')

        bw = (w - 24) / 4
        for i, (label, desc) in enumerate([
            ('LoginApp', 'Entry point'),
            ('16 Controllers', 'Xử lý UI'),
            ('18 FXML Views', 'Scene Builder'),
            ('NetworkService', 'Socket Singleton'),
        ]):
            bx = xm + 4 + i * (bw + 2)
            self.set_fill_color(200, 220, 240)
            self.set_draw_color(100, 150, 200)
            self.rect(bx, cy + 7, bw, 8, style='DF')
            self.set_font('TNR', 'B', 7)
            self.set_text_color(50, 80, 120)
            self.set_xy(bx + 1, cy + 8)
            self.cell(bw - 2, 3.5, label, align='C')
            self.set_font('TNR', 'I', 6.5)
            self.set_text_color(80, 100, 130)
            self.set_xy(bx + 1, cy + 11.5)
            self.cell(bw - 2, 3, desc, align='C')

        # Arrow
        ay = cy + ch
        self.set_draw_color(100, 100, 100)
        self.set_line_width(0.5)
        self.line(xm + w/2, ay, xm + w/2, ay + 5)
        self.line(xm + w/2 - 1.5, ay + 3.5, xm + w/2, ay + 5)
        self.line(xm + w/2 + 1.5, ay + 3.5, xm + w/2, ay + 5)
        self.set_font('TNR', 'I', 8)
        self.set_text_color(100, 100, 100)
        self.set_xy(xm + w/2 + 3, ay - 1)
        self.cell(50, 4, 'TCP Socket (port 8989)')
        self.set_line_width(0.2)

        # Server box
        sy = ay + 6
        sh = 44
        self.set_fill_color(235, 250, 240)
        self.set_draw_color(60, 150, 110)
        self.rect(xm, sy, w, sh, style='DF')
        self.set_font('TNR', 'B', 10)
        self.set_text_color(60, 150, 110)
        self.set_xy(xm, sy + 1)
        self.cell(w, 5, 'SERVER (TCP Multi-threaded)', align='C')

        # Row 1
        sw = (w - 20) / 2
        for i, (label, desc) in enumerate([
            ('AuctionServer', 'accept() & spawn ClientHandler'),
            ('ClientHandler × N', 'Mỗi client 1 thread riêng biệt'),
        ]):
            sx = xm + 4 + i * (sw + 3)
            self.set_fill_color(210, 235, 215)
            self.set_draw_color(90, 170, 130)
            self.rect(sx, sy + 7, sw, 7, style='DF')
            self.set_font('TNR', 'B', 6.5)
            self.set_text_color(40, 100, 70)
            self.set_xy(sx + 1, sy + 8)
            self.cell(sw - 2, 3, label, align='C')
            self.set_font('TNR', 'I', 6)
            self.set_text_color(60, 110, 85)
            self.set_xy(sx + 1, sy + 11)
            self.cell(sw - 2, 2.5, desc, align='C')

        # Row 2
        sw3 = (w - 26) / 3
        for i, (label, desc) in enumerate([
            ('AuctionDAO', 'placeBid() sync + TX'),
            ('7 DAO lớp', 'User, Item, Bid, Chat,...'),
            ('AutoBidEngine', 'Second-Price Vickrey'),
        ]):
            sx = xm + 4 + i * (sw3 + 3)
            self.set_fill_color(210, 235, 215)
            self.set_draw_color(90, 170, 130)
            self.rect(sx, sy + 15, sw3, 7, style='DF')
            self.set_font('TNR', 'B', 6.5)
            self.set_text_color(40, 100, 70)
            self.set_xy(sx + 1, sy + 16)
            self.cell(sw3 - 2, 3, label, align='C')
            self.set_font('TNR', 'I', 6)
            self.set_text_color(60, 110, 85)
            self.set_xy(sx + 1, sy + 19)
            self.cell(sw3 - 2, 2.5, desc, align='C')

        # Row 3 — Schedulers
        for i, (label, desc) in enumerate([
            ('Penalty Timer (30s)', 'Phạt trễ hạn 50.000đ'),
            ('Watchlist Timer (10s)', 'Nhắc ≤5 phút hết hạn'),
        ]):
            sx = xm + 4 + i * (sw + 3)
            self.set_fill_color(210, 235, 215)
            self.set_draw_color(90, 170, 130)
            self.rect(sx, sy + 23, sw, 7, style='DF')
            self.set_font('TNR', 'B', 6.5)
            self.set_text_color(40, 100, 70)
            self.set_xy(sx + 1, sy + 24)
            self.cell(sw - 2, 3, label, align='C')
            self.set_font('TNR', 'I', 6)
            self.set_text_color(60, 110, 85)
            self.set_xy(sx + 1, sy + 27)
            self.cell(sw - 2, 2.5, desc, align='C')

        # Row 4 — Infrastructure
        for i, (label, desc) in enumerate([
            ('HikariCP Pool (max 10)', 'Connection pooling'),
            ('Message / MessageFactory', 'Giao thức đóng gói'),
        ]):
            sx = xm + 4 + i * (sw + 3)
            self.set_fill_color(210, 235, 215)
            self.set_draw_color(90, 170, 130)
            self.rect(sx, sy + 31, sw, 7, style='DF')
            self.set_font('TNR', 'B', 6.5)
            self.set_text_color(40, 100, 70)
            self.set_xy(sx + 1, sy + 32)
            self.cell(sw - 2, 3, label, align='C')
            self.set_font('TNR', 'I', 6)
            self.set_text_color(60, 110, 85)
            self.set_xy(sx + 1, sy + 35)
            self.cell(sw - 2, 2.5, desc, align='C')

        # Arrow to DB
        dy = sy + sh
        self.set_draw_color(100, 100, 100)
        self.set_line_width(0.5)
        self.line(xm + w/2, dy, xm + w/2, dy + 5)
        self.line(xm + w/2 - 1.5, dy + 3.5, xm + w/2, dy + 5)
        self.line(xm + w/2 + 1.5, dy + 3.5, xm + w/2, dy + 5)
        self.set_font('TNR', 'I', 8)
        self.set_text_color(100, 100, 100)
        self.set_xy(xm + w/2 + 3, dy - 1)
        self.cell(30, 4, 'JDBC')
        self.set_line_width(0.2)

        # DB box
        dby = dy + 6
        self.set_fill_color(245, 240, 255)
        self.set_draw_color(120, 100, 180)
        self.rect(xm, dby, w, 8, style='DF')
        self.set_font('TNR', 'B', 9)
        self.set_text_color(120, 100, 180)
        self.set_xy(xm, dby + 1.5)
        self.cell(w, 5, 'MySQL 8+ — auction_db (6 bảng: users, items, auction_sessions, bids, chat_messages, watchlist)', align='C')

        self.set_y(dby + 11)

    # ── Feature block ────────────────────────────────────────

    def feature_block(self, num, name, solution, rationale):
        if self.get_y() > 240:
            self.add_page()
        y0 = self.get_y()

        # Number badge + name
        self.set_fill_color(50, 50, 50)
        self.set_text_color(255, 255, 255)
        self.set_font('TNR', 'B', 10)
        self.set_xy(self.l_margin, y0)
        self.cell(8, 6, str(num), align='C', fill=True)

        self.set_text_color(0, 0, 0)
        self.set_font('TNR', 'B', 13)
        self.set_xy(self.l_margin + 11, y0)
        self.cell(self.w - self.l_margin - self.r_margin - 11, 6, name)

        # Solution — 12pt
        self.set_font('TNR', '', 12)
        self.set_text_color(40, 40, 40)
        self.set_xy(self.l_margin, y0 + 7)
        self.multi_cell(self.w - self.l_margin - self.r_margin, 4.8, '    ' + solution, align='J')

        # Rationale — 11pt Italic
        self.set_font('TNR', 'I', 11)
        self.set_text_color(80, 80, 80)
        self.set_xy(self.l_margin, self.get_y())
        self.multi_cell(self.w - self.l_margin - self.r_margin, 4.3, '    → ' + rationale, align='J')

        # Thin separator
        end_y = self.get_y()
        self.set_draw_color(200, 200, 200)
        self.set_line_width(0.3)
        self.line(self.l_margin, end_y + 2, self.w - self.r_margin, end_y + 2)
        self.set_line_width(0.2)
        self.set_y(end_y + 5)

    # ── Work division table ──────────────────────────────────

    def draw_work_table(self):
        usable_w = self.w - self.l_margin - self.r_margin
        cw = [8, 38, 20, usable_w - 66]
        headers = ['STT', 'Thành viên', 'MSSV', 'Công việc đảm nhiệm']

        self.set_fill_color(50, 50, 50)
        self.set_text_color(255, 255, 255)
        self.set_font('TNR', 'B', 10)
        for i, h in enumerate(headers):
            self.cell(cw[i], 7, h, border=1, align='C', fill=True)
        self.ln()

        rows = [
            ('1', 'Đỗ Hải Đăng', '25020113',
             'Thiết kế Model (Entity, User, Item + 9 subclass), cài đặt Factory Pattern (ItemFactory, UserFactory), viết 115 Unit Test (JUnit 5, JaCoCo).'),
            ('2', 'Nguyễn Vũ Sơn', '25020352',
             'Phát triển TCP Server (AuctionServer, ClientHandler), giao thức Message/MessageFactory, đồng bộ đa luồng, thiết kế MySQL schema (6 bảng).'),
            ('3', 'Lê Quang Nghĩa', '25020292',
             'Thiết kế giao diện JavaFX (18 FXML + CSS), 16 Controller phía Client, xử lý tương tác UI bất đồng bộ (Platform.runLater).'),
            ('4', 'Nguyễn Duy Quang', '25020330',
             'placeBid() đồng bộ, AutoBidEngine (Second-Price), Sniper Protection (gia hạn 2 phút), Scheduler (Penalty + Watchlist Timers).'),
        ]

        for row in rows:
            if self.get_y() > 260:
                self.add_page()
                self.set_fill_color(50, 50, 50)
                self.set_text_color(255, 255, 255)
                self.set_font('TNR', 'B', 10)
                for i, h in enumerate(headers):
                    self.cell(cw[i], 7, h, border=1, align='C', fill=True)
                self.ln()

            self.set_text_color(0, 0, 0)
            self.set_font('TNR', 'B', 11)
            self.cell(cw[0], 5.5, row[0], border='LR', align='C')
            self.cell(cw[1], 5.5, row[1], border='LR', align='L')
            self.set_font('TNR', '', 11)
            self.cell(cw[2], 5.5, row[2], border='LR', align='C')
            # Multi-cell for work description
            x_last = self.get_x()
            y_last = self.get_y()
            self.set_font('TNR', '', 11)
            self.multi_cell(cw[3], 5.0, row[3], border='LRB', align='J')
            # Bottom border across full width
            self.set_xy(self.l_margin, self.get_y())
            self.set_draw_color(50, 50, 50)
            self.line(self.l_margin, self.get_y(), self.w - self.r_margin, self.get_y())
            self.set_y(self.get_y() + 1)


# ═══════════════════════════════════════════════════════════════
#  BUILD REPORT
# ═══════════════════════════════════════════════════════════════

pdf = Report()

# ═══ PAGE 1: TITLE PAGE ══════════════════════════════════════
pdf.add_page()
pdf.ln(22)

pdf.set_font('TNR', 'B', 18)
pdf.set_text_color(0, 0, 0)
pdf.cell(0, 9, 'BÁO CÁO BÀI TẬP LỚN', align='C')
pdf.ln(16)

pdf.set_font('TNR', 'B', 16)
pdf.cell(0, 8, 'HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN', align='C')
pdf.ln(6)

pdf.set_font('TNR', 'I', 13)
pdf.set_text_color(80, 80, 80)
pdf.cell(0, 7, 'Online Auction System', align='C')
pdf.ln(16)

# Decorative line
pdf.set_draw_color(0, 0, 0)
pdf.set_line_width(0.5)
x_sep = 55
pdf.line(x_sep, pdf.get_y(), pdf.w - x_sep, pdf.get_y())
pdf.set_line_width(0.2)
pdf.ln(12)

# Course
pdf.set_font('TNR', 'B', 14)
pdf.set_text_color(0, 0, 0)
pdf.cell(0, 8, 'Môn học: Lập Trình Nâng Cao', align='C')
pdf.ln(16)

# Instructor
pdf.set_font('TNR', 'B', 13)
pdf.cell(0, 7, 'Giảng viên hướng dẫn: ThS. Nguyễn Thị Hồng', align='C')
pdf.ln(16)

# Members
pdf.set_font('TNR', 'B', 13)
pdf.cell(0, 7, 'Nhóm sinh viên thực hiện', align='C')
pdf.ln(10)

members = [
    ('Đỗ Hải Đăng',           '25020113'),
    ('Nguyễn Vũ Sơn',         '25020352'),
    ('Lê Quang Nghĩa',        '25020292'),
    ('Nguyễn Duy Quang',      '25020330'),
]

for name, mssv in members:
    pdf.set_font('TNR', '', 13)
    pdf.set_text_color(0, 0, 0)
    x_center = pdf.w / 2 - 35
    pdf.set_x(x_center)
    pdf.cell(70, 7, name, align='L')
    pdf.set_font('TNR', '', 13)
    pdf.set_text_color(60, 60, 60)
    pdf.cell(30, 7, mssv, align='L')
    pdf.ln(8)

pdf.ln(12)

# Tech bar
pdf.set_draw_color(0, 0, 0)
pdf.set_fill_color(250, 250, 250)
techs = 'Java 21  •  JavaFX 21  •  MySQL 8+  •  HikariCP 5.1  •  TCP Socket  •  JUnit 5 & JaCoCo  •  Maven 3.9+'
pdf.set_font('TNR', 'I', 11)
pdf.set_text_color(50, 50, 50)
x_box = 20
w_box = pdf.w - 40
pdf.set_x(x_box)
pdf.cell(w_box, 8, techs, border=1, align='C', fill=True)
pdf.ln(18)

pdf.set_font('TNR', '', 13)
pdf.set_text_color(0, 0, 0)
pdf.cell(0, 7, 'Hà Nội, tháng 5 năm 2026', align='C')

# ═══ PAGE 2: INTRODUCTION + SCOPE ════════════════════════════
pdf.add_page()
pdf.h1('PHẦN 1: GIỚI THIỆU MỤC TIÊU VÀ PHẠM VI THỰC HIỆN')

pdf.h2('1.1  Mục tiêu dự án')
pdf.body(
    'Đồ án "Hệ thống Đấu Giá Trực Tuyến" được xây dựng nhằm vận dụng toàn diện '
    'kiến thức môn Lập Trình Nâng Cao vào một sản phẩm phần mềm hoàn chỉnh. '
    'Các mục tiêu kỹ thuật cụ thể bao gồm:'
)
pdf.bullet('Lập trình hướng đối tượng (OOP): kế thừa Entity → User → Bidder/Seller/Admin và Entity → Item → 9 danh mục sản phẩm (Art, Books, Electronics, Fashion, Furniture, Jewelry, Music, Sports, Vehicle), đa hình qua getSpecificInfo(), đóng gói dữ liệu.')
pdf.bullet('Lập trình giao diện (JavaFX): 18 file FXML thiết kế bằng Scene Builder, tách biệt View–Controller; CSS tùy chỉnh giao diện responsive.')
pdf.bullet('Lập trình mạng (TCP Socket): kiến trúc Client–Server qua Object Stream, giao thức Message-driven đóng gói bởi MessageFactory, port 8989.')
pdf.bullet('Lập trình đa luồng: mỗi Client được cấp một ClientHandler thread riêng; placeBid() đồng bộ synchronized + Database Transaction atomic COMMIT/ROLLBACK.')
pdf.bullet('Quản trị cơ sở dữ liệu: MySQL 8+ schema 6 bảng chuẩn hóa, DAO Pattern 7 lớp, HikariCP connection pool (tối đa 10 connections).')
pdf.bullet('Đảm bảo chất lượng: 115 unit test (JUnit 5, JaCoCo ngưỡng ≥30%), GitHub Actions CI/CD, OWASP Dependency-Check quét lỗ hổng bảo mật.')

pdf.h2('1.2  Phạm vi hệ thống')
pdf.body('Hệ thống phục vụ ba vai trò người dùng với quyền hạn và chức năng riêng biệt:')
pdf.bullet('Admin (Quản trị viên): quản lý toàn bộ người dùng (khóa/mở khóa, xóa, lịch sử đấu giá), quản lý vật phẩm, giám sát hệ thống.')
pdf.bullet('Seller (Người bán): tạo vật phẩm (9 danh mục), tạo và khởi chạy phiên đấu giá, theo dõi tiến trình đấu giá, nhận thanh toán.')
pdf.bullet('Bidder (Người mua): tìm kiếm phiên đấu giá, đặt giá thủ công hoặc tự động (AutoBid), chat thời gian thực, quản lý watchlist, nạp tiền.')

pdf.body('Vòng đời phiên đấu giá gồm 6 trạng thái chuyển tiếp:')
pdf.bullet('OPEN → RUNNING (Seller khởi động) → PAYMENT_PENDING (hết giờ, có người thắng) → PAID (thanh toán hoàn tất).')
pdf.bullet('OPEN → RUNNING → FINISHED (kết thúc không có người đặt giá).  Bất kỳ trạng thái → CANCELED (Admin hoặc Seller hủy).')

pdf.body('Các tính năng thời gian thực nâng cao: AutoBid theo cơ chế Vickrey Second-Price, '
         'Sniper Protection tự động gia hạn 2 phút khi có giá mới trong 2 phút cuối, '
         'chat trực tiếp theo phòng đấu giá, watchlist tự động nhắc nhở khi phiên sắp kết thúc, '
         'và phạt tự động 50.000đ đối với người thắng không thanh toán trong 1 giờ.')

# ═══ PAGE 3: ARCHITECTURE ════════════════════════════════════
pdf.add_page()
pdf.h1('PHẦN 2: KIẾN TRÚC TỔNG THỂ CỦA HỆ THỐNG')

pdf.h2('2.1  Sơ đồ kiến trúc tổng thể')
pdf.draw_arch_diagram()
pdf.caption('Hình 1. Sơ đồ kiến trúc tổng thể hệ thống — Client (JavaFX) kết nối TCP Socket đến Server đa luồng, '
            'DAO layer truy xuất MySQL qua HikariCP connection pool.')

pdf.h2('2.2  Mô tả kiến trúc')

pdf.h3('Client Side — JavaFX + NetworkService')
pdf.body(
    'Phía Client tổ chức theo mô hình MVC: View (18 FXML) tách biệt với Controller (16 lớp) '
    'chỉ xử lý logic hiển thị. NetworkService là Singleton duy trì kết nối TCP Socket đến '
    'Server qua ObjectOutputStream/InputStream. Mọi phản hồi từ Server được chuyển lên '
    'JavaFX Application Thread bằng Platform.runLater() nhằm tránh nghẽn giao diện. '
    'Package Controller/utils (UIUtils, ResponseUtils, FormatUtils, CategoryMapper, '
    'AlertUtils, SearchCriteriaBuilder) cung cấp tiện ích dùng chung, giảm trùng lặp mã nguồn.'
)

pdf.h3('Server Side — TCP Multi-threaded Server')
pdf.body(
    'AuctionServer lắng nghe tại port 8989, mỗi Client kết nối được cấp một ClientHandler '
    '(kế thừa Thread) xử lý độc lập. Kiến trúc Message-driven: yêu cầu từ Client được đóng '
    'gói thành Message (type, sender, content), Server phân tích và điều phối qua MessageFactory. '
    'Các phương thức nghiệp vụ quan trọng (placeBid, finishAuction, processPayment) được bảo vệ '
    'bởi synchronized + Database Transaction atomic COMMIT/ROLLBACK, ngăn chặn race condition. '
    'Hai TimerTask nền: Penalty Timer (30s) phạt trễ hạn thanh toán, Watchlist Timer (10s) '
    'gửi nhắc nhở khi phiên sắp hết hạn.'
)

pdf.h3('Database & Data Access Layer — DAO Pattern')
pdf.body(
    'MySQL 8+ schema gồm 6 bảng: users, items, auction_sessions, bids, chat_messages, watchlist. '
    'HikariCP connection pool (max 10 connections) tối ưu hiệu năng. 7 lớp DAO (UserDAO, '
    'ItemDAO, AuctionDAO, AuctionSessionDAO, BidDAO, ChatDAO, WatchlistDAO) triển khai CRUD. '
    'DatabaseUtil (Singleton) quản lý pool dùng chung. Các Design Pattern bổ trợ: '
    'Factory (ItemFactory — 9 danh mục, UserFactory — 3 vai trò), '
    'Observer (AuctionObserver cập nhật trạng thái thời gian thực), '
    'Template Method (Entity.getSpecificInfo() trừu tượng).'
)

# ═══ PAGE 4-5: FEATURES ═════════════════════════════════════
pdf.add_page()
pdf.h1('PHẦN 3: CÁC CHỨC NĂNG ĐẠT ĐƯỢC THEO BAREM ĐIỂM')

pdf.body('Năm chức năng cốt lõi được trình bày kèm hướng giải quyết kỹ thuật và lý do lựa chọn:')
pdf.ln(2)

pdf.feature_block(1, 'Đăng nhập & Đăng ký bảo mật (PBKDF2 + Salt)',
    'Mật khẩu được hash bằng PBKDF2WithHmacSHA256 (65.536 vòng lặp, salt ngẫu nhiên 16 byte), '
    'lưu dạng hex 64 ký tự. Hệ thống tự động phát hiện và nâng cấp mật khẩu SHA-256 cũ lên '
    'PBKDF2 khi người dùng đăng nhập (cơ chế migrate tự động). REGISTER mặc định chỉ tạo '
    'RegularUser; Admin chỉ tạo qua seedDefaultAdmin() lúc khởi động Server — chặn leo thang '
    'đặc quyền. Xác thực 3 lớp: kiểm tra thông tin đăng nhập → trạng thái khóa → quyền truy cập.',
    'PBKDF2 với 65.536 vòng lặp khiến tấn công brute-force/rainbow table trở nên bất khả thi. '
    'Cơ chế migrate tự động đảm bảo tương thích ngược với dữ liệu cũ mà không cần can thiệp '
    'thủ công. Giới hạn REGISTER chỉ tạo user thường loại bỏ vector leo thang đặc quyền phổ biến '
    'trong các hệ thống web. Ba lớp xác thực độc lập triển khai nguyên lý defense in depth.')

pdf.feature_block(2, 'Đặt giá thời gian thực & Giải quyết Race Condition',
    'placeBid() được bảo vệ bởi synchronized trên AuctionDAO — chỉ 1 thread thực thi tại một '
    'thời điểm. Toàn bộ quy trình kiểm tra (trạng thái phiên, thời gian, số dư, giá hợp lệ, '
    'chặn seller tự đấu giá) và ghi dữ liệu (INSERT bid, UPDATE current_price, UPDATE '
    'highest_bidder) được bọc trong Transaction với COMMIT/ROLLBACK. Nếu bất kỳ bước nào '
    'thất bại, toàn bộ transaction rollback, đảm bảo dữ liệu nhất quán tuyệt đối.',
    'Trong môi trường đa luồng với hàng chục ClientHandler đồng thời, thiếu synchronized + '
    'transaction, hai người dùng đặt giá cùng mili-giây có thể cùng được ghi nhận thắng — '
    'gây sai lệch nghiêm trọng. Giải pháp synchronized tầng ứng dụng + transaction tầng DB '
    'tạo hàng rào kép, đảm bảo tính ACID của từng lượt đặt giá.')

pdf.feature_block(3, 'Tính năng Tự động trả giá — AutoBid (Vickrey Second-Price)',
    'Người dùng thiết lập maxAmount; AutoBidEngine trên Server tự động đặt giá khi có '
    'người khác trả cao hơn. Mỗi bước = currentPrice + minIncrement, không vượt maxAmount. '
    'Cơ chế Vickrey Second-Price: người thắng chỉ trả mức giá của người thua cao nhất + '
    'một bước giá, không phải toàn bộ maxAmount. Danh sách AutoBid được bảo vệ bởi '
    'synchronized(autoBidLock). Engine tự động dừng khi hết tiền hoặc đạt maxAmount.',
    'AutoBid giải phóng người dùng khỏi việc liên tục theo dõi. Cơ chế Second-Price (Vickrey) '
    'là tiêu chuẩn công nghiệp (eBay, Google Ads) — khuyến khích đặt giá đúng mức sẵn sàng '
    'chi trả thực, tối ưu cả trải nghiệm người dùng lẫn doanh thu cho người bán. '
    'Xử lý trên Server đảm bảo AutoBid hoạt động ngay cả khi Client offline.')

pdf.feature_block(4, 'Tính năng Chống bắn tỉa — Sniper Protection',
    'Khi có lượt đặt giá hợp lệ trong vòng 2 phút cuối (secondsRemaining > 0 && < 120), '
    'hệ thống tự động gia hạn endTime thêm 2 phút. Cơ chế kích hoạt trong placeBid(), '
    'broadcast thông báo tới tất cả Client trong phòng qua BROADCAST_CHAT. '
    'Không giới hạn số lần gia hạn — miễn còn đặt giá mới trong 2 phút cuối, phiên tiếp tục '
    'được kéo dài.',
    'Chống chiến thuật "sniping" (đặt giá giây cuối bằng bot khiến người thật không kịp '
    'phản ứng) — vấn đề nhức nhối trên các sàn đấu giá thực tế. Gia hạn tự động đảm bảo '
    'mọi người tham gia có cơ hội phản hồi công bằng, đồng thời tăng giá trị thu được '
    'cho người bán thông qua cạnh tranh lành mạnh.')

pdf.feature_block(5, 'Scheduler chạy nền — Phạt thanh toán & Thông báo Watchlist',
    'Hai TimerTask độc lập: (1) Penalty Timer (30s) quét PAYMENT_PENDING quá hạn 1 giờ, '
    'tự động phạt người thắng 50.000đ và hủy phiên; (2) Watchlist Timer (10s) duyệt '
    'watchlist toàn hệ thống, nếu phiên còn ≤5 phút sẽ kết thúc, broadcast nhắc nhở '
    'BROADCAST_WATCHLIST. Cả hai chạy hoàn toàn trên Server, không yêu cầu Client online.',
    'Xử lý tập trung trên Server đảm bảo công bằng và nhất quán — không phụ thuộc Client '
    'có đang mở ứng dụng hay không. Cơ chế timer chủ động loại bỏ polling từ Client (gây '
    'tải hệ thống và tiêu tốn băng thông), đảm bảo thời gian phản hồi nhanh, chính xác '
    'cho các sự kiện quan trọng.')

# ═══ PAGE 6: WORK DIVISION ══════════════════════════════════
pdf.add_page()
pdf.h1('PHẦN 4: PHÂN CHIA CÔNG VIỆC TRONG NHÓM')

pdf.body('Dự án hoàn thiện với 128 commit qua GitHub, công việc được phân chia theo module kỹ thuật:')
pdf.ln(1)

pdf.draw_work_table()
pdf.caption('Bảng 1. Phân công công việc chi tiết theo module kỹ thuật của từng thành viên.')

pdf.h2('4.1  Thống kê dự án')

# Statistics table
usable_w = pdf.w - pdf.l_margin - pdf.r_margin
scw = [32, 28, 28, 28, 28]
stats = [
    ('Module', 'Main (LOC)', 'Test (LOC)', 'Số file', 'Số test'),
    ('Model', '~1.800', '—', '22', '—'),
    ('Controller', '~3.500', '—', '23', '—'),
    ('DAO', '~1.600', '—', '8', '—'),
    ('Network', '~2.200', '—', '7', '—'),
    ('Factory/Service', '~800', '—', '6', '—'),
    ('Exception/Util', '~600', '—', '9', '—'),
    ('Unit Tests', '—', '~1.900', '7', '115'),
    ('Tổng cộng', '~12.000', '~1.900', '~80', '115'),
]

self = pdf
self.set_fill_color(50, 50, 50)
self.set_text_color(255, 255, 255)
self.set_font('TNR', 'B', 10)
for i, h in enumerate(stats[0]):
    self.cell(scw[i], 7, h, border=1, align='C', fill=True)
self.ln()

for row in stats[1:]:
    self.set_text_color(0, 0, 0)
    is_total = row[0] == 'Tổng cộng'
    self.set_font('TNR', 'B' if is_total else '', 10)
    for i, v in enumerate(row):
        self.cell(scw[i], 6, v, border=1, align='C')
    self.ln()

pdf.caption('Bảng 2. Thống kê quy mô dự án theo từng module — tổng ~12.000 LOC chính, 115 bài kiểm thử đơn vị.')

pdf.ln(2)
pdf.body(
    'Hệ thống được tích hợp CI/CD qua GitHub Actions: build Maven tự động, kiểm thử JUnit + '
    'JaCoCo (ngưỡng bao phủ dòng lệnh ≥30%, loại trừ Controller/Network do phụ thuộc giao diện), '
    'phân tích chất lượng mã nguồn Qodana, và quét lỗ hổng bảo mật OWASP Dependency-Check '
    '(ngưỡng CVSS ≥8). Các Design Pattern được áp dụng: Singleton (DatabaseUtil, NetworkService), '
    'Factory (ItemFactory 9 danh mục, UserFactory 3 vai trò), DAO (7 lớp), Observer '
    '(AuctionObserver), Template Method (Entity.getSpecificInfo()). Bảo mật được thiết kế '
    'theo defense in depth: 3 lớp kiểm soát truy cập (FXML visibility + Controller guard + '
    'Server isAdmin() check), PBKDF2 hash mật khẩu, PreparedStatement chống SQL injection, '
    'và seed admin duy nhất chặn leo thang đặc quyền.'
)

# ──── SAVE ───────────────────────────────────────────────────
output_path = 'C:/Users/Redmi/IdeaProjects/btl/BaoCao_HeThongDauGia_FINAL.pdf'
pdf.output(output_path)
print(f'PDF saved to: {output_path}')
print(f'Pages: {pdf.page_no()}')
