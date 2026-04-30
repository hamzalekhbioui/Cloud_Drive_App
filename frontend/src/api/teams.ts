import client from './client'

export interface TeamMember {
  id: number
  teamId: number
  userEmail: string
  role: 'OWNER' | 'ADMIN' | 'MEMBER'
  status: 'ACTIVE' | 'PENDING'
  inviteToken?: string
  invitedAt: string
  joinedAt: string | null
}

export interface Team {
  id: number
  name: string
  ownerEmail: string
  createdAt: string
  memberCount: number
  callerRole: string
  members: TeamMember[]
}

export const createTeam = (name: string) =>
  client.post<Team>('/teams', { name })

export const getMyTeams = () =>
  client.get<Team[]>('/teams')

export const getTeamMembers = (teamId: number) =>
  client.get<TeamMember[]>(`/teams/${teamId}/members`)

export const inviteMember = (teamId: number, email: string, role: string = 'MEMBER') =>
  client.post<TeamMember>(`/teams/${teamId}/members`, { email, role })

export const acceptInvite = (inviteToken: string) =>
  client.post(`/teams/invites/${inviteToken}/accept`)

export const removeMember = (teamId: number, memberId: number) =>
  client.delete(`/teams/${teamId}/members/${memberId}`)

export const deleteTeam = (teamId: number) =>
  client.delete(`/teams/${teamId}`)

export const getPendingInvites = () =>
  client.get<TeamMember[]>('/teams/pending-invites')