import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api-base-url';
import {
  AvailablePlayerResponseDto,
  CreatePlayerRequestDto,
  PatchPlayerActiveRequestDto,
  PlayerResponseDto,
  PlayerStatsResponseDto,
  UpdatePlayerRequestDto,
} from './api.models';

@Injectable({
  providedIn: 'root',
})
export class PlayersApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  findAll(): Observable<PlayerResponseDto[]> {
    return this.http.get<PlayerResponseDto[]>(`${this.baseUrl}/players`);
  }

  getAvailablePlayers(groupId: number): Observable<AvailablePlayerResponseDto[]> {
    return this.http.get<AvailablePlayerResponseDto[]>(`${this.baseUrl}/groups/${groupId}/players/available`);
  }

  findById(id: number): Observable<PlayerResponseDto> {
    return this.http.get<PlayerResponseDto>(`${this.baseUrl}/players/${id}`);
  }

  create(request: CreatePlayerRequestDto): Observable<PlayerResponseDto> {
    return this.http.post<PlayerResponseDto>(`${this.baseUrl}/players`, request);
  }

  update(id: number, request: UpdatePlayerRequestDto): Observable<PlayerResponseDto> {
    return this.http.put<PlayerResponseDto>(`${this.baseUrl}/players/${id}`, request);
  }

  linkUser(playerId: number, userId: number): Observable<PlayerResponseDto> {
    return this.http.patch<PlayerResponseDto>(`${this.baseUrl}/players/${playerId}/link-user`, { userId });
  }

  getStats(id: number, tournamentId?: number): Observable<PlayerStatsResponseDto> {
    let params = new HttpParams();
    if (tournamentId) {
      params = params.set('tournamentId', String(tournamentId));
    }
    return this.http.get<PlayerStatsResponseDto>(`${this.baseUrl}/players/${id}/stats`, { params });
  }

  updateActiveStatus(id: number, request: PatchPlayerActiveRequestDto): Observable<PlayerResponseDto> {
    return this.http.patch<PlayerResponseDto>(`${this.baseUrl}/players/${id}/active`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/players/${id}`);
  }
}
