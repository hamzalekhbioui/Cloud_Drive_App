package com.cloud.drive.service;

import com.cloud.drive.dto.team.*;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.Team;
import com.cloud.drive.model.TeamMember;
import com.cloud.drive.repository.TeamMemberRepository;
import com.cloud.drive.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepo;
    private final TeamMemberRepository memberRepo;

    public TeamService(TeamRepository teamRepo, TeamMemberRepository memberRepo) {
        this.teamRepo = teamRepo;
        this.memberRepo = memberRepo;
    }

    @Transactional
    public TeamResponse createTeam(String ownerEmail, CreateTeamRequest req) {
        Team team = new Team();
        team.setName(req.getName().trim());
        team.setOwnerEmail(ownerEmail);
        team.setCreatedAt(LocalDateTime.now());
        team = teamRepo.save(team);

        TeamMember owner = new TeamMember();
        owner.setTeamId(team.getId());
        owner.setUserEmail(ownerEmail);
        owner.setRole("OWNER");
        owner.setStatus("ACTIVE");
        owner.setInvitedAt(LocalDateTime.now());
        owner.setJoinedAt(LocalDateTime.now());
        memberRepo.save(owner);

        return toResponse(team, ownerEmail, List.of(owner));
    }

    public List<TeamResponse> getMyTeams(String userEmail) {
        List<Long> teamIds = memberRepo.findActiveTeamIdsByUserEmail(userEmail);
        List<Long> ownedIds = teamRepo.findByOwnerEmail(userEmail).stream()
                .map(Team::getId).collect(Collectors.toList());
        teamIds.addAll(ownedIds.stream().filter(id -> !teamIds.contains(id)).collect(Collectors.toList()));

        return teamIds.stream()
                .map(id -> teamRepo.findById(id).orElse(null))
                .filter(t -> t != null)
                .map(t -> {
                    List<TeamMember> members = memberRepo.findByTeamId(t.getId());
                    return toResponse(t, userEmail, members);
                })
                .collect(Collectors.toList());
    }

    public List<TeamMemberResponse> getMembers(Long teamId, String callerEmail) {
        requireMembership(teamId, callerEmail);
        return memberRepo.findByTeamId(teamId).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamMemberResponse inviteMember(Long teamId, String callerEmail, InviteMemberRequest req) {
        requireRoleAtLeast(teamId, callerEmail, "ADMIN");
        if (memberRepo.existsByTeamIdAndUserEmail(teamId, req.getEmail())) {
            throw new ApiException("User is already a member or has a pending invite", HttpStatus.CONFLICT);
        }
        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserEmail(req.getEmail());
        member.setRole(req.getRole() != null ? req.getRole() : "MEMBER");
        member.setInviteToken(UUID.randomUUID().toString());
        member.setStatus("PENDING");
        member.setInvitedAt(LocalDateTime.now());
        return toMemberResponse(memberRepo.save(member));
    }

    @Transactional
    public void acceptInvite(String inviteToken, String userEmail) {
        TeamMember member = memberRepo.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ApiException("Invalid invite token", HttpStatus.NOT_FOUND));
        if (!member.getUserEmail().equals(userEmail)) {
            throw new ApiException("This invite was sent to a different email address", HttpStatus.FORBIDDEN);
        }
        member.setStatus("ACTIVE");
        member.setJoinedAt(LocalDateTime.now());
        member.setInviteToken(null);
        memberRepo.save(member);
    }

    @Transactional
    public void removeMember(Long teamId, Long memberId, String callerEmail) {
        TeamMember target = memberRepo.findById(memberId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND));
        if (!target.getTeamId().equals(teamId)) {
            throw new ApiException("Member does not belong to this team", HttpStatus.BAD_REQUEST);
        }
        if ("OWNER".equals(target.getRole())) {
            throw new ApiException("Cannot remove the team owner", HttpStatus.FORBIDDEN);
        }
        boolean isSelf = target.getUserEmail().equals(callerEmail);
        if (!isSelf) {
            requireRoleAtLeast(teamId, callerEmail, "ADMIN");
        }
        memberRepo.delete(target);
    }

    @Transactional
    public void deleteTeam(Long teamId, String ownerEmail) {
        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ApiException("Team not found", HttpStatus.NOT_FOUND));
        if (!team.getOwnerEmail().equals(ownerEmail)) {
            throw new ApiException("Only the owner can delete a team", HttpStatus.FORBIDDEN);
        }
        memberRepo.deleteAll(memberRepo.findByTeamId(teamId));
        teamRepo.delete(team);
    }

    public List<TeamMemberResponse> getPendingInvites(String userEmail) {
        return memberRepo.findByUserEmailAndStatus(userEmail, "PENDING").stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void requireMembership(Long teamId, String email) {
        memberRepo.findByTeamIdAndUserEmail(teamId, email)
                .orElseThrow(() -> new ApiException("Not a member of this team", HttpStatus.FORBIDDEN));
    }

    private void requireRoleAtLeast(Long teamId, String email, String minRole) {
        TeamMember m = memberRepo.findByTeamIdAndUserEmail(teamId, email)
                .orElseThrow(() -> new ApiException("Not a member of this team", HttpStatus.FORBIDDEN));
        int callerRank  = roleRank(m.getRole());
        int requiredRank = roleRank(minRole);
        if (callerRank < requiredRank) {
            throw new ApiException("Insufficient permissions", HttpStatus.FORBIDDEN);
        }
    }

    private int roleRank(String role) {
        return switch (role) {
            case "OWNER" -> 3;
            case "ADMIN" -> 2;
            default      -> 1;
        };
    }

    private TeamResponse toResponse(Team team, String callerEmail, List<TeamMember> members) {
        TeamResponse r = new TeamResponse();
        r.setId(team.getId());
        r.setName(team.getName());
        r.setOwnerEmail(team.getOwnerEmail());
        r.setCreatedAt(team.getCreatedAt());
        r.setMemberCount((int) members.stream().filter(m -> "ACTIVE".equals(m.getStatus())).count());
        r.setCallerRole(members.stream()
                .filter(m -> m.getUserEmail().equals(callerEmail))
                .map(TeamMember::getRole).findFirst().orElse("MEMBER"));
        r.setMembers(members.stream().map(this::toMemberResponse).collect(Collectors.toList()));
        return r;
    }

    private TeamMemberResponse toMemberResponse(TeamMember m) {
        TeamMemberResponse r = new TeamMemberResponse();
        r.setId(m.getId());
        r.setTeamId(m.getTeamId());
        r.setUserEmail(m.getUserEmail());
        r.setRole(m.getRole());
        r.setStatus(m.getStatus());
        r.setInviteToken(m.getInviteToken());
        r.setInvitedAt(m.getInvitedAt());
        r.setJoinedAt(m.getJoinedAt());
        return r;
    }
}