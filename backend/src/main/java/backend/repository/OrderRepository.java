package backend.repository;

import backend.model.CustomerOrder;
import backend.model.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = {"vendor", "customer"})
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"vendor", "customer"})
    List<CustomerOrder> findByVendorIdOrderByCreatedAtDesc(Long vendorId, Pageable pageable);

    @EntityGraph(attributePaths = {"vendor", "customer"})
    List<CustomerOrder> findByVendorIdAndStatusInOrderByCreatedAtDesc(Long vendorId,
                                                                      Collection<OrderStatus> statuses,
                                                                      Pageable pageable);

    @EntityGraph(attributePaths = {"vendor", "customer"})
    Optional<CustomerOrder> findWithPartiesById(Long id);

    @EntityGraph(attributePaths = {"vendor", "customer"})
    List<CustomerOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"vendor", "customer"})
    List<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByVendorIdAndStatus(Long vendorId, OrderStatus status);

    long countByVendorId(Long vendorId);

    @Query("select coalesce(sum(o.totalAmount), 0) from CustomerOrder o where o.status = :status")
    BigDecimal sumTotalByStatus(@Param("status") OrderStatus status);

    @Query("select coalesce(sum(o.totalAmount), 0) from CustomerOrder o "
            + "where o.vendor.id = :vendorId and o.status = :status and o.createdAt >= :since")
    BigDecimal sumVendorTotalSince(@Param("vendorId") Long vendorId, @Param("status") OrderStatus status,
                                   @Param("since") Instant since);

    long countByCustomerIdAndStatusAndCreatedAtAfter(Long customerId, OrderStatus status, Instant since);

    /** [customerId, count] pairs for customers with many cancelled orders recently. */
    @Query("select o.customer.id, count(o) from CustomerOrder o where o.status = :status and o.createdAt >= :since "
            + "group by o.customer.id having count(o) >= :min order by count(o) desc")
    List<Object[]> repeatCancellers(@Param("status") OrderStatus status, @Param("since") Instant since,
                                    @Param("min") long min, Pageable pageable);
}
