package com.cloud.drive.service;

import com.cloud.drive.dto.FileResponseDto;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.FileEntity;
import com.cloud.drive.model.TeamMember;
import com.cloud.drive.repository.FileRepository;
import com.cloud.drive.repository.FolderRepository;
import com.cloud.drive.repository.FileAiProcessingRepository;
import com.cloud.drive.repository.TeamMemberRepository;
import com.cloud.drive.storage.StorageService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock private BlobStorageService blobStorageService;
    @Mock private FileRepository fileRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private StorageService storageService;
    @Mock private UploadCompensationService uploadCompensationService;
    @Mock private FileAiProcessingRepository aiProcessingRepository;

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
                "file", "report.pdf", "application/pdf", "%PDF-1.4\n%âãÏÓ\n".getBytes());
        when(blobStorageService.uploadFile(any(), anyString(), anyString())).thenReturn("https://blob/sas-url");
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
        PageRequest pageable = PageRequest.of(0, 50);
        when(fileRepository.findVisibleByUser(OWNER, "", pageable))
                .thenReturn(new PageImpl<>(List.of(file), pageable, 1));
        when(blobStorageService.generateSasUrlForBlob("uuid-report.pdf"))
                .thenReturn("https://blob/fresh-url");

        Page<FileResponseDto> result = fileService.getFilesByUser(OWNER, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUrl()).isEqualTo("https://blob/fresh-url");
        verify(blobStorageService).generateSasUrlForBlob("uuid-report.pdf");
        verify(aiProcessingRepository).findAllById(List.of(42L));
        verify(aiProcessingRepository, never()).findById(42L);
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
    void purgeExpiredTrash_removesOldTrashedFileAndReleasesQuota() {
        FileEntity file = ownedFile();
        file.setDeletedAt(LocalDateTime.now().minusDays(31));

        fileService.purgeExpiredTrash(file);

        verify(blobStorageService).deleteFile("uuid-report.pdf");
        verify(fileRepository).delete(file);
        verify(subscriptionService).releaseQuota(OWNER, 1024L);
    }

    @Test
    void purgeExpiredTrash_doesNothingForActiveFile() {
        FileEntity file = ownedFile();

        fileService.purgeExpiredTrash(file);

        verifyNoInteractions(blobStorageService, fileRepository, subscriptionService);
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

    @Test
    void teamMember_canReadAnotherMembersFile_butCannotDeleteIt() throws IOException {
        FileEntity file = ownedFile();
        file.setTeamId(8L);
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));
        TeamMember member = new TeamMember();
        member.setTeamId(8L);
        member.setUserEmail(OTHER_USER);
        member.setRole("MEMBER");
        member.setStatus("ACTIVE");
        when(teamMemberRepository.findByTeamIdAndUserEmail(8L, OTHER_USER)).thenReturn(Optional.of(member));

        assertThat(fileService.findOwnedForStream(42L, OTHER_USER)).isSameAs(file);
        assertThatThrownBy(() -> fileService.deleteFile(42L, OTHER_USER))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
        verify(fileRepository, never()).save(file);
    }

    @Test
    void teamAdmin_canDeleteAnotherMembersFile() {
        FileEntity file = ownedFile();
        file.setTeamId(8L);
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));
        TeamMember admin = new TeamMember();
        admin.setTeamId(8L);
        admin.setUserEmail(OTHER_USER);
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        when(teamMemberRepository.findByTeamIdAndUserEmail(8L, OTHER_USER)).thenReturn(Optional.of(admin));

        fileService.deleteFile(42L, OTHER_USER);

        assertThat(file.getDeletedAt()).isNotNull();
        verify(fileRepository).save(file);
    }

    @Test
    void inactiveTeamMember_cannotReadTeamFile() {
        FileEntity file = ownedFile();
        file.setTeamId(8L);
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));
        TeamMember member = new TeamMember();
        member.setTeamId(8L);
        member.setUserEmail(OTHER_USER);
        member.setRole("MEMBER");
        member.setStatus("PENDING");
        when(teamMemberRepository.findByTeamIdAndUserEmail(8L, OTHER_USER)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> fileService.findOwnedForStream(42L, OTHER_USER))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void commitUpload_verifiesContentBeforeActivating() {
        FileEntity pending = ownedFile();
        pending.setStatus("PENDING");
        pending.setSize(1024L);
        pending.setType("application/pdf");
        when(fileRepository.findById(42L)).thenReturn(Optional.of(pending));
        when(fileRepository.save(any(FileEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FileResponseDto result = fileService.commitUpload(42L, OWNER);

        assertThat(result.getType()).isEqualTo("application/pdf");
        assertThat(pending.getStatus()).isEqualTo("ACTIVE");
        verify(storageService).assertLength("uuid-report.pdf", 1024L);
        verify(storageService).verifyContentAndSetHeaders(
                "uuid-report.pdf", "application/pdf", "report.pdf");
        verify(storageService, never()).delete(anyString());
    }

    @Test
    void commitUpload_rejectsContentMismatchAndReleasesReservation() {
        FileEntity pending = ownedFile();
        pending.setStatus("PENDING");
        pending.setSize(1024L);
        pending.setType("application/pdf");
        when(fileRepository.findById(42L)).thenReturn(Optional.of(pending));
        doThrow(new ApiException("Uploaded content does not match the declared file type",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE))
                .when(storageService)
                .verifyContentAndSetHeaders("uuid-report.pdf", "application/pdf", "report.pdf");

        assertThatThrownBy(() -> fileService.commitUpload(42L, OWNER))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        verify(storageService).delete("uuid-report.pdf");
        verify(uploadCompensationService).rejectPendingUpload(42L, OWNER, 1024L);
        verify(fileRepository, never()).save(any(FileEntity.class));
    }
}
