package com.cloud.drive.controller.admin;

import com.cloud.drive.dto.admin.team.AdminTeamDto;
import com.cloud.drive.dto.team.TeamMemberResponse;
import com.cloud.drive.model.Team;
import com.cloud.drive.model.TeamMember;
import com.cloud.drive.repository.TeamMemberRepository;
import com.cloud.drive.repository.TeamRepository;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teams")
public class AdminTeamController {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;

    public AdminTeamController(TeamRepository teamRepository, TeamMemberRepository memberRepository) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
    }

    @GetMapping
    public Page<AdminTeamDto> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return teamRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toDto);
    }

    @GetMapping("/{teamId}")
    public AdminTeamDto detail(@PathVariable Long teamId) {
        return teamRepository.findById(teamId).map(this::toDto)
                .orElseThrow(() -> new com.cloud.drive.exception.ApiException("Team not found", org.springframework.http.HttpStatus.NOT_FOUND));
    }

    private AdminTeamDto toDto(Team team) {
        AdminTeamDto dto = new AdminTeamDto();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setOwnerEmail(team.getOwnerEmail());
        dto.setCreatedAt(team.getCreatedAt());
        dto.setMembers(memberRepository.findByTeamId(team.getId()).stream().map(this::toMember).toList());
        return dto;
    }

    private TeamMemberResponse toMember(TeamMember member) {
        TeamMemberResponse dto = new TeamMemberResponse();
        dto.setId(member.getId());
        dto.setTeamId(member.getTeamId());
        dto.setUserEmail(member.getUserEmail());
        dto.setRole(member.getRole());
        dto.setStatus(member.getStatus());
        dto.setInvitedAt(member.getInvitedAt());
        dto.setJoinedAt(member.getJoinedAt());
        return dto;
    }
}
