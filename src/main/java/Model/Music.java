package Model;

/** Vật phẩm danh mục âm nhạc (đĩa nhạc, nhạc cụ…). */
public class Music extends Item {
    private static final long serialVersionUID = 1L;
    private String artist;
    private String genre;
    private String format;
    private int releaseYear;
    private String label;

    public Music(String id, String name, String description, double startPrice, String sellerId) {
        super(id, name, description, startPrice, sellerId);
        this.category = "MUSIC";
        this.artist = "";
        this.genre = "";
        this.format = "";
        this.releaseYear = 0;
        this.label = "";
    }

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

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getGenre() {
        return genre;
    }

    public String getFormat() {
        return format;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String getSpecificInfo() {
        return String.format("Artist: %s, Genre: %s, Format: %s, Year: %d, Label: %s",
                artist, genre, format, releaseYear, label);
    }
}
