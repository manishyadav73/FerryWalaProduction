package backend.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitPathTest {

    @Test
    void exactPathsStillMatchExactly() {
        assertTrue(RateLimitFilter.pathMatches("/api/auth/login", "/api/auth/login"));
        assertFalse(RateLimitFilter.pathMatches("/api/auth/login", "/api/auth/login/x"));
        assertFalse(RateLimitFilter.pathMatches("/api/orders", "/api/orders/5"));
    }

    @Test
    void wildcardMatchesOneNonEmptySegmentOnly() {
        assertTrue(RateLimitFilter.pathMatches("/api/orders/*/messages", "/api/orders/42/messages"));
        assertFalse(RateLimitFilter.pathMatches("/api/orders/*/messages", "/api/orders//messages"));
        assertFalse(RateLimitFilter.pathMatches("/api/orders/*/messages", "/api/orders/42/7/messages"));
        assertFalse(RateLimitFilter.pathMatches("/api/orders/*/messages", "/api/orders/42/meeting"));
        assertFalse(RateLimitFilter.pathMatches("/api/orders/*/messages", "/api/orders/messages"));
    }
}
