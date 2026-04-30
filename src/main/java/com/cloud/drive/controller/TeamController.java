package com.cloud.drive.controller;

import com.cloud.drive.dto.team.*;
import com.cloud.drive.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse createTeam(
            @Valid @RequestBody CreateTeamRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return teamService.createTeam(ud.getUsername(), req);
    }

    @GetMapping
    public List<TeamResponse> getMyTeams(@AuthenticationPrincipal UserDetails ud) {
        return teamService.getMyTeams(ud.getUsername());
    }

    @GetMapping("/{teamId}/members")
    public List<TeamMemberResponse> getMembers(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetails ud) {
        return teamService.getMembers(teamId, ud.getUsername());
    }

    @PostMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamMemberResponse inviteMember(
            @PathVariable Long teamId,
            @Valid @RequestBody InviteMemberRequest req,
            @AuthenticationPrincipal UserDetails ud) {
        return teamService.inviteMember(teamId, ud.getUsername(), req);
    }

    @PostMapping("/invites/{inviteToken}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptInvite(
            @PathVariable String inviteToken,
            @AuthenticationPrincipal UserDetails ud) {
        teamService.acceptInvite(inviteToken, ud.getUsername());
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal UserDetails ud) {
        teamService.removeMember(teamId, memberId, ud.getUsername());
    }

    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetails ud) {
        teamService.deleteTeam(teamId, ud.getUsername());
    }

    @GetMapping("/pending-invites")
    public List<TeamMemberResponse> getPendingInvites(@AuthenticationPrincipal UserDetails ud) {
        return teamService.getPendingInvites(ud.getUsername());
    }
}