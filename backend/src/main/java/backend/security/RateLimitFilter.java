package backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory sliding-window limiter for abuse-prone endpoints.
 * Per-instance only: behind several app instances use a shared limiter
 * (reverse proxy or Redis) instead. Not a Spring bean (see JwtAuthFilter).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String name, String method, String path, int max, long windowMs) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("login", "POST", "/api/auth/login", 10, 60_000),
            new Rule("register", "POST", "/api/auth/register", 8, 10 * 60_000),
            new Rule("forgot", "POST", "/api/auth/forgot-password", 5, 10 * 60_000),
            new Rule("reset", "POST", "/api/auth/reset-password", 10, 10 * 60_000),
            new Rule("resend", "POST", "/api/auth/resend-verification", 3, 10 * 60_000),
            new Rule("signal", "POST", "/api/demand/signals", 40, 60_000),
            new Rule("order", "POST", "/api/orders", 20, 10 * 60_000),
            new Rule("upload", "POST", "/api/vendor/me/image", 10, 10 * 60_000),
            // AI Concierge, meeting coordination and support ("*" matches one path segment)
            new Rule("chat", "POST", "/api/concierge/chat", 30, 60_000),
            new Rule("msg", "POST", "/api/orders/*/messages", 40, 60_000),
            new Rule("meetupd", "POST", "/api/orders/*/meeting/updates", 20, 10 * 60_000),
            new Rule("share", "PUT", "/api/orders/*/location-share", 120, 60_000),
            new Rule("case", "POST", "/api/support/cases", 10, 10 * 60_000)
    );

    private final boolean trustProxy;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimitFilter(boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Rule rule = match(request);
        if (rule != null) {
            long retryAfterSec = hit(rule.name() + "|" + clientIp(request), rule);
            if (retryAfterSec > 0) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(retryAfterSec));
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":"
                        + "\"Too many attempts. Please wait a moment and try again.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Rule match(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (Rule r : RULES) {
            if (r.method().equalsIgnoreCase(request.getMethod()) && pathMatches(r.path(), path)) {
                return r;
            }
        }
        return null;
    }

    /** Exact match, except that a "*" segment in the pattern matches any single non-empty segment. */
    static boolean pathMatches(String pattern, String path) {
        if (pattern.indexOf('*') < 0) {
            return pattern.equals(path);
        }
        String[] p = pattern.split("/", -1);
        String[] a = path.split("/", -1);
        if (p.length != a.length) {
            return false;
        }
        for (int i = 0; i < p.length; i++) {
            if (p[i].equals("*") ? a[i].isEmpty() : !p[i].equals(a[i])) {
                return false;
            }
        }
        return true;
    }

    /** @return 0 if allowed, otherwise seconds to wait. */
    private long hit(String key, Rule rule) {
        long now = System.currentTimeMillis();
        if (hits.size() > 20_000) {
            hits.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty() || now - e.getValue().peekLast() > 3_600_000;
                }
            });
        }
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > rule.windowMs()) {
                q.pollFirst();
            }
            if (q.size() >= rule.max()) {
                long wait = (rule.windowMs() - (now - q.peekFirst())) / 1000 + 1;
                return Math.max(1, wait);
            }
            q.addLast(now);
            return 0;
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (trustProxy) {
            // Our nginx sets X-Real-IP to the connecting address, overwriting anything the
            // client sent. The first X-Forwarded-For entry is client-controlled, so it is
            // only a fallback (and the LAST entry, added by the nearest proxy, not the first).
            String real = request.getHeader("X-Real-IP");
            if (real != null && !real.isBlank()) {
                return real.trim();
            }
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String[] parts = xff.split(",");
                return parts[parts.length - 1].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
