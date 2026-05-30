package Model;

/**
 * Lớp đại diện cho vật phẩm danh mục nghệ thuật (tranh, điêu khắc…).
 * Kế thừa từ {@link Item}, tự động gán category = "ART".
 * Các trường đặc thù: artist, yearCreated, medium, style, isAuthenticated.
 */
public class Art extends Item {
    private static final long serialVersionUID = 1L;
    private String artist;
    private int yearCreated;
    private String medium;
    private String style;
    private boolean isAuthenticated;

    /**
     * Khởi tạo vật phẩm nghệ thuật với giá trị mặc định cho các trường đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     */
    public Art(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "ART";
        this.artist = "";
        this.yearCreated = 0;
        this.medium = "";
        this.style = "";
        this.isAuthenticated = false;
    }

    /**
     * Khởi tạo vật phẩm nghệ thuật với đầy đủ thông tin đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     * @param artist      tên tác giả/nghệ sĩ
     * @param yearCreated năm sáng tác
     * @param medium      chất liệu (sơn dầu, acrylic...)
     * @param style       phong cách nghệ thuật (hiện đại, cổ điển...)
     */
    public Art(String id, String name, String description, double startPrice,
               String sellerId, String artist, int yearCreated, String medium, String style) {
        super(id, name, description, startPrice, sellerId);
        this.category = "ART";
        this.artist = artist;
        this.yearCreated = yearCreated;
        this.medium = medium;
        this.style = style;
        this.isAuthenticated = false;
    }

    /** @return tên tác giả/nghệ sĩ */
    public String getArtist() {
        return artist;
    }

    /** @param artist tên tác giả mới */
    public void setArtist(String artist) {
        this.artist = artist;
    }

    /** @return năm sáng tác */
    public int getYearCreated() {
        return yearCreated;
    }

    /** @return chất liệu (sơn dầu, acrylic...) */
    public String getMedium() {
        return medium;
    }

    /** @return phong cách nghệ thuật */
    public String getStyle() {
        return style;
    }

    /** @return true nếu tác phẩm đã được giám định/chứng thực */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    /** @param authenticated trạng thái giám định mới */
    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    @Override
    /**
     * Trả về thông tin chi tiết đặc thù của vật phẩm nghệ thuật.
     *
     * @return chuỗi gồm artist, yearCreated, medium, style
     */
    public String getSpecificInfo() {
        return String.format("Artist: %s, Year: %d, Medium: %s, Style: %s",
                artist, yearCreated, medium, style);
    }
}
