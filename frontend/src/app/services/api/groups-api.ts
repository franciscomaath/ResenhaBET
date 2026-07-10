import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api-base-url';
import {
  ClaimPlayerRequestDto,
  GroupJoinRequestDto,
  GroupMemberRequestDto,
  GroupMemberResponseDto,
  GroupRequestDto,
  GroupResponseDto,
  GroupSwitchResponseDto,
  PatchGroupRequestDto,
  PatchGroupRoleRequestDto,
  PlayerStatsResponseDto,
} from './api.models';

@Injectable({
  providedIn: 'root',
})
export class GroupsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  listMine(): Observable<GroupResponseDto[]> {
    return this.http.get<GroupResponseDto[]>(`${this.baseUrl}/groups`);
  }

  listMembers(groupId: number): Observable<GroupMemberResponseDto[]> {
    return this.http.get<GroupMemberResponseDto[]>(`${this.baseUrl}/groups/${groupId}/members`);
  }

  getRanking(groupId: number): Observable<PlayerStatsResponseDto[]> {
    return this.http.get<PlayerStatsResponseDto[]>(`${this.baseUrl}/groups/${groupId}/ranking`);
  }

  create(request: GroupRequestDto): Observable<GroupResponseDto> {
    return this.http.post<GroupResponseDto>(`${this.baseUrl}/groups`, request);
  }

  joinGroup(request: GroupJoinRequestDto): Observable<GroupResponseDto> {
    return this.http.post<GroupResponseDto>(`${this.baseUrl}/groups/join`, request);
  }

  switchGroup(groupId: number): Observable<GroupSwitchResponseDto> {
    return this.http.post<GroupSwitchResponseDto>(`${this.baseUrl}/groups/${groupId}/switch`, null);
  }

  claimPlayer(groupId: number, playerId: number | null): Observable<void> {
    const request: ClaimPlayerRequestDto = { playerId };
    return this.http.post<void>(`${this.baseUrl}/groups/${groupId}/claim-player`, request);
  }

  addMember(groupId: number, request: GroupMemberRequestDto): Observable<GroupResponseDto> {
    return this.http.post<GroupResponseDto>(`${this.baseUrl}/groups/${groupId}/members`, request);
  }

  recalculateElo(groupId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/groups/${groupId}/recalculate-elo`, null);
  }

  update(groupId: number, request: PatchGroupRequestDto): Observable<GroupResponseDto> {
    return this.http.patch<GroupResponseDto>(`${this.baseUrl}/groups/${groupId}`, request);
  }

  delete(groupId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/groups/${groupId}`);
  }

  removeMember(groupId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/groups/${groupId}/members/${userId}`);
  }

  updateMemberRole(groupId: number, userId: number, request: PatchGroupRoleRequestDto): Observable<GroupMemberResponseDto> {
    return this.http.patch<GroupMemberResponseDto>(`${this.baseUrl}/groups/${groupId}/members/${userId}/role`, request);
  }
}
