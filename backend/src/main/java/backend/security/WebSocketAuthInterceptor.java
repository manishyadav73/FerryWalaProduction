package backend.security;

import backend.repository.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Locks the STOMP channel down:
 *  - CONNECT may carry "Authorization: Bearer ..."; a valid token binds the session to that user.
 *  - Anonymous sessions may only read the public vendor status feed.
 *  - Signed-in sessions may also read their own /user/queue/** destinations.
 *  - Clients can never publish (SEND); all pushes originate on the server.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    public static final String PUBLIC_VENDOR_TOPIC = "/topic/vendors/status";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                Long userId = jwtUtil.parseUserId(header.substring(7).trim());
                if (userId != null) {
                    userRepository.findById(userId)
                            .filter(u -> u.isEnabled())
                            .ifPresent(u -> accessor.setUser(new UserPrincipal(u.getId(), u.getEmail(), u.getRole())));
                }
            }
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            boolean publicFeed = PUBLIC_VENDOR_TOPIC.equals(destination);
            boolean ownQueue = accessor.getUser() != null && destination != null
                    && destination.startsWith("/user/queue/");
            if (!publicFeed && !ownQueue) {
                throw new MessagingException("Subscription not allowed");
            }
        } else if (StompCommand.SEND.equals(command)) {
            throw new MessagingException("Clients cannot publish messages");
        }
        return message;
    }
}
