package Controller.utils;

import Model.AuctionSession;

import java.util.ArrayList;
import java.util.List;

public final class CategoryMapper {

    private CategoryMapper() {}

    public static String toEnglish(String vietnamese) {
        if (vietnamese == null) return null;
        return switch (vietnamese) {
            case "Nghệ thuật" -> "ART";
            case "Điện tử" -> "ELECTRONICS";
            case "Xe cộ" -> "VEHICLE";
            case "Thời trang" -> "FASHION";
            case "Sách" -> "BOOKS";
            case "Thể thao" -> "SPORTS";
            case "Trang sức" -> "JEWELRY";
            case "Âm nhạc" -> "MUSIC";
            case "Nội thất" -> "FURNITURE";
            default -> vietnamese;
        };
    }

    public static String toVietnamese(String english) {
        if (english == null) return null;
        return switch (english.toUpperCase()) {
            case "ART" -> "Nghệ thuật";
            case "ELECTRONICS" -> "Điện tử";
            case "VEHICLE" -> "Xe cộ";
            case "FASHION" -> "Thời trang";
            case "BOOKS" -> "Sách";
            case "SPORTS" -> "Thể thao";
            case "JEWELRY" -> "Trang sức";
            case "MUSIC" -> "Âm nhạc";
            case "FURNITURE" -> "Nội thất";
            default -> english;
        };
    }

    public static List<AuctionSession.Status> mapStatus(String vietnameseStatus) {
        List<AuctionSession.Status> statuses = new ArrayList<>();
        if (vietnameseStatus == null) return statuses;

        switch (vietnameseStatus) {
            case "Đang diễn ra" -> statuses.add(AuctionSession.Status.RUNNING);
            case "Sắp diễn ra" -> statuses.add(AuctionSession.Status.OPEN);
            case "Chờ thanh toán" -> statuses.add(AuctionSession.Status.PAYMENT_PENDING);
            case "Đã kết thúc" -> {
                statuses.add(AuctionSession.Status.FINISHED);
                statuses.add(AuctionSession.Status.PAID);
            }
            case "Đã hủy" -> statuses.add(AuctionSession.Status.CANCELED);
        }
        return statuses;
    }

    public static String[] getAllVietnameseNames() {
        return new String[]{"Nghệ thuật", "Điện tử", "Xe cộ", "Thời trang", "Sách", "Thể thao", "Trang sức", "Âm nhạc", "Nội thất"};
    }
}
