package com.bai.inventory.util;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class ProductImageManager {

    private static final Path BASE_IMAGE_DIR = Paths.get(System.getProperty("user.home"), ".bai", "product-images");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif");

    private static final int MAX_CACHE_ENTRIES = 200;
    private static final Map<String, Image> IMAGE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            }
    );

    public static Path getBaseImageDir() {
        return BASE_IMAGE_DIR;
    }

    public static Path getOrgDirectory(int orgId) {
        int safeOrgId = orgId > 0 ? orgId : 1;
        return BASE_IMAGE_DIR.resolve("org-" + safeOrgId);
    }

    public static boolean isValidImageFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        if (file.length() > MAX_FILE_SIZE) {
            return false;
        }
        String name = file.getName().toLowerCase();
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx == -1) return false;
        String ext = name.substring(dotIdx + 1);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return false;
        }

        try {
            BufferedImage bimg = ImageIO.read(file);
            return bimg != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static String storeImage(File sourceFile, int orgId) throws IOException {
        if (!isValidImageFile(sourceFile)) {
            throw new IllegalArgumentException("Invalid image file format or size.");
        }

        Path orgDir = getOrgDirectory(orgId);
        Files.createDirectories(orgDir);

        String originalName = sourceFile.getName().toLowerCase();
        String ext = "png";
        int dot = originalName.lastIndexOf('.');
        if (dot != -1) {
            ext = originalName.substring(dot + 1);
        }

        String uniqueFileName = UUID.randomUUID().toString() + "." + ext;
        Path targetPath = orgDir.resolve(uniqueFileName);

        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        int safeOrgId = orgId > 0 ? orgId : 1;
        return "org-" + safeOrgId + "/" + uniqueFileName;
    }

    public static void deleteOldImage(String relativePath, int orgId) {
        if (relativePath == null || relativePath.isBlank()) return;
        if (relativePath.startsWith("/images/") || relativePath.startsWith("demo:")) return;

        try {
            File f = resolveImageFile(relativePath, orgId);
            if (f != null && f.exists() && f.isFile()) {
                Path fPath = f.toPath().toAbsolutePath().normalize();
                Path orgDir = getOrgDirectory(orgId).toAbsolutePath().normalize();
                if (fPath.startsWith(orgDir)) {
                    Files.deleteIfExists(fPath);
                }
            }
            IMAGE_CACHE.remove(relativePath.trim());
        } catch (Exception e) {
            System.err.println("[ProductImageManager] Failed to delete image " + relativePath + ": " + e.getMessage());
        }
    }

    public static File resolveImageFile(String storedPath, int orgId) {
        if (storedPath == null || storedPath.isBlank()) return null;
        String clean = storedPath.trim();
        if (clean.startsWith("/images/") || clean.startsWith("demo:")) return null;

        File direct = new File(clean);
        if (direct.isAbsolute() && direct.exists() && direct.isFile()) {
            return direct;
        }

        Path relativeInBase = BASE_IMAGE_DIR.resolve(clean).normalize();
        if (Files.exists(relativeInBase) && Files.isRegularFile(relativeInBase)) {
            return relativeInBase.toFile();
        }

        Path inOrgDir = getOrgDirectory(orgId).resolve(clean).normalize();
        if (Files.exists(inOrgDir) && Files.isRegularFile(inOrgDir)) {
            return inOrgDir.toFile();
        }

        return null;
    }

    public static Image getImage(String storedPath, int orgId, double reqWidth, double reqHeight) {
        if (storedPath == null || storedPath.isBlank()) return null;
        String clean = storedPath.trim();

        String cacheKey = clean + "_" + (int) reqWidth + "x" + (int) reqHeight;
        synchronized (IMAGE_CACHE) {
            Image cached = IMAGE_CACHE.get(cacheKey);
            if (cached != null) return cached;
        }

        try {
            Image loaded = null;
            if (clean.startsWith("/images/") || clean.startsWith("demo:")) {
                String resourcePath = clean.startsWith("demo:") ? clean.substring(5) : clean;
                try (InputStream is = ProductImageManager.class.getResourceAsStream(resourcePath)) {
                    if (is != null) {
                        loaded = new Image(is, reqWidth, reqHeight, true, true);
                    }
                }
            } else {
                File file = resolveImageFile(clean, orgId);
                if (file != null && file.exists()) {
                    String url = file.toURI().toString();
                    loaded = new Image(url, reqWidth, reqHeight, true, true, false);
                    if (loaded.isError()) {
                        System.err.println("[ProductImageManager] Warning: Error decoding image at "
                                + file.getAbsolutePath() + ": " + loaded.getException());
                        return null;
                    }
                } else {
                    System.err.println("[ProductImageManager] Warning: Image file not found: " + clean);
                    return null;
                }
            }

            if (loaded != null && !loaded.isError()) {
                synchronized (IMAGE_CACHE) {
                    IMAGE_CACHE.put(cacheKey, loaded);
                }
                return loaded;
            }
        } catch (Throwable t) {
            System.err.println("[ProductImageManager] Warning: Exception loading image '" + clean + "': " + t.getMessage());
        }
        return null;
    }

    public static void clearCache() {
        IMAGE_CACHE.clear();
    }
}
