package backend.service;

import backend.dto.AuthDtos.AuthResponse;
import backend.dto.AuthDtos.LoginRequest;
import backend.dto.AuthDtos.MessageResponse;
import backend.dto.AuthDtos.RegisterRequest;
import backend.dto.AuthDtos.UserDto;
import backend.error.ApiException;
import backend.model.Category;
import backend.model.Role;
import backend.model.User;
import backend.model.Vendor;
import backend.repository.UserRepository;
import backend.repository.VendorRepository;
import backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern HAS_LETTER_AND_DIGIT = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");
    private static final Set<String> RESERVED_USERNAMES =
            Set.of("admin", "administrator", "root", "support", "ferrywala", "system", "moderator");
    private static final Duration VERIFICATION_TTL = Duration.ofHours(48);
    private static final Duration RESET_TTL = Duration.ofHours(1);
    static final String FORGOT_MESSAGE =
            "If an account exists for that email, we've sent a link to reset the password.";

    private final UserRepository users;
    private final VendorRepository vendors;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final MailService mail;
    private final boolean requireVerification;
    private final SecureRandom random = new SecureRandom();
    /** Compared against when the account doesn't exist, so response time doesn't reveal it. */
    private final String dummyHash;

    public AuthService(UserRepository users, VendorRepository vendors, PasswordEncoder encoder, JwtUtil jwt,
                       MailService mail,
                       @Value("${app.auth.require-email-verification:false}") boolean requireVerification) {
        this.users = users;
        this.vendors = vendors;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mail = mail;
        this.requireVerification = requireVerification;
        this.dummyHash = encoder.encode("ferrywala-not-a-real-password-1");
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.role() != Role.CUSTOMER && req.role() != Role.VENDOR) {
            throw ApiException.badRequest("INVALID_ROLE", "Choose Customer or Vendor.");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        if (!HAS_LETTER_AND_DIGIT.matcher(req.password()).matches()) {
            errors.put("password", "Use at least one letter and one number.");
        }
        String shopName = null;
        Category category = null;
        if (req.role() == Role.VENDOR) {
            shopName = collapse(req.shopName());
            if (shopName == null || shopName.length() < 2) {
                errors.put("shopName", "Enter your shop name (at least 2 characters).");
            }
            category = req.category();
            if (category == null) {
                errors.put("category", "Choose what you sell.");
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    errors.values().iterator().next(), errors);
        }

        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String username = req.username().trim().toLowerCase(Locale.ROOT);

        Map<String, String> taken = new LinkedHashMap<>();
        if (users.existsByEmail(email)) {
            taken.put("email", "Email already registered.");
        }
        if (users.existsByUsername(username) || RESERVED_USERNAMES.contains(username)) {
            taken.put("username", "Username already taken.");
        }
        if (!taken.isEmpty()) {
            String code = taken.size() == 2 ? "EMAIL_AND_USERNAME_TAKEN"
                    : taken.containsKey("email") ? "EMAIL_TAKEN" : "USERNAME_TAKEN";
            throw new ApiException(HttpStatus.CONFLICT, code, taken.values().iterator().next(), taken);
        }

        String rawToken = newVerificationToken();
        User user = new User();
        user.setName(collapse(req.name()));
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(req.role());
        user.setEmailVerified(false);
        user.setVerificationToken(sha256Hex(rawToken));
        user.setVerificationExpiresAt(Instant.now().plus(VERIFICATION_TTL));

        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException race) {
            // Two signups with the same email/username at the same moment.
            throw ApiException.conflict("ACCOUNT_EXISTS", "Email or username is already in use.");
        }

        if (req.role() == Role.VENDOR) {
            Vendor vendor = new Vendor();
            vendor.setUser(user);
            vendor.setShopName(shopName);
            vendor.setCategory(category);
            vendors.save(vendor);
        }

        final String toEmail = user.getEmail();
        final String toName = user.getName();
        AfterCommit.run(() -> mail.sendVerification(toEmail, toName, rawToken));

        String token = requireVerification ? null : jwt.generateToken(user.getId(), user.getRole());
        return new AuthResponse(token, toDto(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String identifier = req.identifier().trim().toLowerCase(Locale.ROOT);
        User user = (identifier.contains("@") ? users.findByEmail(identifier) : users.findByUsername(identifier))
                .orElse(null);

        boolean ok = encoder.matches(req.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !ok) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Incorrect email/username or password.");
        }
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED",
                    "This account has been suspended. Please contact support.");
        }
        if (requireVerification && !user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                    "Please confirm your email first - check your inbox for the link.");
        }
        return new AuthResponse(jwt.generateToken(user.getId(), user.getRole()), toDto(user));
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Please sign in again."));
        return toDto(user);
    }

    @Transactional
    public MessageResponse verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            throw ApiException.badRequest("INVALID_TOKEN", "This confirmation link is invalid or has expired.");
        }
        User user = users.findByVerificationToken(sha256Hex(rawToken.trim()))
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN",
                        "This confirmation link is invalid or has expired."));
        if (user.getVerificationExpiresAt() == null || user.getVerificationExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("INVALID_TOKEN", "This confirmation link is invalid or has expired.");
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpiresAt(null);
        return new MessageResponse("Email confirmed. Thank you!");
    }

    @Transactional
    public MessageResponse resendVerification(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Please sign in again."));
        if (user.isEmailVerified()) {
            return new MessageResponse("Your email is already confirmed.");
        }
        String rawToken = newVerificationToken();
        user.setVerificationToken(sha256Hex(rawToken));
        user.setVerificationExpiresAt(Instant.now().plus(VERIFICATION_TTL));
        final String toEmail = user.getEmail();
        final String toName = user.getName();
        AfterCommit.run(() -> mail.sendVerification(toEmail, toName, rawToken));
        return new MessageResponse(mail.isEnabled()
                ? "We've sent a new confirmation email."
                : "Email sending isn't set up on this server yet.");
    }

    /**
     * Always answers the same way, whether or not the email exists, so the form
     * can't be used to find out who has an account. Suspended accounts get nothing.
     */
    @Transactional
    public MessageResponse forgotPassword(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        User user = email.isEmpty() ? null : users.findByEmail(email).orElse(null);
        if (user != null && user.isEnabled()) {
            String rawToken = newVerificationToken();
            user.setPasswordResetToken(sha256Hex(rawToken));
            user.setPasswordResetExpiresAt(Instant.now().plus(RESET_TTL));
            final String toEmail = user.getEmail();
            final String toName = user.getName();
            AfterCommit.run(() -> mail.sendPasswordReset(toEmail, toName, rawToken));
        }
        return new MessageResponse(FORGOT_MESSAGE);
    }

    /** Single use: the token is cleared as soon as the new password is saved. */
    @Transactional
    public MessageResponse resetPassword(String rawToken, String newPassword) {
        final String invalid = "This reset link is invalid or has expired. Please request a new one.";
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            throw ApiException.badRequest("INVALID_TOKEN", invalid);
        }
        if (newPassword == null || !HAS_LETTER_AND_DIGIT.matcher(newPassword).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Use at least one letter and one number.",
                    Map.of("password", "Use at least one letter and one number."));
        }
        User user = users.findByPasswordResetToken(sha256Hex(rawToken.trim()))
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", invalid));
        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            user.setPasswordResetToken(null);
            user.setPasswordResetExpiresAt(null);
            throw ApiException.badRequest("INVALID_TOKEN", invalid);
        }
        user.setPasswordHash(encoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        // They just proved they control this mailbox.
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpiresAt(null);
        return new MessageResponse("Password updated. You can sign in now.");
    }

    private UserDto toDto(User user) {
        Long vendorId = null;
        if (user.getRole() == Role.VENDOR) {
            vendorId = vendors.findByUserId(user.getId()).map(Vendor::getId).orElse(null);
        }
        return new UserDto(user.getId(), user.getName(), user.getUsername(), user.getEmail(), user.getRole(),
                user.isEmailVerified(), vendorId);
    }

    private String newVerificationToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String collapse(String s) {
        if (s == null) {
            return null;
        }
        String c = s.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        return c.isEmpty() ? null : c;
    }
}
