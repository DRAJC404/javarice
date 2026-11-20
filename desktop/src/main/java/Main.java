import com.bai.inventory.config.DatabaseConfig;
import com.bai.inventory.model.User;
import com.bai.inventory.service.AuthService;
import com.bai.inventory.util.AppExecutor;
import com.bai.inventory.util.PasswordUtil;
import com.bai.inventory.view.navigation.ViewNavigator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.Optional;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        loadBundledFonts();

        ViewNavigator.setPrimaryStage(primaryStage);

        ViewNavigator.showSplash();

        primaryStage.show();
        primaryStage.setMaximized(true);
        if (DatabaseConfig.isKioskModeEnabled()) {
            primaryStage.setFullScreen(true);
        }

        AppExecutor.execute(() -> {
            DatabaseConfig.initDataSource();

            boolean bcryptOk = PasswordUtil.runStartupSelfCheck();
            if (bcryptOk) {
                System.out.println("[bAI Startup] BCrypt ($2a) verification verified successfully.");
            } else {
                System.err.println("[bAI Startup] WARNING: BCrypt verification check failed!");
            }

            AuthService authService = new AuthService();
            Optional<User> autoUser = authService.tryAutoLogin();

            Platform.runLater(() -> {
                if (autoUser.isPresent()) {
                    User user = autoUser.get();
                    System.out.println("[bAI] Auto-login authenticated via remember-me token for: " + user.getEmail() + " (" + user.getRole() + ")");
                    ViewNavigator.routeByRole(user);
                } else {
                    ViewNavigator.showLogin();
                }
            });
        });
    }

    @Override
    public void stop() {
        System.out.println("[bAI Shutdown] Releasing background resources and connection pools...");
        AppExecutor.shutdown();
        DatabaseConfig.close();
    }

    private void loadBundledFonts() {
        String[] fontFiles = {
            "/fonts/Inter-Regular.ttf",
            "/fonts/Inter-Medium.ttf",
            "/fonts/Inter-SemiBold.ttf",
            "/fonts/Inter-Bold.ttf"
        };
        for (String file : fontFiles) {
            try (java.io.InputStream is = getClass().getResourceAsStream(file)) {
                if (is != null) {
                    javafx.scene.text.Font f = javafx.scene.text.Font.loadFont(is, 14);
                    if (f != null) {
                        System.out.println("[bAI Fonts] Loaded bundled font: " + f.getName());
                    }
                }
            } catch (Exception e) {
                System.err.println("[bAI Fonts] Could not load font " + file + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}