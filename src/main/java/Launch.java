import java.lang.reflect.Method;

public class Launch {
    public static void main(String[] args) {
        try {
            Class<?> loginAppClass = Class.forName("LoginApp");
            Method mainMethod = loginAppClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (Exception e) {
            System.err.println("Cannot launch application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
