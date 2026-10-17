package com.bai.inventory.util;

import javafx.scene.image.Image;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

public class AppExecutor {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r);
                t.setName("bAI-Worker-" + t.getId());
                t.setDaemon(true);
                return t;
            }
    );

    private static final int MAX_CACHE_ENTRIES = 200;
    private static final Map<String, Image> IMAGE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            }
    );

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    public static void execute(Runnable command) {
        EXECUTOR.execute(command);
    }

    public static <T> Future<T> submit(Callable<T> task) {
        return EXECUTOR.submit(task);
    }

    public static Image getCachedImage(String path, int width, int height) {
        return getCachedImage(path);
    }

    public static Image getCachedImage(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        String cleanPath = path.trim();
        try {
            synchronized (IMAGE_CACHE) {
                Image cached = IMAGE_CACHE.get(cleanPath);
                if (cached != null) return cached;
            }

            Image loaded = null;
            if (cleanPath.startsWith("/")) {
                try (InputStream is = AppExecutor.class.getResourceAsStream(cleanPath)) {
                    if (is != null) {
                        loaded = new Image(is, 200, 200, true, true);
                    }
                }
            } else {
                File f = new File(cleanPath);
                if (f.exists() && f.isFile()) {
                    loaded = new Image(f.toURI().toString(), 200, 200, true, true);
                }
            }

            if (loaded != null && !loaded.isError()) {
                synchronized (IMAGE_CACHE) {
                    IMAGE_CACHE.put(cleanPath, loaded);
                }
                return loaded;
            }
        } catch (Throwable e) {
            System.err.println("[AppExecutor] Could not load image: " + cleanPath + " (" + e.getMessage() + ")");
        }
        return null;
    }

    public static void shutdown() {
        try {
            EXECUTOR.shutdown();
            if (!EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
        }
    }
}
