import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, catchError, finalize, of, tap } from 'rxjs';

import { API_BASE_URL } from './api/api-base-url';
import { GroupRole, LoginResponseDto, UserResponseDto, UserType } from './api/api.models';

const SESSION_TOKEN_KEY = 'sessionToken';
const SESSION_USER_KEY = 'sessionUser';
const SESSION_GROUP_KEY = 'sessionGroup';

interface StoredGroupContext {
  id: number;
  name: string;
  role: GroupRole | null;
  playerClaimed: boolean;
}

interface GroupContextInput {
  id: number;
  name: string;
  role: GroupRole | null;
  playerClaimed?: boolean;
}

interface GroupSwitchLikeResponse {
  id?: number;
  name?: string;
  role?: GroupRole | null;
  groupId?: number;
  groupName?: string;
  groupRole?: GroupRole | null;
  playerClaimed?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  private readonly user = signal<UserResponseDto | null>(this.readStoredUser());
  private readonly token = signal<string | null>(this.readStorage(SESSION_TOKEN_KEY));
  private readonly currentGroup = signal<StoredGroupContext | null>(this.readStoredGroup());

  readonly currentUser = this.user.asReadonly();
  readonly currentGroupId = computed(() => this.currentGroup()?.id ?? null);
  readonly currentGroupName = computed(() => this.currentGroup()?.name ?? null);
  readonly currentGroupRole = computed(() => this.currentGroup()?.role ?? null);
  readonly currentGroupPlayerClaimed = computed(() => this.currentGroup()?.playerClaimed ?? true);
  readonly isLoggedIn = computed(() => !!this.token() && !!this.user());
  readonly isSystemAdmin = computed(() => this.user()?.userType === UserType.ADMIN);
  readonly isAdmin = this.isSystemAdmin;
  readonly isGroupOwner = computed(() => this.currentGroupRole() === GroupRole.OWNER);
  readonly isGroupAdmin = computed(() => this.currentGroupRole() === GroupRole.ADMIN);
  readonly canManageGroup = computed(() => this.isGroupOwner() || this.isGroupAdmin());
  readonly canMutateFifaTournament = this.canManageGroup;
  readonly canMutateRealFootballShared = this.isSystemAdmin;

  login(name: string, pin?: string): Observable<LoginResponseDto> {
    const body = pin ? { name, pin } : { name };
    return this.http.post<LoginResponseDto>(`${this.baseUrl}/auth/login`, body).pipe(
      tap((response) => {
        if (!response.pinRequired && response.hasPin) {
          this.storeSession(response);
        }
      }),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, {}).pipe(
      finalize(() => this.clearSession()),
    );
  }

  resetSession(): void {
    this.clearSession();
  }

  getMe(): Observable<UserResponseDto> {
    return this.http.get<UserResponseDto>(`${this.baseUrl}/auth/me`).pipe(
      tap((user) => this.storeUser(user)),
    );
  }

  restoreSession(): void {
    if (!this.token()) {
      return;
    }

    this.getMe().pipe(
      catchError(() => {
        this.clearSession();
        return of(null);
      }),
    ).subscribe();
  }

  setPin(pin: string, tempToken?: string): Observable<void> {
    const options = tempToken ? { headers: { Authorization: `Bearer ${tempToken}` } } : {};
    return this.http.patch<void>(`${this.baseUrl}/auth/pin`, { pin }, options).pipe(
      tap(() => {
        const current = this.user();
        if (current) {
          this.storeUser({ ...current, firstLogin: false, hasPin: true });
        }
      }),
    );
  }

  getToken(): string | null {
    return this.token();
  }

  finalizeLogin(response: LoginResponseDto): void {
    this.storeSession(response);
  }

  private toUser(response: LoginResponseDto): UserResponseDto {
    return {
      id: response.id,
      name: response.name,
      userType: response.userType,
      firstLogin: response.firstLogin,
      hasPin: response.hasPin,
    };
  }

  private storeSession(response: LoginResponseDto): void {
    const user = this.toUser(response);
    this.token.set(response.token);
    this.writeStorage(SESSION_TOKEN_KEY, response.token);
    this.storeUser(user);
    this.storeCurrentGroup(
      response.currentGroupId ?? null,
      response.currentGroupName ?? null,
      null,
    );
  }

  private storeUser(user: UserResponseDto): void {
    this.user.set(user);
    this.writeStorage(SESSION_USER_KEY, JSON.stringify(user));
  }

  setCurrentGroup(group: GroupContextInput | null): void {
    if (!group) {
      this.currentGroup.set(null);
      this.removeStorage(SESSION_GROUP_KEY);
      return;
    }

    const existing = this.currentGroup();
    const nextGroup: StoredGroupContext = {
      id: group.id,
      name: group.name,
      role: group.role,
      playerClaimed: group.playerClaimed ?? (existing?.id === group.id ? existing.playerClaimed : true),
    };

    this.currentGroup.set(nextGroup);
    this.writeStorage(SESSION_GROUP_KEY, JSON.stringify(nextGroup));
  }

  setCurrentGroupFromSwitch(response: GroupSwitchLikeResponse): GroupContextInput | null {
    const groupId = response.groupId ?? response.id ?? null;
    const groupName = response.groupName ?? response.name ?? null;
    const groupRole = response.groupRole ?? response.role ?? null;

    if (!groupId || !groupName) {
      this.setCurrentGroup(null);
      return null;
    }

    const group: GroupContextInput = {
      id: groupId,
      name: groupName,
      role: groupRole,
      playerClaimed: response.playerClaimed ?? true,
    };

    this.setCurrentGroup(group);
    return group;
  }

  private storeCurrentGroup(id: number | null, name: string | null, role: GroupRole | null): void {
    if (!id || !name) {
      this.setCurrentGroup(null);
      return;
    }

    this.setCurrentGroup({ id, name, role, playerClaimed: true });
  }

  private clearSession(): void {
    this.token.set(null);
    this.user.set(null);
    this.currentGroup.set(null);
    this.removeStorage(SESSION_TOKEN_KEY);
    this.removeStorage(SESSION_USER_KEY);
    this.removeStorage(SESSION_GROUP_KEY);
  }

  private readStoredUser(): UserResponseDto | null {
    const raw = this.readStorage(SESSION_USER_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as UserResponseDto;
    } catch {
      return null;
    }
  }

  private readStoredGroup(): StoredGroupContext | null {
    const raw = this.readStorage(SESSION_GROUP_KEY);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as StoredGroupContext;
      if (typeof parsed.id !== 'number' || parsed.id <= 0 || typeof parsed.name !== 'string') {
        return null;
      }

      return {
        id: parsed.id,
        name: parsed.name,
        role: parsed.role ?? null,
        playerClaimed: typeof parsed.playerClaimed === 'boolean' ? parsed.playerClaimed : true,
      };
    } catch {
      return null;
    }
  }

  private readStorage(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private writeStorage(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      // Local storage is optional for tests and private browsing modes.
    }
  }

  private removeStorage(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch {
      // Local storage is optional for tests and private browsing modes.
    }
  }
}
