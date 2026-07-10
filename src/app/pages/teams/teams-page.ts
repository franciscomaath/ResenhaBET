import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';

import { AdminOnlyDirective } from '../../directives/admin-only.directive';
import { TeamResponseDto } from '../../services/api/api.models';
import { TeamsApi } from '../../services/api/teams-api';
import { AppSectionHeaderComponent } from '../../components/ui/app-section-header';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-teams-page',
  imports: [CommonModule, AdminOnlyDirective, AppSectionHeaderComponent],
  templateUrl: './teams-page.html',
})
export class TeamsPage {
  private readonly teamsApi = inject(TeamsApi);

  protected readonly teams = signal<TeamResponseDto[]>([]);
  protected readonly status = signal<LoadStatus>('idle');
  protected readonly errorMessage = signal('');
  protected readonly isCreateModalOpen = signal(false);
  protected readonly newTeamName = signal('');
  protected readonly newTeamAbbreviation = signal('');
  protected readonly newTeamBadgeUrl = signal('');
  protected readonly createErrorMessage = signal('');

  protected readonly sortedTeams = computed(() =>
    [...this.teams()].sort((a, b) => a.name.localeCompare(b.name)),
  );

  constructor() {
    this.loadTeams();
  }

  protected loadTeams(): void {
    this.status.set('loading');
    this.errorMessage.set('');

    this.teamsApi.findAll().subscribe({
      next: (teams) => {
        this.teams.set(teams);
        this.status.set('success');
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel carregar os times. Verifique se o backend esta rodando.');
      },
    });
  }

  protected openCreateModal(): void {
    this.createErrorMessage.set('');
    this.isCreateModalOpen.set(true);
  }

  protected closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
    this.newTeamName.set('');
    this.newTeamAbbreviation.set('');
    this.newTeamBadgeUrl.set('');
    this.createErrorMessage.set('');
  }

  protected setTeamName(event: Event): void {
    this.newTeamName.set((event.target as HTMLInputElement).value);
  }

  protected setTeamAbbreviation(event: Event): void {
    this.newTeamAbbreviation.set((event.target as HTMLInputElement).value.toUpperCase().slice(0, 4));
  }

  protected setTeamBadgeUrl(event: Event): void {
    this.newTeamBadgeUrl.set((event.target as HTMLInputElement).value);
  }

  protected createTeam(): void {
    const name = this.newTeamName().trim();
    const abbreviation = this.newTeamAbbreviation().trim().toUpperCase();
    const badgeUrl = this.newTeamBadgeUrl().trim();

    if (!name || !abbreviation) {
      this.createErrorMessage.set('Informe nome e abreviacao do time.');
      return;
    }

    this.teamsApi
      .create({
        name,
        abbreviation,
        badgeUrl: badgeUrl || null,
      })
      .subscribe({
        next: (createdTeam) => {
          this.teams.update((teams) => [...teams, createdTeam]);
          this.status.set('success');
          this.closeCreateModal();
        },
        error: () => {
          this.createErrorMessage.set('Nao foi possivel criar o time.');
        },
      });
  }

  protected initials(team: TeamResponseDto): string {
    return team.abbreviation || team.name.slice(0, 3).toUpperCase();
  }

  protected previewInitials(): string {
    const abbreviation = this.newTeamAbbreviation().trim();
    const name = this.newTeamName().trim();
    return abbreviation || name.slice(0, 3).toUpperCase() || 'TM';
  }

  protected updateGameForecastId(team: TeamResponseDto, value: string): void {
    const gameForecastTeamId = value.trim() ? value.trim() : null;

    this.teamsApi.updateGameForecastId(team.id, { gameForecastTeamId }).subscribe({
      next: (updatedTeam) => {
        this.teams.update((teams) =>
          teams.map((t) => (t.id === updatedTeam.id ? updatedTeam : t)),
        );
      }
    });
  }
}
