package com.cloud.drive.service;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock private BlobStorageService blobStorageService;
    @Mock private FileRepository fileRepository;
    @Mock private SubscriptionService subscriptionService;

    @InjectMocks private FileService fileService;

    private static final String OWNER = "alice@example.com";
    private static final String OTHER_USER = "bob@example.com";

    private FileEntity ownedFile() {
        FileEntity f = new FileEntity();
        f.setId(42L);
        f.setUserId(OWNER);
        f.setOriginalFileName("report.pdf");
        f.setBlobFileName("uuid-report.pdf");
        f.setUrl("https://blob/old-url");
        f.setSize(1024L);
        f.setType("application/pdf");
        f.setCreatedAt(LocalDateTime.now());
        f.setStarred(false);
        return f;
    }

    @Test
    void uploadFile_persistsEntityWithFreshSasUrl() throws IOException {
        MockMultipartFile multipart = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "data".getBytes());
        when(blobStorageService.uploadFile(any(), anyString())).thenReturn("https://blob/sas-url");
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FileResponseDto dto = fileService.uploadFile(multipart, OWNER);

        assertThat(dto.getOriginalFileName()).isEqualTo("report.pdf");
        assertThat(dto.getUrl()).isEqualTo("https://blob/sas-url");
        assertThat(dto.getType()).isEqualTo("application/pdf");
        verify(fileRepository).save(argThat(fe ->
                fe.getUserId().equals(OWNER)
                        && fe.getBlobFileName().endsWith("-report.pdf")
                        && fe.getCreatedAt() != null));
    }

    @Test
    void getFilesByUser_regeneratesSasUrlPerFile() {
        FileEntity file = ownedFile();
        when(fileRepository.findByUserIdAndDeletedAtIsNull(OWNER)).thenReturn(List.of(file));
        when(blobStorageService.generateSasUrlForBlob("uuid-report.pdf"))
                .thenReturn("https://blob/fresh-url");

        List<FileResponseDto> result = fileService.getFilesByUser(OWNER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUrl()).isEqualTo("https://blob/fresh-url");
        verify(blobStorageService).generateSasUrlForBlob("uuid-report.pdf");
    }

    @Test
    void deleteFile_setsDeletedAt() {
        FileEntity file = ownedFile();
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));

        fileService.deleteFile(42L, OWNER);

        assertThat(file.getDeletedAt()).isNotNull();
        verify(fileRepository).save(file);
    }

    @Test
    void deleteFile_throwsForbidden_whenNotOwner() {
        FileEntity file = ownedFile();
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> fileService.deleteFile(42L, OTHER_USER))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(fileRepository, never()).save(any());
    }

    @Test
    void deleteFile_throwsNotFound_whenMissing() {
        when(fileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.deleteFile(99L, OWNER))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    void restoreFile_clearsDeletedAt() {
        FileEntity file = ownedFile();
        file.setDeletedAt(LocalDateTime.now());
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));

        fileService.restoreFile(42L, OWNER);

        assertThat(file.getDeletedAt()).isNull();
        verify(fileRepository).save(file);
    }

    @Test
    void permanentlyDelete_removesBlobAndEntity() {
        FileEntity file = ownedFile();
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));

        fileService.permanentlyDeleteFile(42L, OWNER);

        verify(blobStorageService).deleteFile("uuid-report.pdf");
        verify(fileRepository).delete(file);
    }

    @Test
    void toggleStar_flipsValue() {
        FileEntity file = ownedFile();
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FileResponseDto first = fileService.toggleStar(42L, OWNER);
        assertThat(first.isStarred()).isTrue();

        FileResponseDto second = fileService.toggleStar(42L, OWNER);
        assertThat(second.isStarred()).isFalse();
    }
}
