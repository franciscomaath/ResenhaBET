import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';


import { EventResponseDto, TournamentResponseDto, TeamResponseDto } from '../../services/api/api.models';
import { EventsApi } from '../../services/api/events-api';
import { TournamentsApi } from '../../services/api/tournaments-api';
import { TeamsApi } from '../../services/api/teams-api';
import { ResenhaBetState } from '../../services/resenhabet-state';
import { TEAM_TRANSLATIONS } from '../../pipes/team-translations';
import { AppSectionHeaderComponent } from '../../components/ui/app-section-header';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-home-page',
  imports: [CommonModule, RouterLink, AppSectionHeaderComponent],
  templateUrl: './home-page.html',
})
export class HomePage {
  private readonly tournamentsApi = inject(TournamentsApi);
  private readonly eventsApi = inject(EventsApi);
  private readonly teamsApi = inject(TeamsApi);
  private readonly state = inject(ResenhaBetState);

  protected readonly tournaments = signal<TournamentResponseDto[]>([]);
  protected readonly activeTournaments = computed(() =>
    this.tournaments().filter(t => t.status === 'IN_PROGRESS')
  );
  protected readonly playerCounts = signal<Record<number, number | null>>({});
  protected readonly teams = signal<TeamResponseDto[]>([]);
  protected readonly status = signal<LoadStatus>('idle');
  protected readonly errorMessage = signal('');

  protected readonly liveEvents = signal<EventResponseDto[]>([]);
  protected readonly liveEventPlayers = signal<Record<number, any>>({});

  constructor() {
    this.loadLiveEvents();
    this.loadTeams();

    effect(() => {
      this.state.groupSwitchVersion();
      this.loadTournaments();
    });
  }

  private loadTeams(): void {
    this.teamsApi.findAll().subscribe({
      next: (teams) => this.teams.set(teams),
      error: () => {},
    });
  }

  private loadLiveEvents(): void {
    this.eventsApi.getLiveEvents().subscribe({
      next: (events) => {
        if (events.length > 0) {
          this.liveEvents.set(events);
          this.loadPlayersForLiveEvents(events);
        }
      },
      error: () => {},
    });
  }

  private loadPlayersForLiveEvents(events: EventResponseDto[]): void {
    const tournamentIds = [...new Set(events.map(e => e.tournamentId))];
    
    forkJoin(
      tournamentIds.map(tid => this.tournamentsApi.findPlayers(tid).pipe(
        catchError(() => of([]))
      ))
    ).subscribe({
      next: (results) => {
        const playersMap: Record<number, any> = {};
        for (const players of results) {
          for (const player of players) {
            const pid = player.playerId ?? (player as any).player_id ?? 0;
            if (pid) {
              playersMap[pid] = player;
            }
          }
        }
        this.liveEventPlayers.set(playersMap);
      }
    });
  }

  protected scoreValue(value: number | null): number {
    return value ?? 0;
  }

  protected eventLabel(event: EventResponseDto): string {
    const home = this.translatedTeamName(event.teamHomeName) || event.playerHomeName || 'A definir';
    const away = this.translatedTeamName(event.teamAwayName) || event.playerAwayName || 'A definir';
    return `${home} x ${away}`;
  }

  protected displayHomeName(event: EventResponseDto): string {
    return this.translatedTeamName(event.teamHomeName) || event.playerHomeName || 'A definir';
  }

  protected displayAwayName(event: EventResponseDto): string {
    return this.translatedTeamName(event.teamAwayName) || event.playerAwayName || 'A definir';
  }

  protected homeTeamBadgeUrl(event: EventResponseDto): string | null {
    if (event.teamHomeName) {
      return this.teams().find(t => t.name === event.teamHomeName)?.badgeUrl ?? null;
    }
    const player = event.playerHomeId ? this.liveEventPlayers()[event.playerHomeId] : null;
    if (player?.teamId) {
      return this.teams().find(t => t.id === player.teamId)?.badgeUrl ?? null;
    }
    return null;
  }

  protected awayTeamBadgeUrl(event: EventResponseDto): string | null {
    if (event.teamAwayName) {
      return this.teams().find(t => t.name === event.teamAwayName)?.badgeUrl ?? null;
    }
    const player = event.playerAwayId ? this.liveEventPlayers()[event.playerAwayId] : null;
    if (player?.teamId) {
      return this.teams().find(t => t.id === player.teamId)?.badgeUrl ?? null;
    }
    return null;
  }

  protected homeInitials(event: EventResponseDto): string {
    return this.getInitials(this.displayHomeName(event));
  }

  protected awayInitials(event: EventResponseDto): string {
    return this.getInitials(this.displayAwayName(event));
  }

  private getInitials(name: string): string {
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(part => part[0]?.toUpperCase())
      .join('');
  }

  private translatedTeamName(value: string | null | undefined): string {
    if (!value) return '';
    return TEAM_TRANSLATIONS[value] ?? value;
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

}
