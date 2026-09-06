package com.cloud.drive.repository;

import com.cloud.drive.model.Subscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /** Normal read — no lock. */
    Optional<Subscription> findByUserEmail(String userEmail);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);

    /**
     * Acquire a row-level exclusive lock (SELECT … FOR UPDATE) on the subscription.
     * The lock is held until the enclosing transaction commits, preventing concurrent
     * uploads from reading the same {@code usedBytes} value (TOCTOU race).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.userEmail = :email")
    Optional<Subscription> findForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.stripeCustomerId = :customerId")
    Optional<Subscription> findForUpdateByStripeCustomerId(@Param("customerId") String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.stripeSubscriptionId = :stripeSubscriptionId")
    Optional<Subscription> findForUpdateByStripeSubscriptionId(
            @Param("stripeSubscriptionId") String stripeSubscriptionId);
}