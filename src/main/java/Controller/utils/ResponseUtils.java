package Controller.utils;

import Network.Message;

import java.math.BigDecimal;
import java.util.List;

public final class ResponseUtils {

    private ResponseUtils() {}

    @SuppressWarnings("unchecked")
    public static <T> List<T> extractList(Message response) {
        if (response != null && response.getType() == Message.Type.SUCCESS && response.getData() instanceof List) {
            return (List<T>) response.getData();
        }
        return List.of();
    }

    public static BigDecimal extractBalance(Message response) {
        if (response != null && response.getType() == Message.Type.SUCCESS && response.getData() != null) {
            return (BigDecimal) response.getData();
        }
        return null;
    }

    public static boolean isSuccess(Message response) {
        return response != null && response.getType() == Message.Type.SUCCESS;
    }
}
