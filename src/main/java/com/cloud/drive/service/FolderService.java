package com.cloud.drive.service;

import com.cloud.drive.dto.folder.*;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Folder;
import com.cloud.drive.model.TeamMember;
import com.cloud.drive.repository.FolderRepository;
import com.cloud.drive.repository.TeamMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FolderService {
    private final FolderRepository folderRepository;
    private final TeamMemberRepository teamMemberRepository;

    public FolderService(FolderRepository folderRepository, TeamMemberRepository teamMemberRepository) {
        this.folderRepository = folderRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional
    public FolderResponse create(String email, CreateFolderRequest request) {
        authorizeScope(email, request.getTeamId(), false);
        validateParent(email, request.getTeamId(), request.getParentId());
        String name = request.getName().trim();
        if (name.isEmpty() || folderRepository.existsByUserIdAndTeamIdAndParentIdAndNameIgnoreCase(
                email, request.getTeamId(), request.getParentId(), name)) {
            throw new ApiException("A folder with this name already exists", HttpStatus.CONFLICT);
        }
        Folder folder = new Folder();
        folder.setName(name); folder.setUserId(email); folder.setTeamId(request.getTeamId());
        folder.setParentId(request.getParentId());
        folder.setCreatedAt(LocalDateTime.now()); folder.setUpdatedAt(LocalDateTime.now());
        return toResponse(folderRepository.save(folder));
    }

    public List<FolderResponse> list(String email, Long teamId) {
        authorizeScope(email, teamId, false);
        return (teamId == null ? folderRepository.findByUserIdAndTeamIdIsNullOrderByNameAsc(email)
                : folderRepository.findByTeamIdOrderByNameAsc(teamId)).stream().map(this::toResponse).toList();
    }

    @Transactional
    public FolderResponse update(Long id, String email, UpdateFolderRequest request) {
        Folder folder = authorizeFolder(id, email, true);
        validateParent(email, folder.getTeamId(), request.getParentId());
        if (id.equals(request.getParentId())) {
            throw new ApiException("A folder cannot contain itself", HttpStatus.BAD_REQUEST);
        }
        String name = request.getName().trim();
        if (name.isEmpty()) throw new ApiException("Folder name is required", HttpStatus.BAD_REQUEST);
        folder.setName(name); folder.setParentId(request.getParentId()); folder.setUpdatedAt(LocalDateTime.now());
        return toResponse(folderRepository.save(folder));
    }

    private Folder authorizeFolder(Long id, String email, boolean mutate) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ApiException("Folder not found", HttpStatus.NOT_FOUND));
        if (!email.equals(folder.getUserId())) {
            TeamMember member = folder.getTeamId() == null ? null : teamMemberRepository
                    .findByTeamIdAndUserEmail(folder.getTeamId(), email)
                    .filter(m -> "ACTIVE".equals(m.getStatus())).orElse(null);
            if (member == null || (mutate && !"OWNER".equals(member.getRole()) && !"ADMIN".equals(member.getRole()))) {
                throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
            }
        }
        return folder;
    }

    private void authorizeScope(String email, Long teamId, boolean mutate) {
        if (teamId != null) {
            TeamMember member = teamMemberRepository.findByTeamIdAndUserEmail(teamId, email)
                    .filter(m -> "ACTIVE".equals(m.getStatus())).orElseThrow(
                            () -> new ApiException("Not a member of this team", HttpStatus.FORBIDDEN));
            if (mutate && !"OWNER".equals(member.getRole()) && !"ADMIN".equals(member.getRole())) {
                throw new ApiException("Insufficient permissions", HttpStatus.FORBIDDEN);
            }
        }
    }

    private void validateParent(String email, Long teamId, Long parentId) {
        if (parentId == null) return;
        Folder parent = folderRepository.findById(parentId)
                .orElseThrow(() -> new ApiException("Parent folder not found", HttpStatus.NOT_FOUND));
        if ((teamId == null && (parent.getTeamId() != null || !email.equals(parent.getUserId())))
                || (teamId != null && !teamId.equals(parent.getTeamId()))) {
            throw new ApiException("Parent folder is outside the target scope", HttpStatus.FORBIDDEN);
        }
    }

    private FolderResponse toResponse(Folder f) {
        return new FolderResponse(f.getId(), f.getName(), f.getUserId(), f.getTeamId(), f.getParentId(),
                f.getCreatedAt(), f.getUpdatedAt());
    }
}
