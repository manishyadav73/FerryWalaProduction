package backend.service;

import backend.model.LocationShare;
import backend.model.OrderStatus;
import backend.repository.ConciergeMessageRepository;
import backend.repository.LocationShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Deletes what should not be kept: expired or orphaned shared locations, old Concierge chat. */
@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final LocationShareRepository shares;
    private final ConciergeMessageRepository chat;
    private final int retentionDays;

    public RetentionJob(LocationShareRepository shares, ConciergeMessageRepository chat,
                        @Value("${app.concierge.retention-days:30}") int retentionDays) {
        this.shares = shares;
        this.chat = chat;
        this.retentionDays = Math.max(1, retentionDays);
    }

    /** Every minute: a shared position must not outlive its time limit or its request. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    public void purgeLocations() {
        List<LocationShare> expired = shares.findByExpiresAtBefore(Instant.now());
        List<LocationShare> ended = shares.findByOrderStatusIn(
                EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REJECTED));
        Set<LocationShare> doomed = new LinkedHashSet<>(expired);
        doomed.addAll(ended);
        if (!doomed.isEmpty()) {
            shares.deleteAll(doomed);
            log.info("Deleted {} location shares ({} expired, {} on ended requests)", doomed.size(),
                    expired.size(), ended.size());
        }
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgeOldChat() {
        int n = chat.deleteOlderThan(Instant.now().minus(Duration.ofDays(retentionDays)));
        if (n > 0) {
            log.info("Deleted {} Concierge messages older than {} days", n, retentionDays);
        }
    }
}
