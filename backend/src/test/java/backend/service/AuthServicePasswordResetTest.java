package backend.service;

import backend.error.ApiException;
import backend.model.Role;
import backend.model.User;
import backend.repository.UserRepository;
import backend.repository.VendorRepository;
import backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Plain unit tests (no Spring context): forgot-password / reset-password rules. */
class AuthServicePasswordResetTest {

    private UserRepository users;
    private MailService mail;
    private PasswordEncoder encoder;
    private AuthService auth;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        mail = mock(MailService.class);
        encoder = new BCryptPasswordEncoder(4);
        auth = new AuthService(users, mock(VendorRepository.class), encoder, mock(JwtUtil.class), mail, false);

        user = new User();
        user.setName("Asha Rao");
        user.setEmail("asha@example.com");
        user.setUsername("asha");
        user.setRole(Role.CUSTOMER);
        user.setPasswordHash(encoder.encode("OldPassw0rd"));
    }

    @Test
    void forgotPasswordForUnknownEmailSendsNothingAndLooksIdentical() {
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        var res = auth.forgotPassword("nobody@example.com");

        assertEquals(AuthService.FORGOT_MESSAGE, res.message());
        verify(mail, never()).sendPasswordReset(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPasswordForKnownEmailStoresOnlyAHashOfTheEmailedToken() {
        when(users.findByEmail("asha@example.com")).thenReturn(Optional.of(user));

        var res = auth.forgotPassword("  Asha@Example.com ");

        assertEquals(AuthService.FORGOT_MESSAGE, res.message());
        ArgumentCaptor<String> raw = ArgumentCaptor.forClass(String.class);
        verify(mail).sendPasswordReset(anyString(), anyString(), raw.capture());
        assertNotNull(user.getPasswordResetToken());
        assertNotEquals(raw.getValue(), user.getPasswordResetToken());
        assertEquals(AuthService.sha256Hex(raw.getValue()), user.getPasswordResetToken());
        assertTrue(user.getPasswordResetExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void suspendedAccountsGetNoResetEmail() {
        user.setEnabled(false);
        when(users.findByEmail("asha@example.com")).thenReturn(Optional.of(user));

        auth.forgotPassword("asha@example.com");

        verify(mail, never()).sendPasswordReset(anyString(), anyString(), anyString());
        assertNull(user.getPasswordResetToken());
    }

    @Test
    void resetChangesPasswordAndTokenCannotBeReused() {
        String raw = "raw-token-123";
        user.setPasswordResetToken(AuthService.sha256Hex(raw));
        user.setPasswordResetExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
        when(users.findByPasswordResetToken(AuthService.sha256Hex(raw))).thenReturn(Optional.of(user));

        auth.resetPassword(raw, "NewPassw0rd");

        assertTrue(encoder.matches("NewPassw0rd", user.getPasswordHash()));
        assertFalse(encoder.matches("OldPassw0rd", user.getPasswordHash()));
        assertNull(user.getPasswordResetToken());
        assertNull(user.getPasswordResetExpiresAt());
        assertTrue(user.isEmailVerified());

        // Second use: the token no longer exists in the database.
        when(users.findByPasswordResetToken(AuthService.sha256Hex(raw))).thenReturn(Optional.empty());
        ApiException again = assertThrows(ApiException.class, () -> auth.resetPassword(raw, "Another1Pass"));
        assertEquals("INVALID_TOKEN", again.getCode());
    }

    @Test
    void expiredTokenIsRejectedAndCleared() {
        String raw = "old-token";
        user.setPasswordResetToken(AuthService.sha256Hex(raw));
        user.setPasswordResetExpiresAt(Instant.now().minus(Duration.ofMinutes(1)));
        when(users.findByPasswordResetToken(AuthService.sha256Hex(raw))).thenReturn(Optional.of(user));
        String before = user.getPasswordHash();

        ApiException ex = assertThrows(ApiException.class, () -> auth.resetPassword(raw, "NewPassw0rd"));

        assertEquals("INVALID_TOKEN", ex.getCode());
        assertEquals(before, user.getPasswordHash());
        assertNull(user.getPasswordResetToken());
    }

    @Test
    void weakPasswordIsRejectedWithoutTouchingTheToken() {
        String raw = "tok";
        user.setPasswordResetToken(AuthService.sha256Hex(raw));
        user.setPasswordResetExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));

        ApiException ex = assertThrows(ApiException.class, () -> auth.resetPassword(raw, "onlyletters"));

        assertEquals("VALIDATION_ERROR", ex.getCode());
        assertNotNull(user.getPasswordResetToken());
    }

    @Test
    void garbageTokensAreRejected() {
        assertThrows(ApiException.class, () -> auth.resetPassword("", "NewPassw0rd"));
        assertThrows(ApiException.class, () -> auth.resetPassword("x".repeat(200), "NewPassw0rd"));
        assertThrows(ApiException.class, () -> auth.resetPassword(null, "NewPassw0rd"));
    }
}
