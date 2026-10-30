package backend.repository;

import backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Row lock so two concurrent accepts can't both take the last kilo of stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    List<Product> findByVendorIdOrderByNameAsc(Long vendorId);

    List<Product> findByVendorIdAndAvailableTrueOrderByNameAsc(Long vendorId);

    Optional<Product> findByIdAndVendorId(Long id, Long vendorId);

    long countByVendorId(Long vendorId);

    @EntityGraph(attributePaths = "vendor")
    Page<Product> findAllBy(Pageable pageable);
}
