package com.cloud.drive.service;

import com.cloud.drive.dto.team.InviteMemberRequest;
import com.cloud.drive.dto.team.TeamMemberResponse;
import com.cloud.drive.exception.ApiException;
import com.cloud.drive.model.TeamMember;
import com.cloud.drive.repository.TeamMemberRepository;
import com.cloud.drive.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepo;
    @Mock private TeamMemberRepository memberRepo;

    @InjectMocks private TeamService teamService;

    private static final Long TEAM_ID = 99L;
    private static final String OWNER = "owner@example.com";
    private static final String ADMIN = "admin@example.com";
    private static final String MEMBER = "member@example.com";

    private TeamMember caller(String email, String role) {
        TeamMember member = new TeamMember();
        member.setTeamId(TEAM_ID);
        member.setUserEmail(email);
        member.setRole(role);
        member.setStatus("ACTIVE");
        return member;
    }

    @Test
    void inviteMember_throwsForbidden_whenCallerIsMember() {
        when(memberRepo.findByTeamIdAndUserEmail(TEAM_ID, MEMBER)).thenReturn(Optional.of(caller(MEMBER, "MEMBER")));

        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("new@example.com");
        req.setRole("MEMBER");

        assertThatThrownBy(() -> teamService.inviteMember(TEAM_ID, MEMBER, req))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        verify(memberRepo, never()).save(any());
    }

    @Test
    void inviteMember_allowsAdminBecauseRoleRankMeetsThreshold() {
        when(memberRepo.findByTeamIdAndUserEmail(TEAM_ID, ADMIN)).thenReturn(Optional.of(caller(ADMIN, "ADMIN")));
        when(memberRepo.existsByTeamIdAndUserEmail(TEAM_ID, "new@example.com")).thenReturn(false);
        when(memberRepo.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        InviteMemberRequest req = new InviteMemberRequest();
        req.setEmail("new@example.com");
        req.setRole("MEMBER");

        TeamMemberResponse response = teamService.inviteMember(TEAM_ID, ADMIN, req);

        assertThat(response.getUserEmail()).isEqualTo("new@example.com");
        assertThat(response.getRole()).isEqualTo("MEMBER");
    }
}
