package backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends email only when an SMTP server is configured (SPRING_MAIL_HOST set).
 * Otherwise it logs that email is disabled and the app keeps working.
 * Failures are logged, never thrown: signup must not fail because SMTP is down.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String from;
    private final String publicUrl;

    public MailService(ObjectProvider<JavaMailSender> senderProvider,
                       @Value("${app.mail.from}") String from,
                       @Value("${app.public-url}") String publicUrl) {
        this.senderProvider = senderProvider;
        this.from = from;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    public boolean isEnabled() {
        return senderProvider.getIfAvailable() != null;
    }

    @Async
    public void sendPasswordReset(String toEmail, String name, String rawToken) {
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            log.info("Email is not configured (SPRING_MAIL_HOST unset); skipped password reset email.");
            return;
        }
        String safeName = name == null ? "" : name.replaceAll("[\\r\\n\\t]", " ").trim();
        String link = publicUrl + "/reset-password?token=" + rawToken;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("Reset your FerryWala password");
        msg.setText("Hi " + safeName + ",\n\n"
                + "We received a request to reset your FerryWala password. Open this link to choose a new one:\n"
                + link + "\n\n"
                + "The link works once and expires in 1 hour. If you didn't ask for this, you can ignore this email - "
                + "your password stays the same.\n\n"
                + "Team FerryWala");
        try {
            sender.send(msg);
        } catch (Exception e) {
            log.warn("Could not send password reset email: {}", e.getMessage());
        }
    }

    @Async
    public void sendVerification(String toEmail, String name, String rawToken) {
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            log.info("Email is not configured (SPRING_MAIL_HOST unset); skipped confirmation email.");
            return;
        }
        String safeName = name == null ? "" : name.replaceAll("[\\r\\n\\t]", " ").trim();
        String link = publicUrl + "/verify-email?token=" + rawToken;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("Confirm your FerryWala account");
        msg.setText("Hi " + safeName + ",\n\n"
                + "Welcome to FerryWala - helping local vendors find where customers are.\n\n"
                + "Please confirm your email address:\n" + link + "\n\n"
                + "This link works for 48 hours. If you didn't create an account, you can ignore this email.\n\n"
                + "Team FerryWala");
        try {
            sender.send(msg);
        } catch (Exception e) {
            log.warn("Could not send confirmation email: {}", e.getMessage());
        }
    }
}
