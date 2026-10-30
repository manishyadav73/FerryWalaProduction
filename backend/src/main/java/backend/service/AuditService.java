package backend.service;

import backend.model.AuditEvent;
import backend.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository audit;

    public AuditService(AuditEventRepository audit) {
        this.audit = audit;
    }

    /** detail must never contain coordinates, message text or personal data. */
    @Transactional
    public void record(Long actorId, String eventType, Long orderId, String detail) {
        AuditEvent e = new AuditEvent();
        e.setActorId(actorId);
        e.setEventType(eventType);
        e.setOrderId(orderId);
        e.setDetail(detail == null ? null : (detail.length() > 200 ? detail.substring(0, 200) : detail));
        audit.save(e);
    }
}
