package Model;

/**
 * Lớp đại diện cho vật phẩm danh mục sách.
 * Kế thừa từ {@link Item}, tự động gán category = "BOOKS".
 * Các trường đặc thù: author, publisher, genre, pageCount, isbn.
 */
public class Books extends Item {
    private static final long serialVersionUID = 1L;
    private String author;
    private String publisher;
    private String genre;
    private int pageCount;
    private String isbn;

    /**
     * Khởi tạo vật phẩm sách với giá trị mặc định cho các trường đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     */
    public Books(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "BOOKS";
        this.author = "";
        this.publisher = "";
        this.genre = "";
        this.pageCount = 0;
        this.isbn = "";
    }

    /**
     * Khởi tạo vật phẩm sách với đầy đủ thông tin đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     * @param author      tác giả
     * @param publisher   nhà xuất bản
     * @param genre       thể loại
     * @param pageCount   số trang
     * @param isbn        mã ISBN
     */
    public Books(String id, String name, String description, double startPrice,
                 String sellerId, String author, String publisher, String genre, int pageCount, String isbn) {
        super(id, name, description, startPrice, sellerId);
        this.category = "BOOKS";
        this.author = author;
        this.publisher = publisher;
        this.genre = genre;
        this.pageCount = pageCount;
        this.isbn = isbn;
    }

    /** @return tác giả */
    public String getAuthor() {
        return author;
    }

    /** @param author tác giả mới */
    public void setAuthor(String author) {
        this.author = author;
    }

    /** @return nhà xuất bản */
    public String getPublisher() {
        return publisher;
    }

    /** @return thể loại sách */
    public String getGenre() {
        return genre;
    }

    /** @return số trang */
    public int getPageCount() {
        return pageCount;
    }

    /** @return mã ISBN */
    public String getIsbn() {
        return isbn;
    }

    @Override
    /**
     * Trả về thông tin chi tiết đặc thù của vật phẩm sách.
     *
     * @return chuỗi gồm author, publisher, genre, pageCount, isbn
     */
    public String getSpecificInfo() {
        return String.format("Author: %s, Publisher: %s, Genre: %s, Pages: %d, ISBN: %s",
                author, publisher, genre, pageCount, isbn);
    }
}
