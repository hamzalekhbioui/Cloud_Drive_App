package com.cloud.drive.service.admin;

import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.*;
import com.cloud.drive.service.BlobStorageService;
import com.cloud.drive.security.admin.AdminPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminFileServiceTest {
    @Mock private FileRepository fileRepository;
    @Mock private FileShareRepository shareRepository;
    @Mock private FileAiProcessingRepository aiRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private BlobStorageService blobStorageService;
    @Mock private AdminAuditService auditService;

    @InjectMocks private AdminFileService service;

    @Test
    void softDeleteAndRestore_areGlobalAndUpdateDeletedAt() {
        FileEntity file = file();
        when(fileRepository.findById(7L)).thenReturn(Optional.of(file));
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.softDelete(7L);
        assertThat(file.getDeletedAt()).isNotNull();
        service.restore(7L);
        assertThat(file.getDeletedAt()).isNull();
    }

    @Test
    void purge_removesBlobAiSharesAndFile() {
        FileEntity file = file();
        when(fileRepository.findById(7L)).thenReturn(Optional.of(file));

        service.purge(7L);

        verify(blobStorageService).deleteFile("blob-key");
        verify(aiRepository).deleteById(7L);
        verify(shareRepository).deleteByFileId(7L);
        verify(fileRepository).delete(file);
    }

    @Test
    void purge_recordsExactlyOneAuditRowBeforeMutation() {
        FileEntity file = file();
        AdminPrincipal admin = new AdminPrincipal(1L, "admin@example.com", "Admin");
        when(fileRepository.findById(7L)).thenReturn(Optional.of(file));

        service.purge(7L, admin, "10.0.0.1");

        verify(auditService).record(admin, AdminAuditActions.FILE_PURGE, "FILE", "7",
                "{\"fileId\":7}", "10.0.0.1");
        verify(auditService, times(1)).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void missingFile_isRejected() {
        when(fileRepository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.purge(7L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void listFiles_batchLoadsRelatedRowsForTheWholePage() {
        FileEntity first = file();
        FileEntity second = file();
        second.setId(8L);
        second.setOriginalFileName("photo.png");
        PageRequest pageable = PageRequest.of(0, 20);
        when(fileRepository.findAllForAdmin(isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
        when(aiRepository.findAllById(List.of(7L, 8L))).thenReturn(List.of());
        when(shareRepository.findByFileIdInOrderByCreatedAtDesc(List.of(7L, 8L))).thenReturn(List.of());

        var result = service.listFiles(null, null, null, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        verify(aiRepository).findAllById(List.of(7L, 8L));
        verify(shareRepository).findByFileIdInOrderByCreatedAtDesc(List.of(7L, 8L));
        verify(shareRepository, never()).findByFileIdOrderByCreatedAtDesc(anyLong());
        verify(fileRepository, never()).findById(anyLong());
    }

    private FileEntity file() {
        FileEntity file = new FileEntity();
        file.setId(7L);
        file.setOriginalFileName("report.pdf");
        file.setUserId("owner@example.com");
        file.setBlobFileName("blob-key");
        file.setSize(10L);
        return file;
    }
}
