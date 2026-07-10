import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api-base-url';
import { CompetitionRequestDto, CompetitionResponseDto } from './api.models';

@Injectable({
  providedIn: 'root',
})
export class CompetitionsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  findAll(active?: boolean): Observable<CompetitionResponseDto[]> {
    const params: Record<string, string> = {};
    if (active !== undefined) {
      params['active'] = String(active);
    }
    return this.http.get<CompetitionResponseDto[]>(`${this.baseUrl}/competitions`, { params });
  }

  findById(id: number): Observable<CompetitionResponseDto> {
    return this.http.get<CompetitionResponseDto>(`${this.baseUrl}/competitions/${id}`);
  }

  create(request: CompetitionRequestDto): Observable<CompetitionResponseDto> {
    return this.http.post<CompetitionResponseDto>(`${this.baseUrl}/competitions`, request);
  }

  toggleActive(id: number): Observable<CompetitionResponseDto> {
    return this.http.patch<CompetitionResponseDto>(`${this.baseUrl}/competitions/${id}`, null);
  }
}
