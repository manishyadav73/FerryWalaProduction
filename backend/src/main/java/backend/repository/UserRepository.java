package backend.repository;

import backend.model.Role;
import backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    long countByRole(Role role);

    List<User> findByRoleAndEnabledTrue(Role role, Pageable pageable);

    @Query("select u from User u where "
            + "lower(u.name) like :q or lower(u.email) like :q or lower(coalesce(u.username, '')) like :q")
    Page<User> search(@Param("q") String q, Pageable pageable);
}
