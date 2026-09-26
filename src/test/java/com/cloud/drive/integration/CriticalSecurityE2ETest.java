package com.cloud.drive.integration;

import com.azure.storage.blob.BlobServiceClient;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end security coverage against the same PostgreSQL migrations and Azure
 * Blob protocol used in production. No storage or repository mocks are involved.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(parallel = true)
class CriticalSecurityE2ETest {

    private static final String STORAGE_ACCOUNT = "cloudtest";
    private static final String STORAGE_KEY = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                    .getBytes(StandardCharsets.UTF_8));
    private static final String CONTAINER_NAME = "e2e-files";
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.4-alpine")
            .withDatabaseName("clouddrive_e2e")
            .withUsername("clouddrive")
            .withPassword("clouddrive");

    @Container
    static final GenericContainer<?> AZURITE = new GenericContainer<>(
            DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.35.0"))
            .withEnv("AZURITE_ACCOUNTS", STORAGE_ACCOUNT + ":" + STORAGE_KEY)
            .withCommand("azurite-blob", "--blobHost", "0.0.0.0", "--blobPort", "10000",
                    "--skipApiVersionCheck")
            .withExposedPorts(10000)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("azure.storage.enabled", () -> "true");
        registry.add("azure.storage.connection-string", CriticalSecurityE2ETest::azuriteConnectionString);
        registry.add("azure.storage.container-name", () -> CONTAINER_NAME);
        registry.add("admin.bootstrap.enabled", () -> "false");
        registry.add("app.upload.cleanup-delay-ms", () -> "3600000");
        registry.add("app.trash.cleanup-initial-delay-ms", () -> "3600000");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private FileRepository fileRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private BlobServiceClient blobServiceClient;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE users, files, teams, subscriptions RESTART IDENTITY CASCADE");
    }

    @Test
    void directUploadRunsRealMigrationsAndRoundTripsThroughAzurite() throws Exception {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class)).isEqualTo("17");

        String owner = register("owner@example.com");
        UploadedFile uploaded = upload(owner, "pixel.png", PNG, null);

        FileEntity stored = fileRepository.findById(uploaded.id()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo("ACTIVE");
        assertThat(stored.getUrl()).contains("sig=");

        var blob = blobServiceClient.getBlobContainerClient(CONTAINER_NAME)
                .getBlobClient(uploaded.blobKey());
        assertThat(blob.exists()).isTrue();
        assertThat(blob.getProperties().getContentType()).isEqualTo("image/png");
        assertThat(blob.getProperties().getContentDisposition()).isEqualTo("attachment");
        assertThat(blob.getProperties().getCacheControl()).isEqualTo("no-store");

        MvcResult streaming = mockMvc.perform(get("/api/files/{id}/stream", uploaded.id())
                        .header("Authorization", bearer(owner)))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(streaming))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG));
    }

    @Test
    void forgedUploadSizeIsRejectedAndCompensatedInStorageAndQuota() throws Exception {
        String owner = register("owner@example.com");
        JsonNode target = beginUpload(owner, "forged.txt", 12, null);
        putBlob(target.get("writeUrl").asText(), "short".getBytes(StandardCharsets.UTF_8), "text/plain");

        mockMvc.perform(post("/api/files/upload/{id}/commit", target.get("uploadId").asLong())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isBadRequest());

        assertThat(fileRepository.findById(target.get("uploadId").asLong())).isEmpty();
        assertThat(subscriptionRepository.findByUserEmail("owner@example.com").orElseThrow().getUsedBytes())
                .isZero();
        assertThat(blobServiceClient.getBlobContainerClient(CONTAINER_NAME)
                .getBlobClient(target.get("blobKey").asText()).exists()).isFalse();
    }

    @Test
    void unrelatedUserCannotReadMutateOrShareAnotherUsersFile() throws Exception {
        String owner = register("owner@example.com");
        String attacker = register("attacker@example.com");
        UploadedFile uploaded = upload(owner, "private.png", PNG, null);

        mockMvc.perform(get("/api/files/{id}/stream", uploaded.id())
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/files/{id}", uploaded.id())
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/files/{id}/permanent", uploaded.id())
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/files/{id}/star", uploaded.id())
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/documents/{id}/shares", uploaded.id())
                        .header("Authorization", bearer(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permission\":\"DOWNLOAD\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/files/me"))
                .andExpect(status().isUnauthorized());

        assertThat(fileRepository.findById(uploaded.id())).isPresent();
    }

    @Test
    void teamMemberCanReadButCannotPerformOwnerOrAdminFileActions() throws Exception {
        String owner = register("owner@example.com");
        String member = register("member@example.com");

        JsonNode team = json(mockMvc.perform(post("/api/teams")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Security team\"}"))
                .andExpect(status().isCreated()).andReturn());
        long teamId = team.get("id").asLong();

        JsonNode invite = json(mockMvc.perform(post("/api/teams/{id}/members", teamId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated()).andReturn());
        mockMvc.perform(post("/api/teams/invites/{token}/accept", invite.get("inviteToken").asText())
                        .header("Authorization", bearer(member)))
                .andExpect(status().isNoContent());

        UploadedFile uploaded = upload(owner, "team.png", PNG, teamId);
        MvcResult streaming = mockMvc.perform(get("/api/files/{id}/stream", uploaded.id())
                        .header("Authorization", bearer(member)))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(streaming))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG));

        mockMvc.perform(delete("/api/files/{id}", uploaded.id())
                        .header("Authorization", bearer(member)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/files/{id}/star", uploaded.id())
                        .header("Authorization", bearer(member)))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewShareDoesNotLeakCredentialsAndCannotBeDownloadedOrUsedByAnotherRecipient() throws Exception {
        String owner = register("owner@example.com");
        String recipient = register("recipient@example.com");
        String attacker = register("attacker@example.com");
        UploadedFile uploaded = upload(owner, "shared.png", PNG, null);

        JsonNode share = json(mockMvc.perform(post("/api/documents/{id}/shares", uploaded.id())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sharedWithEmail\":\"recipient@example.com\",\"permission\":\"VIEW\"}"))
                .andExpect(status().isCreated()).andReturn());
        long shareId = share.get("id").asLong();
        String token = share.get("token").asText();

        MvcResult recipientList = mockMvc.perform(get("/api/shares/shared-with-me")
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shareId))
                .andExpect(jsonPath("$[0].permission").value("VIEW"))
                .andExpect(jsonPath("$[0].token").doesNotExist())
                .andExpect(jsonPath("$[0].url").doesNotExist())
                .andReturn();
        assertThat(recipientList.getResponse().getContentAsString())
                .doesNotContain(token)
                .doesNotContain("sig=");

        mockMvc.perform(get("/api/shares/shared-with-me/{id}/stream", shareId)
                        .param("download", "true")
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/shares/shared-with-me/{id}/stream", shareId)
                        .header("Authorization", bearer(attacker)))
                .andExpect(status().isForbidden());

        MvcResult publicMetadata = mockMvc.perform(get("/public/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permission").value("VIEW"))
                .andExpect(jsonPath("$.url").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.blobKey").doesNotExist())
                .andReturn();
        assertThat(publicMetadata.getResponse().getContentAsString())
                .doesNotContain("sig=")
                .doesNotContain(uploaded.blobKey());

        mockMvc.perform(get("/public/{token}/stream", token).param("download", "true"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/public/{token}/stream", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"shared.png\""))
                .andExpect(content().bytes(PNG));

        mockMvc.perform(delete("/api/documents/{id}/shares", uploaded.id())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/public/{token}", token))
                .andExpect(status().isGone());
    }

    private String register(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", email.substring(0, email.indexOf('@')),
                "email", email,
                "password", "Password123!"));
        JsonNode response = json(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn());
        return response.get("token").asText();
    }

    private UploadedFile upload(String token, String fileName, byte[] bytes, Long teamId) throws Exception {
        JsonNode target = beginUpload(token, fileName, bytes.length, teamId);
        String contentType = fileName.endsWith(".png") ? "image/png" : "text/plain";
        putBlob(target.get("writeUrl").asText(), bytes, contentType);
        JsonNode committed = json(mockMvc.perform(post("/api/files/upload/{id}/commit",
                                target.get("uploadId").asLong())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn());
        return new UploadedFile(committed.get("id").asLong(), target.get("blobKey").asText());
    }

    private JsonNode beginUpload(String token, String fileName, long size, Long teamId) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "rawFileName", fileName,
                "size", size,
                "teamId", teamId == null ? "" : teamId));
        if (teamId == null) {
            body = objectMapper.writeValueAsString(Map.of("rawFileName", fileName, "size", size));
        }
        return json(mockMvc.perform(post("/api/files/upload/begin")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.writeUrl").isNotEmpty())
                .andReturn());
    }

    private void putBlob(String writeUrl, byte[] bytes, String contentType) throws Exception {
        HttpResponse<Void> response = httpClient.send(HttpRequest.newBuilder(URI.create(writeUrl))
                        .timeout(Duration.ofSeconds(20))
                        .header("x-ms-blob-type", "BlockBlob")
                        .header("Content-Type", contentType)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isEqualTo(201);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String azuriteConnectionString() {
        return "DefaultEndpointsProtocol=http;AccountName=" + STORAGE_ACCOUNT
                + ";AccountKey=" + STORAGE_KEY
                + ";BlobEndpoint=http://" + AZURITE.getHost() + ":" + AZURITE.getMappedPort(10000)
                + "/" + STORAGE_ACCOUNT + ";";
    }

    private record UploadedFile(long id, String blobKey) {}
}
