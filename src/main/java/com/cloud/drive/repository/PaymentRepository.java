package com.cloud.drive.repository;

import com.cloud.drive.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripeInvoiceId(String stripeInvoiceId);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
