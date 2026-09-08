package Utils;

public class ValidationUtils {

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidUsername(String username) {
        return username != null
                && username.length() >= 3
                && username.length() <= 30;
    }

    public static boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 4;
    }
}
