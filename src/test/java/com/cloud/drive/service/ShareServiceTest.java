package com.cloud.drive.service;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.dto.share.CreateShareRequest;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.model.FileShare;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.FileShareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareServiceTest {

    @Mock private FileShareRepository shareRepo;
    @Mock private FileRepository fileRepo;
    @Mock private BlobStorageService blobStorageService;

    @InjectMocks private ShareService shareService;

    private static final String OWNER = "alice@example.com";
    private static final String OTHER = "bob@example.com";

    private FileEntity file() {
        FileEntity file = new FileEntity();
        file.setId(10L);
        file.setUserId(OWNER);
        file.setOriginalFileName("report.pdf");
        file.setBlobFileName("blob-report.pdf");
        file.setSize(123L);
        file.setType("application/pdf");
        file.setCreatedAt(LocalDateTime.now());
        return file;
    }

    @Test
    void resolvePublicToken_throwsGone_whenShareExpired() {
        FileShare share = new FileShare();
        share.setFileId(10L);
        share.setToken("token-1");
        share.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(shareRepo.findByToken("token-1")).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> shareService.resolvePublicToken("token-1"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.GONE);

        verify(fileRepo, never()).findById(any());
    }

    @Test
    void resolvePublicToken_throwsGone_whenShareRevoked() {
        FileShare share = new FileShare();
        share.setFileId(10L);
        share.setToken("token-revoked");
        share.setRevokedAt(LocalDateTime.now().minusMinutes(1));
        when(shareRepo.findByToken("token-revoked")).thenReturn(Optional.of(share));

        assertThatThrownBy(() -> shareService.resolvePublicToken("token-revoked"))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.GONE);

        verify(fileRepo, never()).findById(any());
    }

    @Test
    void createShare_enforcesOneDayExpiration() {
        when(fileRepo.findById(10L)).thenReturn(Optional.of(file()));
        when(shareRepo.save(any(FileShare.class))).thenAnswer(i -> i.getArgument(0));

        CreateShareRequest req = new CreateShareRequest();
        req.setPermission("VIEW");
        // Even if requester tries to set a different date
        req.setExpiresAt(LocalDateTime.now().plusDays(10));

        var response = shareService.createShare(10L, OWNER, req);

        assertThat(response.getExpiresAt()).isNotNull();
        assertThat(response.getExpiresAt()).isBeforeOrEqualTo(LocalDateTime.now().plusDays(1).plusSeconds(5));
        assertThat(response.getExpiresAt()).isAfterOrEqualTo(LocalDateTime.now().plusDays(1).minusSeconds(5));
    }

    @Test
    void createShare_usesSecureToken() {
        when(fileRepo.findById(10L)).thenReturn(Optional.of(file()));
        when(shareRepo.save(any(FileShare.class))).thenAnswer(i -> i.getArgument(0));

        CreateShareRequest req = new CreateShareRequest();
        req.setPermission("VIEW");

        var response = shareService.createShare(10L, OWNER, req);

        assertThat(response.getToken()).hasSize(43); // 32 bytes base64url is 43 chars
        assertThat(response.getToken()).doesNotContain("+", "/", "=");
    }
    @Test
    void createShare_throwsForbidden_whenCallerIsNotOwner() {
        when(fileRepo.findById(10L)).thenReturn(Optional.of(file()));

        CreateShareRequest req = new CreateShareRequest();
        req.setSharedWithEmail("carol@example.com");
        req.setPermission("VIEW");

        assertThatThrownBy(() -> shareService.createShare(10L, OTHER, req))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(shareRepo, never()).save(any());
    }
}
