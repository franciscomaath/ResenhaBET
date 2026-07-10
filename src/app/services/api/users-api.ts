import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api-base-url';
import {
  CreateUserRequestDto,
  PatchUserRequestDto,
  UserResponseDto,
} from './api.models';

@Injectable({
  providedIn: 'root',
})
export class UsersApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getAll(): Observable<UserResponseDto[]> {
    return this.http.get<UserResponseDto[]>(`${this.baseUrl}/users`);
  }

  create(request: CreateUserRequestDto): Observable<UserResponseDto> {
    return this.http.post<UserResponseDto>(`${this.baseUrl}/users`, request);
  }

  resetPin(userId: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/users/${userId}/reset-pin`, {});
  }

  findById(id: number): Observable<UserResponseDto> {
    return this.http.get<UserResponseDto>(`${this.baseUrl}/users/${id}`);
  }

  update(id: number, request: PatchUserRequestDto): Observable<UserResponseDto> {
    return this.http.patch<UserResponseDto>(`${this.baseUrl}/users/${id}`, request);
  }
}
