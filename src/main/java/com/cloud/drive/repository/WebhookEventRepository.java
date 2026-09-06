package com.cloud.drive.repository;

import com.cloud.drive.model.WebhookEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    Optional<WebhookEvent> findByStripeEventId(String stripeEventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from WebhookEvent e where e.stripeEventId = :eventId")
    Optional<WebhookEvent> findForUpdate(@Param("eventId") String eventId);
}
