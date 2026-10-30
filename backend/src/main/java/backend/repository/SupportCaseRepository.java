package backend.repository;

import backend.model.SupportCase;
import backend.model.SupportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SupportCaseRepository extends JpaRepository<SupportCase, Long> {

    @EntityGraph(attributePaths = {"creator", "order"})
    List<SupportCase> findByCreatorIdOrderByCreatedAtDesc(Long creatorId, Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "order"})
    List<SupportCase> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "order"})
    List<SupportCase> findByStatusInOrderByCreatedAtDesc(Collection<SupportStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "order"})
    Optional<SupportCase> findWithPartiesById(Long id);

    long countByCreatorIdAndStatusIn(Long creatorId, Collection<SupportStatus> statuses);
}
