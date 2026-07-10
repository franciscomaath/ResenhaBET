import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { API_BASE_URL } from './api-base-url';
import {
  AddTournamentPlayerRequestDto,
  CreateTournamentRequestDto,
  OddsImportResultDto,
  PageResponseDto,
  StartTournamentRequestDto,
  SyncResultDto,
  TournamentGroupConfigResponseDto,
  TournamentPlayerResponseDto,
  TournamentPlayersResponseDto,
  TournamentResponseDto,
  TournamentRoundResponseDto,
  TournamentScoreboardResponseDto,
  UpdateTournamentPlayerTeamRequestDto,
  PatchTournamentRequestDto,
  BetRankingResponseDto,
} from './api.models';

@Injectable({
  providedIn: 'root',
})
export class TournamentsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  findAll(page: number = 0, size: number = 20): Observable<PageResponseDto<TournamentResponseDto>> {
    return this.http.get<PageResponseDto<TournamentResponseDto>>(`${this.baseUrl}/tournaments`, {
      params: { page, size },
    });
  }

  create(request: CreateTournamentRequestDto): Observable<TournamentResponseDto> {
    return this.http.post<TournamentResponseDto>(`${this.baseUrl}/tournaments`, request);
  }

  start(id: number, request?: StartTournamentRequestDto): Observable<TournamentResponseDto> {
    return this.http.post<TournamentResponseDto>(`${this.baseUrl}/tournaments/${id}/start`, request ?? null);
  }

  findPlayers(id: number): Observable<TournamentPlayerResponseDto[]> {
    return this.findPlayersSummary(id).pipe(map((summary) => summary.players));
  }

  findPlayersSummary(id: number): Observable<TournamentPlayersResponseDto> {
    return this.http.get<TournamentPlayersResponseDto>(`${this.baseUrl}/tournaments/${id}/players`);
  }

  findRounds(id: number): Observable<TournamentRoundResponseDto[]> {
    return this.http.get<TournamentRoundResponseDto[]>(`${this.baseUrl}/tournaments/${id}/rounds`);
  }

  addPlayer(id: number, request: AddTournamentPlayerRequestDto): Observable<TournamentPlayerResponseDto> {
    return this.http.post<TournamentPlayerResponseDto>(`${this.baseUrl}/tournaments/${id}/players`, request);
  }

  removePlayer(id: number, playerId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tournaments/${id}/players/${playerId}`);
  }

  updatePlayerTeam(
    id: number,
    playerId: number,
    request: UpdateTournamentPlayerTeamRequestDto,
  ): Observable<TournamentPlayerResponseDto> {
    return this.http.post<TournamentPlayerResponseDto>(
      `${this.baseUrl}/tournaments/${id}/players/${playerId}/team`,
      request,
    );
  }

  getGroupConfig(playerCount: number): Observable<TournamentGroupConfigResponseDto> {
    return this.http.get<TournamentGroupConfigResponseDto>(
      `${this.baseUrl}/tournaments/tournament-group-config`,
      { params: { playerCount } },
    );
  }

  getScoreboard(id: number): Observable<TournamentScoreboardResponseDto> {
    return this.http.get<TournamentScoreboardResponseDto>(`${this.baseUrl}/tournaments/${id}/scoreboard`);
  }

  getRanking(id: number): Observable<BetRankingResponseDto[]> {
    return this.http.get<BetRankingResponseDto[]>(`${this.baseUrl}/tournaments/${id}/ranking`);
  }

  advanceToBracket(id: number): Observable<TournamentResponseDto> {
    return this.http.post<TournamentResponseDto>(`${this.baseUrl}/tournaments/${id}/advance-to-bracket`, null);
  }

  forceAdvanceToBracket(id: number): Observable<TournamentResponseDto> {
    return this.http.post<TournamentResponseDto>(`${this.baseUrl}/tournaments/${id}/force-advance-to-bracket`, null);
  }

  syncFixtures(id: number, from: string, to: string): Observable<SyncResultDto> {
    return this.http.post<SyncResultDto>(
      `${this.baseUrl}/tournaments/${id}/sync-fixtures`,
      null,
      { params: { from, to } },
    );
  }

  syncOdds(id: number): Observable<OddsImportResultDto> {
    return this.http.post<OddsImportResultDto>(`${this.baseUrl}/tournaments/${id}/sync-odds`, null);
  }

  update(id: number, request: PatchTournamentRequestDto): Observable<TournamentResponseDto> {
    return this.http.patch<TournamentResponseDto>(`${this.baseUrl}/tournaments/${id}`, request);
  }

  cancel(id: number): Observable<TournamentResponseDto> {
    return this.http.post<TournamentResponseDto>(`${this.baseUrl}/tournaments/${id}/cancel`, null);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/tournaments/${id}`);
  }
}
