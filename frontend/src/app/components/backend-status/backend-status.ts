import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';

import { PlayerResponseDto } from '../../services/api/api.models';
import { PlayersApi } from '../../services/api/players-api';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-backend-status',
  imports: [CommonModule],
  templateUrl: './backend-status.html',
})
export class BackendStatus {
  private readonly playersApi = inject(PlayersApi);

  protected readonly status = signal<LoadStatus>('idle');
  protected readonly players = signal<PlayerResponseDto[]>([]);
  protected readonly errorMessage = signal('');

  protected readonly statusLabel = computed(() => {
    switch (this.status()) {
      case 'loading':
        return 'Carregando';
      case 'success':
        return 'Conectado';
      case 'error':
        return 'Indisponivel';
      default:
        return 'Nao testado';
    }
  });

  protected loadPlayers(): void {
    this.status.set('loading');
    this.errorMessage.set('');

    this.playersApi.findAll().subscribe({
      next: (players) => {
        this.players.set(players);
        this.status.set('success');
      },
      error: () => {
        this.players.set([]);
        this.errorMessage.set('Nao foi possivel carregar /api/v1/players. Confirme se o backend esta rodando e se CORS esta permitido.');
        this.status.set('error');
      },
    });
  }
}
