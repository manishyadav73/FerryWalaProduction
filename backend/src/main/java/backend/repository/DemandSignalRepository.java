package backend.repository;

import backend.model.DemandSignal;
import backend.model.SignalType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DemandSignalRepository extends JpaRepository<DemandSignal, Long> {

    @Query("select s from DemandSignal s where s.createdAt >= :since "
            + "and s.latitude between :minLat and :maxLat and s.longitude between :minLng and :maxLng "
            + "order by s.createdAt desc")
    List<DemandSignal> findInBox(@Param("since") Instant since,
                                 @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                 @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                                 Pageable pageable);

    @Query("select s from DemandSignal s where s.createdAt >= :since order by s.createdAt desc")
    List<DemandSignal> findSince(@Param("since") Instant since, Pageable pageable);

    boolean existsByCustomerIdAndProductNameAndTypeAndCreatedAtAfter(Long customerId, String productName,
                                                                     SignalType type, Instant since);

    long countByCreatedAtAfter(Instant since);

    long countByCustomerIdAndCreatedAtAfter(Long customerId, Instant since);

    /** [customerId, count] pairs for users with unusually many signals. */
    @Query("select s.customerId, count(s) from DemandSignal s where s.createdAt >= :since "
            + "group by s.customerId having count(s) >= :min order by count(s) desc")
    List<Object[]> heavySignalUsers(@Param("since") Instant since, @Param("min") long min, Pageable pageable);
}
