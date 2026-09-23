package com.cloud.drive.repository;

import com.cloud.drive.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("""
            select p from Payment p
            where (:status is null or upper(p.status) = upper(:status))
              and (:plan is null or upper(p.subscription.plan) = upper(:plan))
              and (:fromDate is null or p.createdAt >= :fromDate)
              and (:toDate is null or p.createdAt <= :toDate)
            """)
    Page<Payment> findAllForAdmin(@Param("status") String status,
                                  @Param("plan") String plan,
                                  @Param("fromDate") java.time.LocalDateTime fromDate,
                                  @Param("toDate") java.time.LocalDateTime toDate,
                                  Pageable pageable);
    Optional<Payment> findByStripeInvoiceId(String stripeInvoiceId);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    @Query("SELECT COALESCE(SUM(p.amountCents), 0) FROM Payment p WHERE p.status IN ('SUCCEEDED', 'PAID')")
    Long sumSuccessfulAmountCents();
}
