package Factory;

import Model.User;

public class UserFactory {
    private static int userCounter = 0;

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

    public static User createUser(String username, String password) {
        String id = "USR" + String.format("%04d", ++userCounter);
        return new Model.User(id, username, password);
    }
}