import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api-base-url';
import {
  CreateTeamRequestDto,
  PatchTeamGameForecastIdRequestDto,
  TeamResponseDto,
} from './api.models';

@Injectable({
  providedIn: 'root',
})
export class TeamsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  findAll(): Observable<TeamResponseDto[]> {
    return this.http.get<TeamResponseDto[]>(`${this.baseUrl}/teams`);
  }

  create(request: CreateTeamRequestDto): Observable<TeamResponseDto> {
    return this.http.post<TeamResponseDto>(`${this.baseUrl}/teams`, request);
  }

  findById(id: number): Observable<TeamResponseDto> {
    return this.http.get<TeamResponseDto>(`${this.baseUrl}/teams/${id}`);
  }

  updateGameForecastId(id: number, request: PatchTeamGameForecastIdRequestDto): Observable<TeamResponseDto> {
    return this.http.patch<TeamResponseDto>(`${this.baseUrl}/teams/${id}/game-forecast-id`, request);
  }
}
