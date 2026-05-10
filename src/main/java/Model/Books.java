package Model;

/** Vật phẩm danh mục sách. */
public class Books extends Item {
    private static final long serialVersionUID = 1L;
    private String author;
    private String publisher;
    private String genre;
    private int pageCount;
    private String isbn;

    public Books(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "BOOKS";
        this.author = "";
        this.publisher = "";
        this.genre = "";
        this.pageCount = 0;
        this.isbn = "";
    }

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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getGenre() {
        return genre;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String getIsbn() {
        return isbn;
    }

    @Override
    public String getSpecificInfo() {
        return String.format("Author: %s, Publisher: %s, Genre: %s, Pages: %d, ISBN: %s",
                author, publisher, genre, pageCount, isbn);
    }
}
