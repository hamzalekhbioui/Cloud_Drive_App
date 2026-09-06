package com.cloud.drive.repository;

import com.cloud.drive.model.UsageTracking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UsageTrackingRepository extends JpaRepository<UsageTracking, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from UsageTracking u
            where u.userEmail = :email and u.resourceType = :resourceType
              and u.periodStart = :periodStart
            """)
    Optional<UsageTracking> findForUpdate(@Param("email") String email,
                                          @Param("resourceType") String resourceType,
                                          @Param("periodStart") LocalDate periodStart);
}
