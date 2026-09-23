package com.cloud.drive.repository;

import com.cloud.drive.model.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    @Query("""
            select a from AdminAuditLog a
            where (:admin is null or lower(a.adminEmail) like lower(concat('%', :admin, '%')))
              and (:action is null or a.action = :action)
              and (:targetType is null or a.targetType = :targetType)
              and (:fromDate is null or a.createdAt >= :fromDate)
              and (:toDate is null or a.createdAt <= :toDate)
            """)
    Page<AdminAuditLog> findAllForAdmin(@Param("admin") String admin,
                                        @Param("action") String action,
                                        @Param("targetType") String targetType,
                                        @Param("fromDate") java.time.LocalDateTime fromDate,
                                        @Param("toDate") java.time.LocalDateTime toDate,
                                        Pageable pageable);
}
