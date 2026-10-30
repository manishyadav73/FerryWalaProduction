package backend.service;

import backend.model.Role;
import backend.model.User;
import backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD, once, if that
 * email doesn't exist yet. Never overwrites an existing account and never
 * logs the password. There are no built-in credentials.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.password = password == null ? "" : password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isEmpty() || password.isEmpty()) {
            return;
        }
        if (password.length() < 10) {
            log.warn("ADMIN_PASSWORD is shorter than 10 characters; admin account was NOT created.");
            return;
        }
        if (users.existsByEmail(email)) {
            return;
        }
        User admin = new User();
        admin.setName("Administrator");
        admin.setEmail(email);
        admin.setUsername(users.existsByUsername("ferry-admin") ? null : "ferry-admin");
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setEmailVerified(true);
        users.save(admin);
        log.info("Created admin account for {}", email);
    }
}
