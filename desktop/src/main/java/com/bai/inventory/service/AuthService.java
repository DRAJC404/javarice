package com.bai.inventory.service;

import com.bai.inventory.dao.JoinCodeDao;
import com.bai.inventory.dao.UserDao;
import com.bai.inventory.model.User;
import com.bai.inventory.util.PasswordUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AuthService {

    private static final String DUMMY_BCRYPT_HASH = "$2a$12$YH6lVmhQsRBF05fpaV3SNeiYSTOBHcsFfOofrPgD8naganXu1kjVW";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 5;

    private static final int MAX_REGISTRATIONS_PER_MINUTE = 3;
    private static final List<Long> registrationTimestamps = Collections.synchronizedList(new ArrayList<>());

    private static final int MAX_JOIN_ATTEMPTS_PER_MINUTE = 5;
    private static final AtomicInteger globalFailedJoinCount = new AtomicInteger(0);

    private static final Path TOKEN_STORAGE_PATH = Paths.get(System.getProperty("user.home"), ".bai", "remember.token");
    private static final Path DEVICE_ID_PATH = Paths.get(System.getProperty("user.home"), ".bai", "device.id");
    private static String cachedDeviceId;

    private final UserDao userDao;
    private final JoinCodeDao joinCodeDao;
    private static User currentUser;

    private static final Map<String, LockoutInfo> failedAttempts = new ConcurrentHashMap<>();

    public AuthService() {
        this.userDao = new UserDao();
        this.joinCodeDao = new JoinCodeDao();
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
        this.joinCodeDao = new JoinCodeDao();
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public User authenticate(String email, String rawPassword, boolean rememberMe) throws AuthenticationException {
        if (email == null || email.trim().isEmpty()) {
            throw new AuthenticationException("Email address is required.");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new AuthenticationException("Password is required.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        checkLockout(normalizedEmail);

        Optional<User> userOpt = userDao.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            PasswordUtil.verify(rawPassword, DUMMY_BCRYPT_HASH);
            recordFailedAttempt(normalizedEmail);
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();

        boolean passwordMatches = PasswordUtil.verify(rawPassword, user.getPasswordHash());
        if (!passwordMatches) {
            recordFailedAttempt(normalizedEmail);
            throw new AuthenticationException("Invalid email or password.");
        }

        String status = user.getApprovalStatus();
        if ("PENDING".equalsIgnoreCase(status)) {
            throw new AuthenticationException("Your account is waiting for approval.");
        }
        if ("REJECTED".equalsIgnoreCase(status)) {
            throw new AuthenticationException("Your registration was not approved.");
        }
        if ("DISABLED".equalsIgnoreCase(status) || !user.isActive()) {
            throw new AuthenticationException("Account is disabled.");
        }

        failedAttempts.remove(normalizedEmail);
        currentUser = user;

        if (rememberMe) {
            String rawToken = UUID.randomUUID().toString();
            String tokenHash = PasswordUtil.sha256Hex(rawToken);
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

            userDao.updateRememberToken(user.getId(), tokenHash, expiresAt);
            user.setRememberTokenHash(tokenHash);
            user.setRememberExpiresAt(expiresAt);

            saveTokenLocally(rawToken);
        } else {
            clearLocalToken();
        }

        return user;
    }

    public Optional<User> tryAutoLogin() {
        String savedToken = readLocalToken();
        if (savedToken == null || savedToken.isEmpty()) {
            return Optional.empty();
        }

        String tokenHash = PasswordUtil.sha256Hex(savedToken);
        Optional<User> userOpt = userDao.findByRememberTokenHash(tokenHash);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            currentUser = user;
            return Optional.of(user);
        } else {
            clearLocalToken();
            return Optional.empty();
        }
    }

    public void logout() {
        if (currentUser != null) {
            userDao.clearRememberToken(currentUser.getId());
            currentUser = null;
        }
        clearLocalToken();
    }

    public static String normalizeCode(String rawCode) {
        if (rawCode == null) return "";
        return rawCode.replaceAll("[\\s\\-\\u00A0\\u200B\\uFEFF]+", "").toUpperCase(java.util.Locale.ROOT).trim();
    }

    public static String getPasswordValidationError(String pass) {
        if (pass == null || pass.isEmpty()) {
            return "Please create a password.";
        }
        boolean hasMinLength = pass.length() >= 8;
        boolean hasLetter = pass.matches(".*[A-Za-z].*");
        boolean hasDigit = pass.matches(".*\\d.*");

        if (!hasMinLength && (!hasLetter || !hasDigit)) {
            return "Password must be at least 8 characters long and contain both letters and numbers.";
        }
        if (!hasMinLength) {
            return "Password must be at least 8 characters long.";
        }
        if (!hasLetter) {
            return "Password must contain at least one letter.";
        }
        if (!hasDigit) {
            return "Password must contain at least one number.";
        }
        return null;
    }

    public String validateJoinCode(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return "Please enter your invitation code.";
        }
        String normalized = normalizeCode(rawCode);
        if (normalized.length() != 8 || !normalized.matches("^[A-Z0-9]{8}$")) {
            return "The code should be 8 characters, like G87M-6MZK.";
        }
        String codeHash = PasswordUtil.sha256Hex(normalized);
        JoinCodeDao.CodeValidationResult result = joinCodeDao.validateCodeStatus(rawCode, codeHash);

        return switch (result.status()) {
            case VALID -> null;
            case NOT_FOUND -> "We couldn't find that code. Please check it and try again.";
            case EXPIRED -> "This code has expired. Ask your admin to generate a new one.";
            case ALREADY_USED -> "This code has already been used.";
            case ORG_INACTIVE -> "The organization associated with this code is inactive.";
            case SYSTEM_ERROR -> "Something went wrong on our side. Please try again in a moment.";
        };
    }

    public User registerBusinessOwner(String businessName, String fullName, String email, String password, String confirmPassword, boolean termsAccepted)
            throws ValidationException, AuthenticationException {

        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        synchronized (registrationTimestamps) {
            registrationTimestamps.removeIf(ts -> ts < windowStart);
            if (registrationTimestamps.size() >= MAX_REGISTRATIONS_PER_MINUTE) {
                throw new ValidationException("Too many registration attempts. Please wait a minute before trying again.");
            }
            registrationTimestamps.add(now);
        }

        if (businessName == null || businessName.trim().isEmpty()) {
            throw new ValidationException("Please enter your business or store name.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Please enter your full name.");
        }
        if (email == null || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("That email doesn't look right. Try something like name@company.com.");
        }
        String passErr = getPasswordValidationError(password);
        if (passErr != null) {
            throw new ValidationException(passErr);
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            throw new ValidationException("Please confirm your password.");
        }
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Passwords don't match. Please re-enter them.");
        }
        if (!termsAccepted) {
            throw new ValidationException("Please agree to the Terms of Service to continue.");
        }

        if (userDao.isEmailTaken(email.trim())) {
            throw new ValidationException("An account with this email address already exists. Try signing in instead.");
        }

        String passwordHash = PasswordUtil.hash(password);
        try {
            return userDao.registerOwner(businessName.trim(), fullName.trim(), email.trim(), passwordHash);
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new ValidationException("An account with this email address already exists. Try signing in instead.");
            }
            throw new AuthenticationException("Something went wrong on our side. Please try again in a moment.");
        }
    }

    public User register(String fullName, String email, String password, String confirmPassword, boolean termsAccepted)
            throws ValidationException, AuthenticationException {
        return registerBusinessOwner("Retail Store", fullName, email, password, confirmPassword, termsAccepted);
    }

    public static synchronized String getDeviceId() {
        if (cachedDeviceId != null) return cachedDeviceId;
        try {
            if (Files.exists(DEVICE_ID_PATH)) {
                cachedDeviceId = Files.readString(DEVICE_ID_PATH).trim();
                if (!cachedDeviceId.isEmpty()) return cachedDeviceId;
            }
            cachedDeviceId = UUID.randomUUID().toString();
            Files.createDirectories(DEVICE_ID_PATH.getParent());
            Files.writeString(DEVICE_ID_PATH, cachedDeviceId);
        } catch (IOException e) {
            cachedDeviceId = "device-" + System.getProperty("user.name", "local");
        }
        return cachedDeviceId;
    }

    public User redeemCode(String rawCode, String fullName, String email, String password, String confirmPassword)
            throws ValidationException, AuthenticationException {
        return redeemCode(rawCode, fullName, email, password, confirmPassword, true);
    }

    public User redeemCode(String rawCode, String fullName, String email, String password, String confirmPassword, boolean termsAccepted)
            throws ValidationException, AuthenticationException {

        String deviceId = getDeviceId();
        int recentAttempts = joinCodeDao.countJoinAttemptsInWindow(deviceId, 1);
        if (recentAttempts >= MAX_JOIN_ATTEMPTS_PER_MINUTE) {
            throw new ValidationException("Too many join attempts. Please wait 1 minute before trying again.");
        }
        joinCodeDao.recordJoinAttempt(deviceId);

        int failures = globalFailedJoinCount.get();
        if (failures > 3) {
            try {
                long delay = Math.min(3000L, 400L * (failures - 3));
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        if (rawCode == null || rawCode.trim().isEmpty()) {
            globalFailedJoinCount.incrementAndGet();
            throw new ValidationException("Please enter your invitation code.");
        }
        String cleanedCode = normalizeCode(rawCode);
        if (cleanedCode.length() != 8 || !cleanedCode.matches("^[A-Z0-9]{8}$")) {
            globalFailedJoinCount.incrementAndGet();
            throw new ValidationException("The code should be 8 characters, like G87M-6MZK.");
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Please enter your full name.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Please enter your work email address.");
        }
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("That email doesn't look right. Try something like name@company.com.");
        }
        String passErr = getPasswordValidationError(password);
        if (passErr != null) {
            throw new ValidationException(passErr);
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            throw new ValidationException("Please confirm your password.");
        }
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Passwords don't match. Please re-enter them.");
        }
        if (!termsAccepted) {
            throw new ValidationException("Please agree to the Terms of Service to continue.");
        }

        String codeHash = PasswordUtil.sha256Hex(cleanedCode);
        String passwordHash = PasswordUtil.hash(password);

        System.out.printf("[JoinCodeService] Redeeming code - raw: '%s', normalized: '%s', hash: '%s', now: %s%n",
                rawCode, cleanedCode, codeHash, java.time.Instant.now());

        try {
            User user = userDao.redeemJoinCode(codeHash, fullName.trim(), email.trim(), passwordHash);
            globalFailedJoinCount.set(0);
            return user;
        } catch (ValidationException ve) {
            globalFailedJoinCount.incrementAndGet();
            throw ve;
        } catch (SQLException e) {
            globalFailedJoinCount.incrementAndGet();
            throw new AuthenticationException("Something went wrong on our side. Please try again in a moment.");
        }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    private void checkLockout(String email) throws AuthenticationException {
        if (email == null) return;
        LockoutInfo info = failedAttempts.get(email.trim().toLowerCase());
        if (info != null && info.isLocked()) {
            long remainingMinutes = info.getRemainingLockoutMinutes();
            throw new AuthenticationException("Account is temporarily locked due to too many failed attempts. Please try again in " + remainingMinutes + " minute(s).");
        }
    }

    private void recordFailedAttempt(String email) {
        if (email == null) return;
        String key = email.trim().toLowerCase();
        failedAttempts.compute(key, (k, existing) -> {
            if (existing == null) {
                return new LockoutInfo(1, null);
            }
            if (existing.isLocked()) {
                return existing;
            }
            int newCount = existing.count + 1;
            if (newCount >= MAX_FAILED_ATTEMPTS) {
                return new LockoutInfo(newCount, LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            }
            return new LockoutInfo(newCount, null);
        });
    }

    private void saveTokenLocally(String rawToken) {
        try {
            Files.createDirectories(TOKEN_STORAGE_PATH.getParent());
            Files.writeString(TOKEN_STORAGE_PATH, rawToken);
        } catch (IOException e) {
            System.err.println("[AuthService] Could not persist remember token: " + e.getMessage());
        }
    }

    private String readLocalToken() {
        try {
            if (Files.exists(TOKEN_STORAGE_PATH)) {
                return Files.readString(TOKEN_STORAGE_PATH).trim();
            }
        } catch (IOException e) {
            System.err.println("[AuthService] Could not read remember token: " + e.getMessage());
        }
        return null;
    }

    private void clearLocalToken() {
        try {
            if (Files.exists(TOKEN_STORAGE_PATH)) {
                Files.delete(TOKEN_STORAGE_PATH);
            }
        } catch (IOException e) {
            System.err.println("[AuthService] Could not delete remember token file: " + e.getMessage());
        }
    }

    private static class LockoutInfo {
        final int count;
        final LocalDateTime lockExpiry;

        LockoutInfo(int count, LocalDateTime lockExpiry) {
            this.count = count;
            this.lockExpiry = lockExpiry;
        }

        boolean isLocked() {
            return lockExpiry != null && LocalDateTime.now().isBefore(lockExpiry);
        }

        long getRemainingLockoutMinutes() {
            if (lockExpiry == null) return 0;
            long secs = java.time.Duration.between(LocalDateTime.now(), lockExpiry).toSeconds();
            return Math.max(1, (secs + 59) / 60);
        }
    }

    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
