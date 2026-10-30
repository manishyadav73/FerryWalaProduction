package backend.service;

import backend.dto.ConciergeDtos.CaseDto;
import backend.dto.ConciergeDtos.CreateCaseRequest;
import backend.dto.ConciergeDtos.UpdateCaseRequest;
import backend.error.ApiException;
import backend.model.CustomerOrder;
import backend.model.NotificationType;
import backend.model.Role;
import backend.model.SupportCase;
import backend.model.SupportStatus;
import backend.model.User;
import backend.repository.SupportCaseRepository;
import backend.repository.UserRepository;
import backend.security.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Support cases: the human escalation path for customers and vendors. Admins work them. */
@Service
public class SupportService {

    static final int MAX_ACTIVE_CASES_PER_USER = 5;
    private static final Set<SupportStatus> ACTIVE = EnumSet.of(SupportStatus.OPEN, SupportStatus.IN_PROGRESS);

    private final SupportCaseRepository cases;
    private final UserRepository users;
    private final OrderAccess access;
    private final NotificationService notifications;
    private final AuditService audit;

    public SupportService(SupportCaseRepository cases, UserRepository users, OrderAccess access,
                          NotificationService notifications, AuditService audit) {
        this.cases = cases;
        this.users = users;
        this.access = access;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional
    public CaseDto create(UserPrincipal who, CreateCaseRequest req) {
        if (who.getRole() == Role.ADMIN) {
            throw ApiException.forbidden("Admins work on cases; they don't open them.");
        }
        User me = users.findById(who.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("Please sign in again."));
        if (cases.countByCreatorIdAndStatusIn(me.getId(), ACTIVE) >= MAX_ACTIVE_CASES_PER_USER) {
            throw ApiException.conflict("TOO_MANY_OPEN_CASES",
                    "You already have several open support cases. Please wait for a reply on those first.");
        }
        String summary = Text.clean(req.summary(), 1000);
        if (summary == null || summary.length() < 5) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Describe the problem in a few words.");
        }
        CustomerOrder order = null;
        if (req.orderId() != null) {
            order = access.require(who, req.orderId()).order(); // 404 unless the caller is a party
        }
        SupportCase c = new SupportCase();
        c.setCreator(me);
        c.setOrder(order);
        c.setCategory(req.category());
        c.setSummary(summary);
        c.setSource("CONCIERGE".equals(req.source()) ? "CONCIERGE" : "APP");
        SupportCase saved = cases.save(c);

        audit.record(me.getId(), "SUPPORT_CASE_CREATED", order == null ? null : order.getId(),
                "case=" + saved.getId() + " " + req.category() + " via " + saved.getSource());
        for (User admin : users.findByRoleAndEnabledTrue(Role.ADMIN, PageRequest.of(0, 20))) {
            notifications.notifyUser(admin, NotificationType.SUPPORT, "New support case #" + saved.getId(),
                    req.category().name().toLowerCase(Locale.ROOT).replace('_', ' '), "/admin");
        }
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CaseDto> mine(Long userId) {
        return cases.findByCreatorIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50)).stream()
                .map(SupportService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CaseDto get(UserPrincipal who, Long id) {
        SupportCase c = cases.findWithPartiesById(id).orElseThrow(() -> ApiException.notFound("Case not found."));
        if (who.getRole() != Role.ADMIN && !c.getCreator().getId().equals(who.getUserId())) {
            throw ApiException.notFound("Case not found.");
        }
        return toDto(c);
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public List<CaseDto> adminList(String status) {
        PageRequest page = PageRequest.of(0, 100);
        List<SupportCase> list;
        if (status == null || status.isBlank() || status.equalsIgnoreCase("active")) {
            list = cases.findByStatusInOrderByCreatedAtDesc(ACTIVE, page);
        } else if (status.equalsIgnoreCase("all")) {
            list = cases.findAllByOrderByCreatedAtDesc(page);
        } else {
            SupportStatus s;
            try {
                s = SupportStatus.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("BAD_REQUEST", "Unknown status filter.");
            }
            list = cases.findByStatusInOrderByCreatedAtDesc(Set.of(s), page);
        }
        return list.stream().map(SupportService::toDto).toList();
    }

    @Transactional
    public CaseDto adminUpdate(Long adminId, Long id, UpdateCaseRequest req) {
        SupportCase c = cases.findWithPartiesById(id).orElseThrow(() -> ApiException.notFound("Case not found."));
        c.setStatus(req.status());
        c.setResolutionNote(Text.clean(req.resolutionNote(), 500));
        c.setUpdatedAt(Instant.now());
        audit.record(adminId, "SUPPORT_CASE_UPDATED", c.getOrder() == null ? null : c.getOrder().getId(),
                "case=" + id + " -> " + req.status());
        notifications.notifyUser(c.getCreator(), NotificationType.SUPPORT, "Your support case #" + id + " is "
                + req.status().name().toLowerCase(Locale.ROOT).replace('_', ' '), c.getResolutionNote(), "/support");
        return toDto(c);
    }

    static CaseDto toDto(SupportCase c) {
        return new CaseDto(c.getId(), c.getCategory(), c.getStatus(), c.getSummary(), c.getSource(),
                c.getOrder() == null ? null : c.getOrder().getId(), c.getResolutionNote(),
                Text.firstName(c.getCreator().getName()), c.getCreator().getRole().name(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
