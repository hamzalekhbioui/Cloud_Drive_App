package com.cloud.drive.service.admin;

import com.cloud.drive.model.User;
import com.cloud.drive.repository.UserRepository;
import com.cloud.drive.security.admin.AdminPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private AdminAuditService auditService;
    @InjectMocks private AdminUserService service;

    @Test
    void disable_recordsExactlyOneAuditRowBeforeMutation() {
        User user = new User();
        user.setId(5L);
        user.setStatus(User.STATUS_ACTIVE);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AdminPrincipal admin = new AdminPrincipal(1L, "admin@example.com", "Admin");

        service.disable(5L, admin, "10.0.0.1");

        verify(auditService).record(admin, AdminAuditActions.USER_DISABLE, "USER", "5",
                "{\"userId\":5}", "10.0.0.1");
        verify(auditService, times(1)).record(any(), any(), any(), any(), any(), any());
        verify(userRepository).save(user);
    }
}
