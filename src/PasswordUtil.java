import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

// Passwords are stored as a salted SHA-256 hash, never as plain text.
// Stored format:  sha256$<salt>$<hash>
public final class PasswordUtil {

    private static final String PREFIX = "sha256$";
    private static final int ITERATIONS = 10000;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final String RULES =
            "Password must contain:\n"
            + "- At least 8 characters\n"
            + "- One uppercase letter (A-Z)\n"
            + "- One lowercase letter (a-z)\n"
            + "- One number (0-9)\n"
            + "- One special character (@, #, !, etc.)";

    private PasswordUtil() {
    }

    // Create a new hash with a random salt
    public static String hash(String password) {

        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);

        String salt = Base64.getEncoder().encodeToString(saltBytes);

        return PREFIX + salt + "$" + digest(salt, password);
    }

    // Check a typed password against the stored value.
    // Old accounts that still have a plain-text password also work.
    public static boolean verify(String password, String stored) {

        if (password == null || stored == null) {
            return false;
        }

        if (isLegacy(stored)) {
            return stored.equals(password);
        }

        String[] parts = stored.split("\\$");

        if (parts.length != 3) {
            return false;
        }

        byte[] expected = parts[2].getBytes(StandardCharsets.UTF_8);
        byte[] actual = digest(parts[1], password)
                .getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expected, actual);
    }

    // True when the stored password is still plain text
    public static boolean isLegacy(String stored) {
        return stored != null && !stored.startsWith(PREFIX);
    }

    public static boolean isStrong(String password) {
        return password != null && password.matches(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s])\\S{8,}$");
    }

    private static String digest(String salt, String password) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] result = (salt + password)
                    .getBytes(StandardCharsets.UTF_8);

            for (int i = 0; i < ITERATIONS; i++) {
                md.reset();
                result = md.digest(result);
            }

            return Base64.getEncoder().encodeToString(result);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
