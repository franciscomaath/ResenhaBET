import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api-base-url';
import {
  WalletDepositRequestDto,
  WalletResponseDto,
} from './api.models';

@Injectable({
  providedIn: 'root',
})
export class WalletApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getBalance(userId: number, tournamentId: number): Observable<WalletResponseDto> {
    const params = new HttpParams()
      .set('userId', String(userId))
      .set('tournamentId', String(tournamentId));
    return this.http.get<WalletResponseDto>(`${this.baseUrl}/wallet`, {
      params,
    });
  }

  deposit(request: WalletDepositRequestDto): Observable<WalletResponseDto> {
    return this.http.post<WalletResponseDto>(
      `${this.baseUrl}/wallet/deposit`,
      request,
    );
  }

  depositAll(tournamentId: number, amount: number): Observable<void> {
    const params = new HttpParams()
      .set('tournamentId', String(tournamentId))
      .set('amount', String(amount));
    return this.http.post<void>(
      `${this.baseUrl}/wallet/deposit-all`,
      null,
      { params },
    );
  }
}
