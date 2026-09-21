package com.cloud.drive.service;

import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileShare;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.FileShareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareServiceRevocationTest {
    @Mock private FileShareRepository shareRepository;
    @Mock private FileRepository fileRepository;
    @Mock private BlobStorageService blobStorage;

    @Test
    void revokedShare_isNoLongerPubliclyResolvable() {
        FileShare share = new FileShare();
        share.setToken("revoked-token");
        share.setRevokedAt(LocalDateTime.now());
        when(shareRepository.findByToken("revoked-token")).thenReturn(Optional.of(share));
        ShareService service = new ShareService(shareRepository, fileRepository, blobStorage);

        assertThatThrownBy(() -> service.resolvePublicToken("revoked-token"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("revoked");
    }
}
