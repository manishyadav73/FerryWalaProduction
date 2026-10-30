package backend.repository;

import backend.model.ConciergeMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ConciergeMessageRepository extends JpaRepository<ConciergeMessage, Long> {

    List<ConciergeMessage> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("delete from ConciergeMessage m where m.user.id = :userId")
    int deleteAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from ConciergeMessage m where m.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
