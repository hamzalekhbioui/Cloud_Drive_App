package com.cloud.drive.service.admin;

import com.cloud.drive.model.AdminAuditLog;
import com.cloud.drive.repository.AdminAuditLogRepository;
import com.cloud.drive.security.admin.AdminPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {
    @Mock private AdminAuditLogRepository repository;

    @Test
    void record_persistsActingAdminActionTargetDetailAndIp() {
        AdminAuditService service = new AdminAuditService(repository);
        AdminPrincipal admin = new AdminPrincipal(7L, "admin@example.com", "Admin");
        when(repository.save(any(AdminAuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.record(admin, AdminAuditActions.FILE_PURGE, "FILE", "42",
                "{\"fileId\":42}", "127.0.0.1");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(repository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAdminId()).isEqualTo(7L);
        assertThat(saved.getAdminEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getAction()).isEqualTo(AdminAuditActions.FILE_PURGE);
        assertThat(saved.getTargetType()).isEqualTo("FILE");
        assertThat(saved.getTargetId()).isEqualTo("42");
        assertThat(saved.getDetail()).isEqualTo("{\"fileId\":42}");
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
