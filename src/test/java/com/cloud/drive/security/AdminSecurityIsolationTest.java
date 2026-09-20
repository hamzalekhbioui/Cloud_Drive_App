package com.cloud.drive.security;

import com.cloud.drive.model.AdminUser;
import com.cloud.drive.repository.AdminUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityIsolationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void seedAdmin() {
        adminUserRepository.deleteAll();
        AdminUser admin = new AdminUser();
        admin.setEmail("isolation-admin@example.com");
        admin.setName("Isolation Admin");
        admin.setPassword(passwordEncoder.encode("Password123!"));
        admin.setStatus(AdminUser.STATUS_ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());
        adminUserRepository.save(admin);
    }

    @Test
    void tenantTokenIsRejectedByAdminChain() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"isolation-tenant@example.com","name":"Tenant","password":"Password123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tenantToken = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/admin/auth/me")
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminTokenIsRejectedByTenantChain() throws Exception {
        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"isolation-admin@example.com","password":"Password123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);

        mockMvc.perform(get("/api/files")
                        .header("Authorization", "Bearer " + body.get("token").asText()))
                .andExpect(status().isUnauthorized());
    }
}
