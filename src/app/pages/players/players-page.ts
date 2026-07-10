import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';

import { PlayerResponseDto, PlayerStatsResponseDto, UpdatePlayerRequestDto, UserResponseDto, UserType } from '../../services/api/api.models';
import { PlayersApi } from '../../services/api/players-api';
import { UsersApi } from '../../services/api/users-api';
import { ResenhaBetState } from '../../services/resenhabet-state';
import { AppSectionHeaderComponent } from '../../components/ui/app-section-header';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

interface PlayerViewModel {
  id: number;
  name: string;
  active: boolean;
  userId: number | null;
}

@Component({
  selector: 'app-players-page',
  imports: [CommonModule, AppSectionHeaderComponent],
  templateUrl: './players-page.html',
})
export class PlayersPage {
  private readonly playersApi = inject(PlayersApi);
  private readonly usersApi = inject(UsersApi);
  private readonly state = inject(ResenhaBetState);

  protected readonly players = signal<PlayerViewModel[]>([]);
  protected readonly users = signal<UserResponseDto[]>([]);
  protected readonly status = signal<LoadStatus>('idle');
  protected readonly errorMessage = signal('');
  protected readonly newPlayerName = signal('');
  protected readonly searchTerm = signal('');
  protected readonly linkSelections = signal<Record<number, number | null>>({});
  protected readonly playerStats = signal<Record<number, PlayerStatsResponseDto>>({});
  protected readonly resetPinUserId = signal<number | null>(null);
  
  protected readonly editUserSelectedId = signal<number | null>(null);
  protected readonly editUserName = signal('');
  protected readonly editUserType = signal<UserType>(UserType.USER);
  protected readonly isUpdatingUser = signal(false);
  protected readonly canManageGroup = this.state.canManageGroup;
  protected readonly isSystemAdmin = this.state.isSystemAdmin;
  protected readonly activeGroup = this.state.activeGroup;

  protected readonly activePlayers = computed(() =>
    this.filteredPlayers()
      .filter((player) => player.active)
      .sort((a, b) => a.name.localeCompare(b.name)),
  );

  protected readonly inactivePlayers = computed(() =>
    this.filteredPlayers()
      .filter((player) => !player.active)
      .sort((a, b) => a.name.localeCompare(b.name)),
  );

  constructor() {
    this.loadPlayers();
    this.loadUsers();

    effect(() => {
      const groupId = this.activeGroup()?.id;
      if (!groupId) {
        return;
      }

      this.linkSelections.set({});
      this.resetPinUserId.set(null);
      this.loadPlayers();
    });
  }

  protected loadPlayers(): void {
    this.status.set('loading');
    this.errorMessage.set('');

    this.playersApi.findAll().subscribe({
      next: (players) => {
        this.players.set(players.map((player) => this.toPlayerViewModel(player)));
        this.status.set('success');
        this.loadAllPlayerStats();
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel carregar os jogadores. Verifique se o backend esta rodando e se CORS esta configurado.');
      },
    });
  }

  protected loadUsers(): void {
    this.usersApi.getAll().subscribe({
      next: (users) => this.users.set(users),
    });
  }

  private loadAllPlayerStats(): void {
    this.players().forEach((player) => {
      this.playersApi.getStats(player.id).subscribe({
        next: (stats) => {
          this.playerStats.update((current) => ({ ...current, [player.id]: stats }));
        },
      });
    });
  }

  protected setNewPlayerName(event: Event): void {
    this.newPlayerName.set((event.target as HTMLInputElement).value);
  }

  protected setSearchTerm(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  protected clearSearch(): void {
    this.searchTerm.set('');
  }

  protected createPlayer(): void {
    const name = this.newPlayerName().trim();
    if (!name) {
      return;
    }

    this.status.set('loading');
    this.playersApi.create({ name }).subscribe({
      next: (createdPlayer) => {
        this.players.update((players) => [...players, this.toPlayerViewModel(createdPlayer)]);
        this.newPlayerName.set('');
        this.status.set('success');
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel criar o jogador.');
      },
    });
  }

  protected updatePlayerName(player: PlayerViewModel, event: Event): void {
    const name = (event.target as HTMLInputElement).value.trim();
    if (!name || name === player.name) {
      return;
    }

    this.updatePlayer(player.id, { name, active: player.active, isActive: player.active });
  }

  protected togglePlayerStatus(player: PlayerViewModel): void {
    const nextActive = !player.active;
    this.playersApi.updateActiveStatus(player.id, { active: nextActive }).subscribe({
      next: (updatedPlayer) => {
        this.players.update((players) =>
          players.map((p) => (p.id === updatedPlayer.id ? this.toPlayerViewModel(updatedPlayer) : p)),
        );
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel atualizar o status do jogador.');
      }
    });
  }

  protected deletePlayer(player: PlayerViewModel): void {
    if (!confirm('Deseja realmente excluir este jogador?')) {
      return;
    }

    this.playersApi.delete(player.id).subscribe({
      next: () => {
        this.players.update((players) => players.filter((p) => p.id !== player.id));
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel excluir o jogador.');
      }
    });
  }

  protected setLinkSelection(playerId: number, event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    this.linkSelections.update((selections) => ({
      ...selections,
      [playerId]: Number.isFinite(value) && value > 0 ? value : null,
    }));
  }

  protected linkUser(player: PlayerViewModel): void {
    const userId = this.linkSelections()[player.id];
    if (!userId) {
      return;
    }

    this.playersApi.linkUser(player.id, userId).subscribe({
      next: (updatedPlayer) => {
        this.players.update((players) =>
          players.map((item) =>
            item.id === updatedPlayer.id ? this.toPlayerViewModel(updatedPlayer) : item,
          ),
        );
        this.linkSelections.update((selections) => ({ ...selections, [player.id]: null }));
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel vincular o usuario ao jogador.');
      },
    });
  }

  protected setResetPinUser(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    this.resetPinUserId.set(Number.isFinite(value) && value > 0 ? value : null);
  }

  protected resetPin(): void {
    const userId = this.resetPinUserId();
    if (!userId) {
      return;
    }

    this.usersApi.resetPin(userId).subscribe({
      next: () => {
        this.users.update((users) =>
          users.map((user) =>
            user.id === userId ? { ...user, firstLogin: true, hasPin: false } : user,
          ),
        );
        this.resetPinUserId.set(null);
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel resetar o PIN.');
      },
    });
  }

  protected setEditUserSelectedId(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    const userId = Number.isFinite(value) && value > 0 ? value : null;
    this.editUserSelectedId.set(userId);
    
    if (userId) {
      const user = this.users().find((u) => u.id === userId);
      if (user) {
        this.editUserName.set(user.name);
        this.editUserType.set(user.userType);
      }
    } else {
      this.editUserName.set('');
      this.editUserType.set(UserType.USER);
    }
  }

  protected setEditUserName(event: Event): void {
    this.editUserName.set((event.target as HTMLInputElement).value);
  }

  protected setEditUserType(event: Event): void {
    this.editUserType.set((event.target as HTMLSelectElement).value as UserType);
  }

  protected updateUser(): void {
    const userId = this.editUserSelectedId();
    const name = this.editUserName().trim();
    if (!userId || !name || this.isUpdatingUser()) {
      return;
    }

    this.isUpdatingUser.set(true);
    this.usersApi.update(userId, { name, userType: this.editUserType() }).subscribe({
      next: (updatedUser) => {
        this.users.update((users) =>
          users.map((user) => (user.id === updatedUser.id ? updatedUser : user)),
        );
        this.isUpdatingUser.set(false);
        this.editUserSelectedId.set(null);
      },
      error: () => {
        this.isUpdatingUser.set(false);
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel atualizar o usuario.');
      },
    });
  }

  protected playerInitials(player: PlayerViewModel): string {
    return player.name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  }

  protected hasVisiblePlayers(): boolean {
    return this.activePlayers().length > 0 || this.inactivePlayers().length > 0;
  }

  protected availableUsersFor(player: PlayerViewModel): UserResponseDto[] {
    const linkedUserIds = new Set(
      this.players()
        .filter((item) => item.userId && item.id !== player.id)
        .map((item) => item.userId),
    );

    return this.users()
      .filter((user) => !linkedUserIds.has(user.id))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  protected userName(userId: number | null): string {
    return this.users().find((user) => user.id === userId)?.name ?? 'Sem usuario vinculado';
  }

  private updatePlayer(id: number, request: UpdatePlayerRequestDto): void {
    this.playersApi.update(id, request).subscribe({
      next: (updatedPlayer) => {
        this.players.update((players) =>
          players.map((player) =>
            player.id === updatedPlayer.id ? this.toPlayerViewModel(updatedPlayer) : player,
          ),
        );
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel atualizar o jogador.');
      },
    });
  }

  private toPlayerViewModel(player: PlayerResponseDto): PlayerViewModel {
    return {
      id: player.id,
      name: player.name,
      active: player.active ?? player.isActive ?? false,
      userId: player.userId ?? null,
    };
  }

  private filteredPlayers(): PlayerViewModel[] {
    const search = this.normalize(this.searchTerm());
    if (!search) {
      return this.players();
    }

    return this.players().filter((player) => this.normalize(player.name).includes(search));
  }

  private normalize(value: string): string {
    return value.trim().toLowerCase();
  }
}
