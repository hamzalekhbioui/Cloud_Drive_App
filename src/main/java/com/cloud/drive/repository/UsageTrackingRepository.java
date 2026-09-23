package com.cloud.drive.repository;

import com.cloud.drive.model.UsageTracking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface UsageTrackingRepository extends JpaRepository<UsageTracking, Long> {
    @Query("""
            select u from UsageTracking u
            where (:email is null or lower(u.userEmail) like lower(concat('%', :email, '%')))
              and (:plan is null or exists (select s.id from Subscription s where s.userEmail = u.userEmail and upper(s.plan) = upper(:plan)))
              and (:fromDate is null or u.periodStart >= :fromDate)
              and (:toDate is null or u.periodStart <= :toDate)
            """)
    Page<UsageTracking> findAllForAdmin(@Param("email") String email,
                                        @Param("plan") String plan,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate,
                                        Pageable pageable);

    void deleteByUserEmail(String userEmail);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from UsageTracking u
            where u.userEmail = :email and u.resourceType = :resourceType
              and u.periodStart = :periodStart
            """)
    Optional<UsageTracking> findForUpdate(@Param("email") String email,
                                          @Param("resourceType") String resourceType,
                                          @Param("periodStart") LocalDate periodStart);

    Optional<UsageTracking> findByUserEmailAndResourceTypeAndPeriodStart(
            String userEmail, String resourceType, LocalDate periodStart);
}
