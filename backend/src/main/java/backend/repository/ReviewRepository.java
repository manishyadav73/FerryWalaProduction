package backend.repository;

import backend.model.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderId(Long orderId);

    @EntityGraph(attributePaths = "customer")
    List<Review> findByVendorIdOrderByCreatedAtDesc(Long vendorId, Pageable pageable);

}
