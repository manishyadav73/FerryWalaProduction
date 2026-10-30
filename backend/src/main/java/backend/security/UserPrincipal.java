package backend.security;

import backend.model.Role;

import java.security.Principal;

/** The authenticated caller. getName() is the user id so that STOMP
 *  user-destinations (/user/queue/...) route to exactly this user. */
public final class UserPrincipal implements Principal {

    private final Long userId;
    private final String email;
    private final Role role;

    public UserPrincipal(Long userId, String email, Role role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
