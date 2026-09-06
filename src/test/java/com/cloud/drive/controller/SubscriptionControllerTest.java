package com.cloud.drive.controller;

import com.cloud.drive.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SubscriptionController(org.mockito.Mockito.mock(SubscriptionService.class)))
                .build();
    }

    @Test
    void authenticatedClientCannotUseLegacyPlanMutationEndpoint() throws Exception {
        mockMvc.perform(put("/api/subscriptions/plan")
                        .header("Authorization", "Bearer authenticated-user-token")
                        .contentType("application/json")
                        .content("{\"plan\":\"BUSINESS\"}"))
                .andExpect(status().isNotFound());
    }
}
