import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';

import { AppSectionHeaderComponent } from '../../components/ui/app-section-header';
import { GroupAdminOnlyDirective } from '../../directives/group-admin-only.directive';
import { CompetitionResponseDto, TournamentResponseDto } from '../../services/api/api.models';
import { CompetitionsApi } from '../../services/api/competitions-api';
import { TournamentsApi } from '../../services/api/tournaments-api';
import { ResenhaBetState } from '../../services/resenhabet-state';
import { ToastService } from '../../services/toast.service';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-tournaments-page',
  imports: [CommonModule, RouterLink, GroupAdminOnlyDirective, AppSectionHeaderComponent],
  templateUrl: './tournaments-page.html',
})
export class TournamentsListPage {
  private readonly tournamentsApi = inject(TournamentsApi);
  private readonly competitionsApi = inject(CompetitionsApi);
  private readonly state = inject(ResenhaBetState);

  protected readonly tournaments = signal<TournamentResponseDto[]>([]);
  protected readonly playerCounts = signal<Record<number, number | null>>({});
  protected readonly status = signal<LoadStatus>('idle');
  protected readonly errorMessage = signal('');
  protected readonly createNotice = signal('');
  protected readonly isCreateModalOpen = signal(false);
  protected readonly newTournamentName = signal('');
  protected readonly newTournamentFormat = signal('LEAGUE');
  protected readonly newTournamentType = signal<string>('FIFA_MATCH');
  protected readonly selectedMarketTypes = signal<string[]>(['MATCH_RESULT']);
  protected readonly competitions = signal<CompetitionResponseDto[]>([]);
  protected readonly selectedCompetitionId = signal<number | null>(null);
  protected readonly newTournamentGenerationMode = signal('AUTO');
  protected readonly newTournamentHasThirdPlaceMatch = signal(false);
  protected readonly newTournamentStartDate = signal('');
  protected readonly newTournamentEndDate = signal('');

  protected readonly sortedTournaments = computed(() =>
    [...this.tournaments()].sort((a, b) => b.id - a.id),
  );

  protected readonly showThirdPlaceMatch = computed(() => {
    const format = this.newTournamentFormat();
    return format === 'BRACKET' || format === 'LEAGUE_BRACKET';
  });

  protected readonly isRealFootball = computed(() =>
    this.newTournamentType() === 'REAL_FOOTBALL'
  );

  constructor() {
    effect(() => {
      this.state.groupSwitchVersion();
      this.loadTournaments();
    });
  }

  protected loadTournaments(): void {
    this.status.set('loading');
    this.errorMessage.set('');

    this.tournamentsApi.findAll(0, 100).pipe(
      switchMap((page) => {
        const tournaments = page.content;
        if (tournaments.length === 0) {
          return of({ tournaments, playerCounts: {} });
        }

        return forkJoin(
          tournaments.map((tournament) =>
            this.tournamentsApi.findPlayersSummary(tournament.id).pipe(
              map((summary) => [tournament.id, summary.playerCount] as const),
              catchError(() => of([tournament.id, null] as const)),
            ),
          ),
        ).pipe(
          map((entries) => ({
            tournaments,
            playerCounts: Object.fromEntries(entries),
          })),
        );
      }),
    ).subscribe({
      next: ({ tournaments, playerCounts }) => {
        this.tournaments.set(tournaments);
        this.playerCounts.set(playerCounts);
        this.status.set('success');
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel carregar os torneios. Verifique se o backend esta rodando.');
      },
    });
  }

  protected openCreateModal(): void {
    const now = this.localDateTimeNow();
    this.newTournamentStartDate.set(now);
    this.newTournamentEndDate.set(now);
    this.createNotice.set('');
    this.competitionsApi.findAll(true).subscribe({
      next: (competitions) => this.competitions.set(competitions),
    });
    this.isCreateModalOpen.set(true);
  }

  protected closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
    this.newTournamentName.set('');
    this.newTournamentFormat.set('LEAGUE');
    this.newTournamentType.set('FIFA_MATCH');
    this.selectedMarketTypes.set(['MATCH_RESULT']);
    this.competitions.set([]);
    this.selectedCompetitionId.set(null);
    this.newTournamentGenerationMode.set('AUTO');
    this.newTournamentHasThirdPlaceMatch.set(false);
    this.newTournamentStartDate.set('');
    this.newTournamentEndDate.set('');
  }

  protected setSelectedCompetitionId(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedCompetitionId.set(value ? Number(value) : null);
  }

  protected setTournamentName(event: Event): void {
    this.newTournamentName.set((event.target as HTMLInputElement).value);
  }

  protected setTournamentStartDate(event: Event): void {
    this.newTournamentStartDate.set((event.target as HTMLInputElement).value);
  }

  protected setTournamentEndDate(event: Event): void {
    this.newTournamentEndDate.set((event.target as HTMLInputElement).value);
  }

  protected setTournamentFormat(event: Event): void {
    this.newTournamentFormat.set((event.target as HTMLSelectElement).value);
  }

  protected setTournamentType(type: string): void {
    this.newTournamentType.set(type);
  }

  protected toggleMarketType(type: string): void {
    this.selectedMarketTypes.update((current) => {
      if (current.includes(type)) {
        return current.filter((t) => t !== type);
      }
      return [...current, type];
    });
  }

  protected isMarketTypeSelected(type: string): boolean {
    return this.selectedMarketTypes().includes(type);
  }

  protected marketTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      MATCH_RESULT: 'Resultado (1X2)',
      OVER_UNDER_25: 'Over/Under 2.5 gols',
      OVER_UNDER_35: 'Over/Under 3.5 gols',
      BTTS: 'Ambas marcam',
      EXACT_SCORE: 'Placar exato',
      QUALIFY: 'Quem avança',
    };
    return labels[type] ?? type;
  }

  protected tournamentTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      FIFA_MATCH: 'FIFA Match',
      REAL_FOOTBALL: 'Futebol real',
    };

    return labels[type] ?? type;
  }

  protected tournamentTypeHint(type: string): string {
    if (type === 'REAL_FOOTBALL') {
      return 'Jogos e mercados de futebol real sao compartilhados entre grupos; apostas e carteiras continuam por grupo.';
    }

    return 'Jogadores, partidas e controles sao do grupo ativo.';
  }

  protected availableMarketTypes(): { value: string; label: string }[] {
    return [
      { value: 'MATCH_RESULT', label: 'Resultado (1X2)' },
      { value: 'OVER_UNDER_25', label: 'Over/Under 2.5 gols' },
      { value: 'OVER_UNDER_35', label: 'Over/Under 3.5 gols' },
      { value: 'BTTS', label: 'Ambas marcam' },
      { value: 'EXACT_SCORE', label: 'Placar exato' },
      { value: 'QUALIFY', label: 'Quem avança' },
    ];
  }

  protected setTournamentGenerationMode(mode: string): void {
    this.newTournamentGenerationMode.set(mode);
  }

  protected toggleHasThirdPlaceMatch(): void {
    this.newTournamentHasThirdPlaceMatch.update((v) => !v);
  }

  protected createTournament(): void {
    const isRf = this.isRealFootball();
    const name = isRf ? 'Tournament' : this.newTournamentName().trim();
    if (!name) {
      return;
    }

    if (isRf && !this.selectedCompetitionId()) {
      this.errorMessage.set('Selecione uma competicao para criar ou conectar um torneio de Futebol Real.');
      this.status.set('error');
      return;
    }

    this.tournamentsApi
      .create({
        name,
        format: isRf ? 'LEAGUE_BRACKET' : this.newTournamentFormat(),
        type: this.newTournamentType(),
        marketTypes: this.selectedMarketTypes(),
        generationMode: isRf ? 'MANUAL' : this.newTournamentGenerationMode(),
        hasThirdPlaceMatch: isRf ? true : (this.showThirdPlaceMatch() ? this.newTournamentHasThirdPlaceMatch() : undefined),
        startDate: isRf ? undefined : this.toApiDateTime(this.newTournamentStartDate()),
        endDate: isRf ? undefined : this.toApiDateTime(this.newTournamentEndDate()),
        competitionId: isRf ? this.selectedCompetitionId() : undefined,
      })
      .subscribe({
        next: (createdTournament) => {
          this.tournaments.update((tournaments) => [...tournaments, createdTournament]);
          this.createNotice.set(
            isRf && createdTournament.groupTournamentId
              ? 'Grupo conectado ao torneio compartilhado de Futebol Real.'
              : 'Torneio criado com sucesso.',
          );
          this.closeCreateModal();
          this.status.set('success');
        },
        error: () => {
          this.errorMessage.set('Nao foi possivel criar o torneio.');
          this.status.set('error');
        },
      });
  }

  protected statusLabel(status: string): string {
    const labels: Record<string, string> = {
      CREATED: 'Criado',
      IN_PROGRESS: 'Em andamento',
      COMPLETED: 'Encerrado',
      CANCELLED: 'Cancelado',
    };

    return labels[status] ?? status;
  }

  protected tournamentFormatLabel(format: string): string {
    const labels: Record<string, string> = {
      LEAGUE: 'Liga',
      BRACKET: 'Mata-mata',
      LEAGUE_BRACKET: 'Liga + Mata-mata',
    };

    return labels[format] ?? format;
  }

  protected formatDescription(format: string): string {
    const descriptions: Record<string, string> = {
      LEAGUE: 'Todos contra todos, melhor classificado vence',
      BRACKET: 'Eliminacao direta, mata-mata',
      LEAGUE_BRACKET: 'Fase de grupos seguida de mata-mata',
    };
    return descriptions[format] ?? '';
  }

  protected formatDate(value: string | null): string {
    if (!value) {
      return 'Sem data';
    }

    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(new Date(value));
  }

  protected trackTournament(_: number, tournament: TournamentResponseDto): number {
    return tournament.id;
  }

  protected playerCountLabel(tournamentId: number): string {
    const playerCount = this.playerCounts()[tournamentId];
    return playerCount === null || playerCount === undefined ? '--' : String(playerCount);
  }

  private toApiDateTime(value: string): string | null {
    return value ? this.withSeconds(value) : null;
  }

  private localDateTimeNow(): string {
    const now = new Date();
    const timezoneOffset = now.getTimezoneOffset() * 60_000;
    return new Date(now.getTime() - timezoneOffset).toISOString().slice(0, 16);
  }

  private withSeconds(value: string): string {
    return value.length === 16 ? `${value}:00` : value;
  }
}
