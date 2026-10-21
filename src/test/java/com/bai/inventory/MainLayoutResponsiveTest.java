package com.bai.inventory;

import com.bai.inventory.view.layout.MainLayoutView;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MainLayoutResponsiveTest {

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testLayoutAt375pxMobile() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                MainLayoutView layout = new MainLayoutView();
                layout.triggerResponsiveLayout(375.0);

                // 1. Mobile narrow mode
                assertTrue(layout.isNarrowMode(), "Should be in narrow mode at 375px");
                assertFalse(layout.isDrawerOpen(), "Drawer must be closed by default at 375px");
                assertTrue(layout.getTopNavbarHamburgerBtn().isVisible(), "Top navbar hamburger must be visible at 375px");
                assertNull(layout.getSidebarBox().getParent(), "Sidebar should NOT push content at 375px");

                // 2. Open drawer via top hamburger
                layout.getTopNavbarHamburgerBtn().fire();
                assertTrue(layout.isDrawerOpen(), "Drawer should be open after firing hamburger");
                assertEquals(256.0, layout.getSidebarBox().getPrefWidth(), 0.1, "Sidebar in drawer should have full 256px width");

                layout.dispose();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testLayoutAt768pxTablet() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                MainLayoutView layout = new MainLayoutView();
                layout.triggerResponsiveLayout(768.0);

                // 1. Tablet narrow mode (< 1024px)
                assertTrue(layout.isNarrowMode(), "Should be in narrow mode at 768px");
                assertFalse(layout.isDrawerOpen(), "Drawer must be closed by default at 768px");
                assertTrue(layout.getTopNavbarHamburgerBtn().isVisible(), "Top navbar hamburger must be visible at 768px");
                assertNull(layout.getSidebarBox().getParent(), "Sidebar should not be docked at 768px");

                layout.dispose();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testLayoutAt1440pxDesktop() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                MainLayoutView layout = new MainLayoutView();
                layout.triggerResponsiveLayout(1440.0);

                // 1. Desktop full width
                assertFalse(layout.isNarrowMode(), "Should not be in narrow mode at 1440px");
                assertFalse(layout.getTopNavbarHamburgerBtn().isVisible(), "Top navbar hamburger must be hidden at 1440px");
                assertNotNull(layout.getSidebarBox().getParent(), "Sidebar must be docked in main layout at 1440px");
                assertEquals(256.0, layout.getSidebarBox().getPrefWidth(), 0.1, "Sidebar width should be 256px on desktop");

                // 2. Main content minWidth = 0 to prevent cropping
                assertEquals(0.0, layout.getContentArea().getMinWidth(), 0.1, "Content area min-w-0");

                layout.dispose();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
