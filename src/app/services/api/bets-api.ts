import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api-base-url';
import {
  BetSlipResponseDto,
  CreateBetSlipRequestDto,
} from './api.models';

@Injectable({
  providedIn: 'root',
})
export class BetsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  placeBet(request: CreateBetSlipRequestDto): Observable<BetSlipResponseDto> {
    return this.http.post<BetSlipResponseDto>(
      `${this.baseUrl}/bets`,
      request,
    );
  }

  getMyBets(): Observable<BetSlipResponseDto[]> {
    return this.http.get<BetSlipResponseDto[]>(`${this.baseUrl}/bets/me`);
  }

  getBetsByEvent(eventId: number): Observable<BetSlipResponseDto[]> {
    const params = new HttpParams().set('eventId', String(eventId));
    return this.http.get<BetSlipResponseDto[]>(`${this.baseUrl}/bets`, {
      params,
    });
  }
}
