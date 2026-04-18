package Factory;

import Model.User;

public class UserFactory {
    private static int bidderCounter = 0;
    private static int sellerCounter = 0;
    private static int adminCounter = 0;

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        for (char c : password.toCharArray()) {
            if (Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    public static String getPasswordError(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        if (password.contains(" ")) {
            return "Password cannot contain whitespace";
        }
        return null;
    }

    public static User createBidder(String username, String password) {
        String id = "BID" + String.format("%04d", ++bidderCounter);
        return new Model.Bidder(id, username, password);
    }

    public static User createSeller(String username, String password) {
        String id = "SEL" + String.format("%04d", ++sellerCounter);
        return new Model.Seller(id, username, password);
    }

    public static User createAdmin(String username, String password) {
        String id = "ADM" + String.format("%04d", ++adminCounter);
        return new Model.Admin(id, username, password);
    }

    public static User createUser(String role, String username, String password) {
        switch (role.toUpperCase()) {
            case "BIDDER":
                return createBidder(username, password);
            case "SELLER":
                return createSeller(username, password);
            case "ADMIN":
                return createAdmin(username, password);
            default:
                throw new IllegalArgumentException("Unknown role: " + role);
        }
    }
}
