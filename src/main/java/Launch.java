import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Launch {
    private static final String BOOTSTRAPPED_ENV = "BTL_LAUNCHED_BY_MAVEN";

    public static void main(String[] args) {
        if (!"true".equals(System.getenv(BOOTSTRAPPED_ENV))) {
            runThroughMaven(args);
            return;
        }

        launchLoginApp(args);
    }

    private static void launchLoginApp(String[] args) {
        try {
            Class<?> loginAppClass = Class.forName("LoginApp");
            Method mainMethod = loginAppClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (ReflectiveOperationException e) {
            System.err.println("Cannot launch application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runThroughMaven(String[] args) {
        try {
            List<String> command = new ArrayList<>();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                command.addAll(Arrays.asList("cmd", "/c", "mvn"));
            } else {
                command.add("mvn");
            }

            command.add("javafx:run");
            command.add("-Djavafx.mainClass=Launch");

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().put(BOOTSTRAPPED_ENV, "true");
            builder.inheritIO();

            Process process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Maven launch failed with exit code " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Cannot start Maven launcher: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
