package com.cloud.drive.repository;

import com.cloud.drive.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripeInvoiceId(String stripeInvoiceId);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    @Query("SELECT COALESCE(SUM(p.amountCents), 0) FROM Payment p WHERE p.status IN ('SUCCEEDED', 'PAID')")
    Long sumSuccessfulAmountCents();
}
