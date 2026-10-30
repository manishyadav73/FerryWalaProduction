package backend.repository;

import backend.model.Vendor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<Vendor> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<Vendor> findWithUserById(Long id);

    /** Bounding-box pre-filter; exact distance is computed in the service. */
    @Query("select v from Vendor v join fetch v.user u "
            + "where u.enabled = true and v.latitude is not null and v.longitude is not null "
            + "and v.latitude between :minLat and :maxLat and v.longitude between :minLng and :maxLng")
    List<Vendor> findInBox(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                           @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                           Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("select v from Vendor v")
    List<Vendor> findAllWithUser(Pageable pageable);

    long countByVerifiedTrue();

    long countByOnlineTrue();
}
