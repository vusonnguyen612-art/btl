package Controller.utils;

import Model.AuctionSession;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Locale;

public final class FormatUtils {

    private static final DecimalFormat MONEY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        MONEY_FORMAT = new DecimalFormat("#,###", symbols);
    }

    private FormatUtils() {}

    public static String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value);
    }

    public static String formatMoney(double value) {
        return MONEY_FORMAT.format(value);
    }

    public static String formatDuration(long minutes) {
        if (minutes >= 60) {
            long hours = minutes / 60;
            long mins = minutes % 60;
            if (mins > 0) {
                return hours + "h " + mins + "p";
            }
            return hours + "h";
        }
        return minutes + " phút";
    }

    public static String getTimeRemaining(AuctionSession auction) {
        if (auction.getEndTime() == null) {
            return formatDuration(auction.getDurationMinutes());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return "Đã kết thúc";
        }

        long totalSeconds = Duration.between(now, auction.getEndTime()).getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("Còn %02d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("Còn %02d:%02d", minutes, seconds);
        } else {
            return String.format("Còn %ds", seconds);
        }
    }

    public static String getRemainingTimeShort(AuctionSession auction) {
        if (auction.getEndTime() == null) {
            return formatDuration(auction.getDurationMinutes());
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return "Đã kết thúc";
        }

        long minutes = Duration.between(now, auction.getEndTime()).toMinutes();
        long seconds = Duration.between(now, auction.getEndTime()).getSeconds() % 60;

        if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public static String formatCountdown(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public static String formatCountdownWithPrefix(long totalSeconds, String suffix) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("Còn %02d:%02d:%02d %s", hours, minutes, seconds, suffix);
        } else {
            return String.format("Còn %02d:%02d %s", minutes, seconds, suffix);
        }
    }
}
