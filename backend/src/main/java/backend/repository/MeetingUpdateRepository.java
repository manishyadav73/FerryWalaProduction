package backend.repository;

import backend.model.MeetingUpdate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingUpdateRepository extends JpaRepository<MeetingUpdate, Long> {

    @EntityGraph(attributePaths = "sender")
    List<MeetingUpdate> findByOrderIdOrderByCreatedAtDesc(Long orderId, Pageable pageable);
}
