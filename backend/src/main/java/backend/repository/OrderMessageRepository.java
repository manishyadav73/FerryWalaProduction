package backend.repository;

import backend.model.OrderMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {

    @EntityGraph(attributePaths = "sender")
    List<OrderMessage> findByOrderIdOrderByCreatedAtDesc(Long orderId, Pageable pageable);
}
