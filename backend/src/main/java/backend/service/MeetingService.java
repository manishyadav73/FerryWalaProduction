package backend.service;

import backend.concierge.MeetingPolicy;
import backend.demand.GeoUtil;
import backend.dto.ConciergeDtos.MeetingDto;
import backend.dto.ConciergeDtos.MeetingUpdateDto;
import backend.dto.ConciergeDtos.MeetingUpdateRequest;
import backend.dto.ConciergeDtos.MessageDto;
import backend.dto.ConciergeDtos.MyShare;
import backend.dto.ConciergeDtos.PeerShare;
import backend.dto.ConciergeDtos.ShareRequest;
import backend.error.ApiException;
import backend.model.CustomerOrder;
import backend.model.LocationShare;
import backend.model.MeetingUpdate;
import backend.model.MeetingUpdateType;
import backend.model.NotificationType;
import backend.model.OrderMessage;
import backend.security.UserPrincipal;
import backend.service.OrderAccess.Party;
import backend.repository.LocationShareRepository;
import backend.repository.MeetingUpdateRepository;
import backend.repository.OrderMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Meeting coordination between the two parties of one request: updates, chat, consented location sharing. */
@Service
public class MeetingService {

    /** What the Concierge (and the meeting screen) may know about the current meeting state. */
    public record Snapshot(boolean coordinationOpen, boolean myShareActive, boolean otherShareActive,
                           Long distanceMeters, MeetingUpdateType otherLastType, Integer otherLastEta) {
    }

    private final OrderAccess access;
    private final LocationShareRepository shares;
    private final MeetingUpdateRepository updates;
    private final OrderMessageRepository messages;
    private final NotificationService notifications;
    private final AuditService audit;

    public MeetingService(OrderAccess access, LocationShareRepository shares, MeetingUpdateRepository updates,
                          OrderMessageRepository messages, NotificationService notifications, AuditService audit) {
        this.access = access;
        this.shares = shares;
        this.updates = updates;
        this.messages = messages;
        this.notifications = notifications;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public MeetingDto view(UserPrincipal who, Long orderId) {
        Party p = access.require(who, orderId);
        CustomerOrder o = p.order();
        Instant now = Instant.now();
        boolean open = MeetingPolicy.coordinationOpen(o.getStatus());
        LocationShare mine = open ? activeShare(orderId, p.me().getId(), now) : null;
        LocationShare theirs = open ? activeShare(orderId, p.other().getId(), now) : null;
        Long distance = distance(mine, theirs);

        List<MeetingUpdateDto> ups = updates.findByOrderIdOrderByCreatedAtDesc(orderId, PageRequest.of(0, 20)).stream()
                .map(u -> new MeetingUpdateDto(u.getId(), u.getSender().getId().equals(p.me().getId()),
                        Text.firstName(u.getSender().getName()), u.getType(), u.getEtaMinutes(), u.getNote(),
                        u.isViaConcierge(), u.getCreatedAt()))
                .toList();
        List<MessageDto> msgs = new ArrayList<>(messages.findByOrderIdOrderByCreatedAtDesc(orderId, PageRequest.of(0, 50)).stream()
                .map(m -> new MessageDto(m.getId(), m.getSender().getId().equals(p.me().getId()),
                        Text.firstName(m.getSender().getName()), m.getBody(), m.getCreatedAt()))
                .toList());
        Collections.reverse(msgs);

        PeerShare peer = theirs == null
                ? new PeerShare(false, null, null, null, null)
                : new PeerShare(true, theirs.getExpiresAt(), MeetingPolicy.round5(theirs.getLatitude()),
                MeetingPolicy.round5(theirs.getLongitude()), theirs.getUpdatedAt());
        String otherName = p.customer() ? p.vendor().getShopName() : Text.firstName(p.other().getName());
        return new MeetingDto(o.getId(), o.getStatus(), open, MeetingPolicy.messagingOpen(o.getStatus()),
                p.customer() ? "CUSTOMER" : "VENDOR", otherName, p.vendor().getShopName(), o.getLatitude(),
                o.getLongitude(), o.getAreaName(), o.getNote(),
                new MyShare(mine != null, mine == null ? null : mine.getExpiresAt()), peer, distance, ups, msgs);
    }

    /** Booleans and a distance only; used by the Concierge so it never sees coordinates. */
    @Transactional(readOnly = true)
    public Snapshot snapshot(Party p) {
        Long orderId = p.order().getId();
        Instant now = Instant.now();
        boolean open = MeetingPolicy.coordinationOpen(p.order().getStatus());
        LocationShare mine = open ? activeShare(orderId, p.me().getId(), now) : null;
        LocationShare theirs = open ? activeShare(orderId, p.other().getId(), now) : null;
        MeetingUpdateType lastType = null;
        Integer lastEta = null;
        for (MeetingUpdate u : updates.findByOrderIdOrderByCreatedAtDesc(orderId, PageRequest.of(0, 10))) {
            if (!u.getSender().getId().equals(p.me().getId())) {
                lastType = u.getType();
                lastEta = u.getEtaMinutes();
                break;
            }
        }
        return new Snapshot(open, mine != null, theirs != null, distance(mine, theirs), lastType, lastEta);
    }

    // ------------------------------------------------------------------ updates

    @Transactional
    public MeetingUpdateDto postUpdate(UserPrincipal who, Long orderId, MeetingUpdateRequest req) {
        Party p = access.require(who, orderId);
        requireOpen(p);
        Integer eta;
        try {
            eta = MeetingPolicy.validateEta(req.type(), req.etaMinutes());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("VALIDATION_ERROR", e.getMessage());
        }
        boolean viaConcierge = "CONCIERGE".equals(req.source());
        MeetingUpdate u = new MeetingUpdate();
        u.setOrder(p.order());
        u.setSender(p.me());
        u.setType(req.type());
        u.setEtaMinutes(eta);
        u.setNote(Text.clean(req.note(), 200));
        u.setViaConcierge(viaConcierge);
        MeetingUpdate saved = updates.save(u);

        String first = Text.firstName(p.me().getName());
        String who2 = p.customer() ? first : p.vendor().getShopName();
        notifications.notifyUser(p.other(), NotificationType.MEETING, who2 + ": " + describe(req.type(), eta),
                "Request #" + orderId, "/meet/" + orderId);
        notifications.pushOrderEvent(p.other().getId(), orderId, p.order().getStatus());
        audit.record(p.me().getId(), "MEETING_UPDATE", orderId,
                req.type().name() + (viaConcierge ? " via concierge" : ""));
        return new MeetingUpdateDto(saved.getId(), true, first, saved.getType(), saved.getEtaMinutes(),
                saved.getNote(), saved.isViaConcierge(), saved.getCreatedAt());
    }

    // ------------------------------------------------------------------ messages

    @Transactional
    public MessageDto sendMessage(UserPrincipal who, Long orderId, String rawBody) {
        Party p = access.require(who, orderId);
        if (!MeetingPolicy.messagingOpen(p.order().getStatus())) {
            throw ApiException.conflict("CHAT_CLOSED", "This request has ended, so messages are closed.");
        }
        String body = Text.clean(rawBody, 500);
        if (body == null) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Type a message first.");
        }
        OrderMessage m = new OrderMessage();
        m.setOrder(p.order());
        m.setSender(p.me());
        m.setBody(body);
        OrderMessage saved = messages.save(m);

        String first = Text.firstName(p.me().getName());
        String label = p.customer() ? first : p.vendor().getShopName();
        notifications.notifyUser(p.other(), NotificationType.MESSAGE, "Message from " + label,
                body.length() > 120 ? body.substring(0, 119) + "…" : body, "/meet/" + orderId);
        notifications.pushOrderEvent(p.other().getId(), orderId, p.order().getStatus());
        return new MessageDto(saved.getId(), true, first, saved.getBody(), saved.getCreatedAt());
    }

    // ------------------------------------------------------------------ location sharing

    /**
     * Starts sharing, or refreshes the position of a share that is already running. Refreshing does
     * not extend the time limit: to share longer the user has to start again on purpose.
     */
    @Transactional
    public MyShare startOrRefreshShare(UserPrincipal who, Long orderId, ShareRequest req) {
        Party p = access.require(who, orderId);
        requireOpen(p);
        if (!GeoUtil.validLatLng(req.latitude(), req.longitude())) {
            throw ApiException.badRequest("INVALID_LOCATION", "That location doesn't look right.");
        }
        Instant now = Instant.now();
        LocationShare s = shares.findByOrderIdAndUserId(orderId, p.me().getId()).orElseGet(LocationShare::new);
        boolean fresh = s.getId() == null || !MeetingPolicy.shareActive(s.getExpiresAt(), now);
        if (fresh) {
            int minutes = MeetingPolicy.clampShareMinutes(req.durationMinutes());
            s.setOrder(p.order());
            s.setUser(p.me());
            s.setStartedAt(now);
            s.setExpiresAt(now.plus(Duration.ofMinutes(minutes)));
        }
        s.setLatitude(req.latitude());
        s.setLongitude(req.longitude());
        s.setUpdatedAt(now);
        LocationShare saved = shares.save(s);
        if (fresh) {
            audit.record(p.me().getId(), "LOCATION_SHARE_START", orderId,
                    "minutes=" + Duration.between(now, saved.getExpiresAt()).toMinutes());
            String label = p.customer() ? Text.firstName(p.me().getName()) : p.vendor().getShopName();
            notifications.notifyUser(p.other(), NotificationType.MEETING, label + " is sharing their location",
                    "For a short time, to help you meet. Request #" + orderId, "/meet/" + orderId);
            notifications.pushOrderEvent(p.other().getId(), orderId, p.order().getStatus());
        }
        return new MyShare(true, saved.getExpiresAt());
    }

    /** Stopping deletes the stored position straight away. Idempotent. */
    @Transactional
    public MyShare stopShare(UserPrincipal who, Long orderId) {
        Party p = access.require(who, orderId);
        shares.findByOrderIdAndUserId(orderId, p.me().getId()).ifPresent(s -> {
            shares.delete(s);
            audit.record(p.me().getId(), "LOCATION_SHARE_STOP", orderId, null);
            notifications.pushOrderEvent(p.other().getId(), orderId, p.order().getStatus());
        });
        return new MyShare(false, null);
    }

    // ------------------------------------------------------------------ helpers

    private LocationShare activeShare(Long orderId, Long userId, Instant now) {
        return shares.findByOrderIdAndUserId(orderId, userId)
                .filter(s -> MeetingPolicy.shareActive(s.getExpiresAt(), now))
                .orElse(null);
    }

    private static Long distance(LocationShare a, LocationShare b) {
        if (a == null || b == null) {
            return null;
        }
        return MeetingPolicy.distanceMeters(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    private static void requireOpen(Party p) {
        if (!MeetingPolicy.coordinationOpen(p.order().getStatus())) {
            throw ApiException.conflict("MEETING_CLOSED",
                    "Meeting tools are available once the vendor accepts the request, until it ends.");
        }
    }

    private static String describe(MeetingUpdateType type, Integer eta) {
        return switch (type) {
            case RUNNING_LATE -> "running late (about " + eta + " min)";
            case PROPOSE_TIME -> "suggests meeting in about " + eta + " min";
            case ARRIVED -> "has arrived at the meeting point";
            case CANT_FIND -> "can't find the meeting point";
            case NEED_HELP -> "needs help meeting up";
        };
    }
}
