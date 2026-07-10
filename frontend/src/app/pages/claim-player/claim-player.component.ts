import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ClaimPlayerCardComponent } from './claim-player-card.component';
import { AvailablePlayerResponseDto } from '../../services/api/api.models';
import { GroupsApi } from '../../services/api/groups-api';
import { PlayersApi } from '../../services/api/players-api';
import { ResenhaBetState } from '../../services/resenhabet-state';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-claim-player',
  imports: [CommonModule, ClaimPlayerCardComponent],
  templateUrl: './claim-player.component.html',
})
export class ClaimPlayerComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly playersApi = inject(PlayersApi);
  private readonly groupsApi = inject(GroupsApi);
  private readonly state = inject(ResenhaBetState);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly groupId = Number(this.route.snapshot.paramMap.get('groupId'));
  private readonly conflictReloaded = signal(false);

  readonly availablePlayers = signal<AvailablePlayerResponseDto[]>([]);
  readonly selectedPlayerId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly activeGroup = computed(() => this.state.activeGroup());
  readonly groupName = computed(() => this.activeGroup()?.name ?? '');
  readonly hasAvailablePlayers = computed(() => this.availablePlayers().length > 0);

  constructor() {
    this.loadAvailablePlayers();
  }

  protected selectPlayer(playerId: number): void {
    if (!this.loading()) {
      this.selectedPlayerId.set(this.selectedPlayerId() === playerId ? null : playerId);
      this.error.set(null);
    }
  }

  protected confirmPlayer(): void {
    this.submitClaim(this.selectedPlayerId());
  }

  protected claimAsBettor(): void {
    this.submitClaim(null);
  }

  protected isSelected(playerId: number): boolean {
    return this.selectedPlayerId() === playerId;
  }

  private loadAvailablePlayers(preserveError = false): void {
    if (!Number.isFinite(this.groupId) || this.groupId <= 0) {
      this.error.set('Não foi possível identificar o grupo ativo.');
      return;
    }

    this.loading.set(true);
    if (!preserveError) {
      this.error.set(null);
    }

    this.playersApi.getAvailablePlayers(this.groupId).subscribe({
      next: (players) => {
        this.availablePlayers.set(players);
        this.loading.set(false);
        if (!preserveError) {
          this.conflictReloaded.set(false);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(this.backendMessage(error) ?? 'Não foi possível carregar os jogadores disponíveis.');
      },
    });
  }

  private submitClaim(playerId: number | null): void {
    if (!Number.isFinite(this.groupId) || this.groupId <= 0 || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.groupsApi.claimPlayer(this.groupId, playerId).subscribe({
      next: () => {
        const group = this.activeGroup();
        this.auth.setCurrentGroup({
          id: this.groupId,
          name: group?.name ?? this.auth.currentGroupName() ?? '',
          role: group?.role ?? this.auth.currentGroupRole(),
          playerClaimed: true,
        });
        this.state.loadGroups();
        void this.router.navigate(['/']);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);

        if (error.status === 409) {
          this.selectedPlayerId.set(null);
          this.error.set('Este jogador já foi escolhido por outro membro. Escolha outro.');

          if (!this.conflictReloaded()) {
            this.conflictReloaded.set(true);
            this.loadAvailablePlayers(true);
          }

          return;
        }

        if (error.status === 400) {
          this.error.set(this.backendMessage(error) ?? 'Não foi possível concluir a escolha.');
          return;
        }

        this.error.set(this.backendMessage(error) ?? 'Não foi possível concluir a escolha.');
      },
    });
  }

  private backendMessage(error: HttpErrorResponse): string | null {
    const body = error.error as { message?: unknown; error?: unknown } | null;
    if (!body || typeof body !== 'object') {
      return null;
    }

    if (typeof body.message === 'string' && body.message.trim()) {
      return body.message;
    }

    if (typeof body.error === 'string' && body.error.trim()) {
      return body.error;
    }

    return null;
  }
}
