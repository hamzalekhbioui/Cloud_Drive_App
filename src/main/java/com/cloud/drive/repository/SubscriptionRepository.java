package com.cloud.drive.repository;

import com.cloud.drive.model.Subscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * Repairs every quota counter with one grouped aggregate/update instead of
     * loading all subscriptions and issuing one SUM query per account.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE subscriptions s
               SET used_bytes = totals.actual_bytes
              FROM (
                    SELECT sub.id, COALESCE(SUM(f.size), 0) AS actual_bytes
                      FROM subscriptions sub
                      LEFT JOIN files f ON f.user_id = sub.user_email
                     GROUP BY sub.id
                   ) totals
             WHERE s.id = totals.id
               AND s.used_bytes IS DISTINCT FROM totals.actual_bytes
            """, nativeQuery = true)
    int reconcileUsedBytesFromFiles();

    @Query("""
            select s from Subscription s
            where (:status is null or s.status = :status)
              and (:plan is null or upper(s.plan) = upper(:plan))
              and (:fromDate is null or s.startDate >= :fromDate)
              and (:toDate is null or s.startDate <= :toDate)
            """)
    Page<Subscription> findAllForAdmin(@Param("status") String status,
                                       @Param("plan") String plan,
                                       @Param("fromDate") java.time.LocalDateTime fromDate,
                                       @Param("toDate") java.time.LocalDateTime toDate,
                                       Pageable pageable);

    /** Normal read — no lock. */
    Optional<Subscription> findByUserEmail(String userEmail);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);

    @Query("SELECT s.plan, COUNT(s) FROM Subscription s WHERE s.status IN (com.cloud.drive.model.SubscriptionStatus.ACTIVE, com.cloud.drive.model.SubscriptionStatus.TRIALING) GROUP BY s.plan ORDER BY s.plan")
    List<Object[]> countActiveByPlan();

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
