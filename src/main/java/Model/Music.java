package Model;

/**
 * Lớp đại diện cho vật phẩm danh mục âm nhạc (đĩa nhạc, nhạc cụ…).
 * Kế thừa từ {@link Item}, tự động gán category = "MUSIC".
 * Các trường đặc thù: artist, genre, format, releaseYear, label.
 */
public class Music extends Item {
    private static final long serialVersionUID = 1L;
    private String artist;
    private String genre;
    private String format;
    private int releaseYear;
    private String label;

    /**
     * Khởi tạo vật phẩm âm nhạc với giá trị mặc định cho các trường đặc thù.
     *
     * @param id          mã vật phẩm
     * @param name        tên vật phẩm
     * @param description mô tả
     * @param startPrice  giá khởi điểm
     * @param sellerId    ID người bán
     */
    public Music(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "MUSIC";
        this.artist = "";
        this.genre = "";
        this.format = "";
        this.releaseYear = 0;
        this.label = "";
    }

    /**
     * Khởi tạo vật phẩm âm nhạc với đầy đủ thông tin đặc thù.
     *
     * @param id           mã vật phẩm
     * @param name         tên vật phẩm
     * @param description  mô tả
     * @param startPrice   giá khởi điểm
     * @param sellerId     ID người bán
     * @param artist       nghệ sĩ/nhóm nhạc
     * @param genre        thể loại nhạc
     * @param format       định dạng (CD, vinyl, digital...)
     * @param releaseYear  năm phát hành
     * @param label        hãng đĩa
     */
    public Music(String id, String name, String description, double startPrice,
                 String sellerId, String artist, String genre, String format, int releaseYear, String label) {
        super(id, name, description, startPrice, sellerId);
        this.category = "MUSIC";
        this.artist = artist;
        this.genre = genre;
        this.format = format;
        this.releaseYear = releaseYear;
        this.label = label;
    }

    /** @return nghệ sĩ/nhóm nhạc */
    public String getArtist() {
        return artist;
    }

    /** @param artist nghệ sĩ mới */
    public void setArtist(String artist) {
        this.artist = artist;
    }

    /** @return thể loại nhạc */
    public String getGenre() {
        return genre;
    }

    /** @return định dạng (CD, vinyl...) */
    public String getFormat() {
        return format;
    }

    /** @return năm phát hành */
    public int getReleaseYear() {
        return releaseYear;
    }

    /** @return hãng đĩa */
    public String getLabel() {
        return label;
    }

    @Override
    /**
     * Trả về thông tin chi tiết đặc thù của vật phẩm âm nhạc.
     *
     * @return chuỗi gồm artist, genre, format, releaseYear, label
     */
    public String getSpecificInfo() {
        return String.format("Artist: %s, Genre: %s, Format: %s, Year: %d, Label: %s",
                artist, genre, format, releaseYear, label);
    }
}
