package com.cloud.drive.service.admin;

import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.*;
import com.cloud.drive.service.BlobStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
    void missingFile_isRejected() {
        when(fileRepository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.purge(7L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("File not found");
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
