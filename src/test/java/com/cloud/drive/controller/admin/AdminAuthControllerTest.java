package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.AuthResponse;
import com.cloud.drive.model.AdminUser;
import com.cloud.drive.security.admin.AdminPrincipal;
import com.cloud.drive.service.admin.AdminAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private AdminAuthService adminAuthService;

    @InjectMocks
    private AdminAuthController controller;

    @Test
    void loginReturnsAdminTokenResponse() throws Exception {
        when(adminAuthService.login(any(), anyString()))
                .thenReturn(new AuthResponse("admin-token", "admin@example.com", "Admin"));
        MockMvc mvc = standaloneSetup(controller).build();

        mvc.perform(post("/api/admin/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"admin@example.com","password":"Password123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("admin-token"))
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void meReturnsAuthenticatedAdminDetails() throws Exception {
        AdminUser admin = new AdminUser();
        admin.setId(7L);
        admin.setEmail("admin@example.com");
        admin.setName("Admin");
        admin.setStatus(AdminUser.STATUS_ACTIVE);
        when(adminAuthService.me("admin@example.com")).thenReturn(admin);
        MockMvc mvc = standaloneSetup(controller).build();

        mvc.perform(get("/api/admin/auth/me")
                .principal(new UsernamePasswordAuthenticationToken(
                        new AdminPrincipal(7L, "admin@example.com", "Admin"), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
