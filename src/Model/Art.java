package Model;

public class Art extends Item {
    private static final long serialVersionUID = 1L;
    private String artist;
    private int yearCreated;
    private String medium;
    private String style;
    private boolean isAuthenticated;

    public Art(String id, String name, String description, double startPrice,
               String sellerId, String artist, int yearCreated, String medium, String style) {
        super(id, name, description, startPrice, sellerId);
        this.category = "Art";
        this.artist = artist;
        this.yearCreated = yearCreated;
        this.medium = medium;
        this.style = style;
        this.isAuthenticated = false;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    public String getMedium() {
        return medium;
    }

    public String getStyle() {
        return style;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    @Override
    public String getSpecificInfo() {
        return String.format("Artist: %s, Year: %d, Medium: %s, Style: %s",
                artist, yearCreated, medium, style);
    }
}
