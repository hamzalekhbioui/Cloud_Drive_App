package com.cloud.drive.repository;

import com.cloud.drive.model.WebhookEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    @Query("""
            select e from WebhookEvent e
            where (:processed is null or e.processed = :processed)
              and (:fromDate is null or e.createdAt >= :fromDate)
              and (:toDate is null or e.createdAt <= :toDate)
            """)
    Page<WebhookEvent> findAllForAdmin(@Param("processed") Boolean processed,
                                       @Param("fromDate") java.time.LocalDateTime fromDate,
                                       @Param("toDate") java.time.LocalDateTime toDate,
                                       Pageable pageable);
    Optional<WebhookEvent> findByStripeEventId(String stripeEventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from WebhookEvent e where e.stripeEventId = :eventId")
    Optional<WebhookEvent> findForUpdate(@Param("eventId") String eventId);
}
