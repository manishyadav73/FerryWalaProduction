package backend.repository;

import backend.model.LocationShare;
import backend.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LocationShareRepository extends JpaRepository<LocationShare, Long> {

    Optional<LocationShare> findByOrderIdAndUserId(Long orderId, Long userId);

    List<LocationShare> findByExpiresAtBefore(Instant now);

    List<LocationShare> findByOrderStatusIn(Collection<OrderStatus> statuses);
}
