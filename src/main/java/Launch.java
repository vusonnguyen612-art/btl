import java.lang.reflect.Method;

/**
 * Lớp khởi chạy (Entry point) cho ứng dụng JavaFX đấu giá.
 * <p>
 * Sử dụng Java Reflection để gọi phương thức {@code main()} của lớp
 * {@code LoginApp} — cho phép tách biệt module khởi động khỏi module
 * JavaFX chính, hỗ trợ cấu hình multi-module hoặc classloading linh hoạt.
 * <p>
 * Cách dùng: {@code java Launch}
 */
public class Launch {
    public static void main(String[] args) {
        try {
            Class<?> loginAppClass = Class.forName("LoginApp");
            Method mainMethod = loginAppClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ReflectiveOperationException e) {
            System.err.println("Cannot launch application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
