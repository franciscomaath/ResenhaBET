import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api-base-url';
import {
  CompletedEventRequestDto,
  CreateEventRequestDto,
  EventResponseDto,
  FinishEventRequestDto,
  PatchEventPlayersRequestDto,
  UpdateEventScoreRequestDto,
  PatchEventDatetimeRequestDto,
  UpdateEventRequestDto,
} from './api.models';

@Injectable({
  providedIn: 'root',
})
export class EventsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  findAll(): Observable<EventResponseDto[]> {
    return this.http.get<EventResponseDto[]>(`${this.baseUrl}/events`);
  }

  findByTournamentId(tournamentId: number): Observable<EventResponseDto[]> {
    return this.http.get<EventResponseDto[]>(`${this.baseUrl}/events`, {
      params: { tournamentId },
    });
  }

  findById(id: number): Observable<EventResponseDto> {
    return this.http.get<EventResponseDto>(`${this.baseUrl}/events/${id}`);
  }

  create(request: CreateEventRequestDto): Observable<EventResponseDto> {
    return this.http.post<EventResponseDto>(`${this.baseUrl}/events`, request);
  }

  createCompleted(request: CompletedEventRequestDto): Observable<EventResponseDto> {
    return this.http.post<EventResponseDto>(`${this.baseUrl}/events/completed`, request);
  }

  start(id: number): Observable<EventResponseDto> {
    return this.http.post<EventResponseDto>(`${this.baseUrl}/events/${id}/start`, null);
  }

  updateScore(id: number, request: UpdateEventScoreRequestDto): Observable<EventResponseDto> {
    return this.http.post<EventResponseDto>(`${this.baseUrl}/events/${id}/score`, request);
  }

  finish(id: number): Observable<EventResponseDto> {
    return this.http.post<EventResponseDto>(`${this.baseUrl}/events/${id}/end`, null);
  }

  reopen(id: number): Observable<EventResponseDto> {
    return this.http.post<EventResponseDto>(`${this.baseUrl}/events/${id}/reopen`, null);
  }

  reset(id: number): Observable<EventResponseDto> {
    return this.http.post<EventResponseDto>(`${this.baseUrl}/events/${id}/reset`, null);
  }

  getLiveEvents(): Observable<EventResponseDto[]> {
    return this.http.get<EventResponseDto[]>(`${this.baseUrl}/events`, {
      params: { status: 'IN_PROGRESS' },
    });
  }

  recordPenalties(id: number, request: FinishEventRequestDto): Observable<EventResponseDto> {
    return this.http.patch<EventResponseDto>(`${this.baseUrl}/events/${id}/penalties`, request);
  }

  updatePlayers(id: number, request: PatchEventPlayersRequestDto): Observable<EventResponseDto> {
    return this.http.patch<EventResponseDto>(`${this.baseUrl}/events/${id}/players`, request);
  }

  update(id: number, request: UpdateEventRequestDto): Observable<EventResponseDto> {
    return this.http.patch<EventResponseDto>(`${this.baseUrl}/events/${id}`, request);
  }

  updateDatetime(id: number, request: PatchEventDatetimeRequestDto): Observable<EventResponseDto> {
    return this.http.patch<EventResponseDto>(`${this.baseUrl}/events/${id}/datetime`, request);
  }

  cancel(id: number): Observable<EventResponseDto> {
    return this.http.post<EventResponseDto>(`${this.baseUrl}/events/${id}/cancel`, null);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/events/${id}`);
  }
}
