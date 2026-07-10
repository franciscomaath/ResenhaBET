import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api-base-url';
import {
  MarketResponseDto,
  UpdateMarketStatusRequestDto,
} from './api.models';

@Injectable({
  providedIn: 'root',
})
export class MarketsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  findByEventId(eventId: number): Observable<MarketResponseDto[]> {
    return this.http.get<MarketResponseDto[]>(
      `${this.baseUrl}/markets/${eventId}`,
    );
  }

  updateStatus(
    eventId: number,
    request: UpdateMarketStatusRequestDto,
  ): Observable<MarketResponseDto[]> {
    return this.http.post<MarketResponseDto[]>(
      `${this.baseUrl}/markets/${eventId}/status`,
      request,
    );
  }
}
