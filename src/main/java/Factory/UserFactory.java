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
            return "Mật khẩu không thể rỗng!";
        }
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự!";
        }
        if (password.contains(" ")) {
            return "Mật khẩu không thể chứa dấu cách!";
        }
        return null;
    }

    public static User createUser(String username, String password) {
        String id = "USR" + String.format("%04d", ++userCounter);
        return new Model.User(id, username, password);
    }
}