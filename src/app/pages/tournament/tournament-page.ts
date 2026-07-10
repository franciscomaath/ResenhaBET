import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, OnDestroy, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';

import { AdminOnlyDirective } from '../../directives/admin-only.directive';
import { GroupAdminOnlyDirective } from '../../directives/group-admin-only.directive';
import { ToastService } from '../../services/toast.service';
import {
  CompletedEventRequestDto,
  BracketPlacementDto,
  EventResponseDto,
  GroupStandingsDto,
  MarketResponseDto,
  OddsImportResultDto,
  PlayerResponseDto,
  PlayerStatsResponseDto,
  SyncResultDto,
  TeamResponseDto,
  TournamentGroupOptionDto,
  TournamentPlayerResponseDto,
  TournamentResponseDto,
  TournamentRoundResponseDto,
  TournamentScoreboardResponseDto,
  BetRankingResponseDto,
} from '../../services/api/api.models';
import { EventsApi } from '../../services/api/events-api';
import { MarketsApi } from '../../services/api/markets-api';
import { PlayersApi } from '../../services/api/players-api';
import { TeamsApi } from '../../services/api/teams-api';
import { TournamentsApi } from '../../services/api/tournaments-api';
import { BetCartEntry, BetCartService } from '../../services/bet-cart.service';
import { TEAM_TRANSLATIONS } from '../../pipes/team-translations';
import { ResenhaBetState } from '../../services/resenhabet-state';
import { AppSectionHeaderComponent } from '../../components/ui/app-section-header';
import { TournamentWalletPanelComponent } from './tournament-wallet-panel';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';
type TournamentTab = 'matches' | 'players' | 'standings' | 'bracket' | 'ranking' | 'admin';

@Component({
  selector: 'app-tournament-page',
  imports: [CommonModule, RouterLink, AdminOnlyDirective, GroupAdminOnlyDirective, AppSectionHeaderComponent, TournamentWalletPanelComponent],
  templateUrl: './tournament-page.html',
  styleUrl: './tournament-page.css',
})
export class TournamentPage implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tournamentsApi = inject(TournamentsApi);
  private readonly eventsApi = inject(EventsApi);
  private readonly marketsApi = inject(MarketsApi);
  private readonly playersApi = inject(PlayersApi);
  private readonly teamsApi = inject(TeamsApi);
  private readonly toast = inject(ToastService);
  private readonly state = inject(ResenhaBetState);
  protected readonly betCart = inject(BetCartService);

  protected readonly tournamentId = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly status = signal<LoadStatus>('idle');
  protected readonly errorMessage = signal('');
  protected readonly activeTab = signal<TournamentTab>('matches');
  protected readonly tournament = signal<TournamentResponseDto | null>(null);
  protected readonly allPlayers = signal<PlayerResponseDto[]>([]);
  protected readonly teams = signal<TeamResponseDto[]>([]);
  protected readonly players = signal<TournamentPlayerResponseDto[]>([]);
  protected readonly rounds = signal<TournamentRoundResponseDto[]>([]);
  protected readonly events = signal<EventResponseDto[]>([]);
  protected readonly scoreboard = signal<TournamentScoreboardResponseDto | null>(null);
  protected readonly markets = signal<Record<number, MarketResponseDto[]>>({});
  protected readonly bettingRanking = signal<BetRankingResponseDto[]>([]);
  protected readonly bettingRankingStatus = signal<LoadStatus>('idle');
  protected readonly bettingRankingError = signal('');
  protected readonly isAddPlayerModalOpen = signal(false);
  protected readonly selectedPlayerId = signal<number | null>(null);
  protected readonly newPlayerName = signal('');
  protected readonly isCreateMatchModalOpen = signal(false);
  protected readonly selectedRoundId = signal<number | null>(null);
  protected readonly selectedHomePlayerId = signal<number | null>(null);
  protected readonly selectedAwayPlayerId = signal<number | null>(null);
  protected readonly selectedGameDateTime = signal('');
  protected readonly isCreateCompletedEvent = signal(false);
  protected readonly isCreateByeMatch = signal(false);
  protected readonly completedHomeScoreInput = signal('0');
  protected readonly completedAwayScoreInput = signal('0');
  protected readonly createMatchErrorMessage = signal('');
  protected readonly isTeamModalOpen = signal(false);
  protected readonly teamPlayer = signal<TournamentPlayerResponseDto | null>(null);
  protected readonly selectedTeamId = signal<number | null>(null);
  protected readonly teamErrorMessage = signal('');
  protected readonly walletBalance = this.state.walletBalance;
  protected readonly walletTournamentName = computed(() => this.tournament()?.name ?? 'Torneio atual');

  protected readonly isStartConfigModalOpen = signal(false);
  protected readonly startConfigGroupOptions = signal<TournamentGroupOptionDto[]>([]);
  protected readonly startConfigNumberOfGroups = signal<number | null>(null);
  protected readonly startConfigPlayersAdvancing = signal<number | null>(null);
  protected readonly startConfigLoading = signal(false);
  protected readonly startConfigErrorMessage = signal('');

  protected readonly isAdvanceModalOpen = signal(false);
  protected readonly isAdvancing = signal(false);
  protected readonly advanceErrorMessage = signal('');
  protected readonly isSyncing = signal(false);

  protected readonly isUpdatingTournament = signal(false);
  protected readonly isCancelingTournament = signal(false);
  protected readonly isDeletingTournament = signal(false);
  protected readonly updateTournamentName = signal('');

  protected readonly eventsByRound = computed(() => {
    const tournament = this.tournament();
    const isLeagueBracket = tournament?.format === 'LEAGUE_BRACKET';

    return this.rounds()
      .map((round) => {
        const baseEvents = this.events()
          .filter((event) => event.roundId === this.roundId(round))
          .sort((a, b) => this.compareRoundEvents(a, b));

        // For LEAGUE_BRACKET group stage, group events by round.groupNumber
        if (isLeagueBracket && round.phaseType === 'GROUP_STAGE' && round.groupNumber !== null) {
          return {
            round,
            groupNumber: round.groupNumber,
            events: baseEvents,
            isGroupStage: true,
          };
        }

        return { round, events: baseEvents, isGroupStage: false };
      })
      .filter((group) => group.events.length > 0);
  });

  protected readonly availablePlayers = computed(() => {
    const enrolledIds = new Set(this.players().map((player) => player.playerId));
    return this.allPlayers()
      .filter((player) => this.isPlayerActive(player))
      .filter((player) => !enrolledIds.has(player.id))
      .sort((a, b) => a.name.localeCompare(b.name));
  });

  protected readonly isLeagueBracket = computed(() => this.tournament()?.format === 'LEAGUE_BRACKET');

  protected readonly isRealFootball = computed(() => this.tournament()?.type === 'REAL_FOOTBALL');

  protected readonly canCreateManualMatch = computed(() => this.tournament()?.generationMode === 'MANUAL');

  protected readonly canAdvanceToBracket = computed(() => {
    const tournament = this.tournament();
    if (
      !tournament ||
      tournament.type === 'REAL_FOOTBALL' ||
      tournament.format !== 'LEAGUE_BRACKET' ||
      tournament.status !== 'IN_PROGRESS'
    ) {
      return false;
    }
    // Check if knockout rounds already exist (if any round has phaseType === 'KNOCKOUT')
    const hasKnockoutRounds = this.rounds().some((round) => round.phaseType === 'KNOCKOUT');
    return !hasKnockoutRounds;
  });

  protected readonly knockoutRounds = computed(() =>
    this.rounds()
      .filter((round) => round.phaseType === 'KNOCKOUT')
      .sort((a, b) => b.roundOrder - a.roundOrder),
  );

  protected readonly bracketColumns = computed(() =>
    this.knockoutRounds().map((round) => ({
      round,
      events: this.events()
        .filter((event) => event.roundId === this.roundId(round))
        .sort((a, b) => a.id - b.id),
    })),
  );

  protected readonly showBracketTab = computed(() => {
    const format = this.tournament()?.format;
    return format === 'BRACKET' || (format === 'LEAGUE_BRACKET' && this.knockoutRounds().length > 0);
  });

  protected readonly showStandingsTab = computed(() => {
    const format = this.tournament()?.format;
    return format === 'LEAGUE' || format === 'LEAGUE_BRACKET';
  });

  protected readonly visibleTabs = computed<TournamentTab[]>(() => {
    const tabs: TournamentTab[] = ['matches'];
    if (this.showStandingsTab()) tabs.push('standings');
    if (this.showBracketTab()) tabs.push('bracket');
    tabs.push('players', 'ranking', 'admin');
    return tabs;
  });

  protected readonly startConfigSelectedOption = computed(() => {
    const groupCount = this.startConfigNumberOfGroups();
    if (!groupCount) return null;
    return this.startConfigGroupOptions().find((o) => o.groupCount === groupCount) ?? null;
  });

  protected readonly startConfigTotalAdvancing = computed(() => {
    const groups = this.startConfigNumberOfGroups() ?? 0;
    const advancing = this.startConfigPlayersAdvancing() ?? 0;
    return groups * advancing;
  });

  constructor() {
    this.loadTournamentPage();

    effect(() => {
      const tournament = this.tournament();
      if (tournament) {
        this.updateTournamentName.set(tournament.name);
      }
    });

    effect(() => {
      const activeTab = this.activeTab();
      const visibleTabs = this.visibleTabs();
      if (!visibleTabs.includes(activeTab)) {
        this.activeTab.set(visibleTabs[0] ?? 'matches');
      }
    });

    this.state.activeTournamentId.set(this.tournamentId);
    this.state.refreshWallet(undefined, this.tournamentId);
  }

  ngOnDestroy(): void {
    this.state.activeTournamentId.set(null);
  }

  protected setTab(tab: TournamentTab): void {
    if (!this.visibleTabs().includes(tab)) return;
    this.activeTab.set(tab);
  }

  protected loadTournamentPage(): void {
    this.status.set('loading');
    this.errorMessage.set('');

    forkJoin({
      tournaments: this.tournamentsApi.findAll().pipe(map((p) => p.content)),
      allPlayers: this.playersApi.findAll(),
      teams: this.teamsApi.findAll(),
      players: this.tournamentsApi.findPlayers(this.tournamentId),
      rounds: this.tournamentsApi.findRounds(this.tournamentId),
      events: this.eventsApi.findByTournamentId(this.tournamentId),
    }).subscribe({
      next: ({ tournaments, allPlayers, teams, players, rounds, events }) => {
        const tournament = tournaments.find((item) => item.id === this.tournamentId) ?? null;
        this.tournament.set(tournament);
        this.allPlayers.set(allPlayers);
        this.teams.set([...teams].sort((a, b) => a.name.localeCompare(b.name)));
        this.players.set(players);
        this.rounds.set([...rounds].sort((a, b) => b.roundOrder - a.roundOrder));
        this.events.set(events);
        this.status.set(tournament ? 'success' : 'error');
        this.loadMarkets(events);
        this.loadScoreboard();
        this.loadBettingRanking();

        if (!tournament) {
          this.errorMessage.set('Torneio nao encontrado.');
        }
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel carregar os dados do torneio.');
      },
    });
  }

  protected startTournament(): void {
    const tournament = this.tournament();
    if (!tournament) return;

    if (tournament.format === 'LEAGUE_BRACKET') {
      this.openStartConfigModal();
    } else {
      this.tournamentsApi.start(this.tournamentId).subscribe({
        next: () => this.loadTournamentPage(),
        error: () => {
          this.status.set('error');
          this.errorMessage.set('Nao foi possivel iniciar o torneio.');
        },
      });
    }
  }

  protected syncFixtures(): void {
    const tournament = this.tournament();
    if (!tournament || this.isSyncing()) return;

    this.isSyncing.set(true);
    // Default sync window: last 7 days to next 30 days
    const from = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
    const to = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

    this.tournamentsApi.syncFixtures(this.tournamentId, from, to).subscribe({
      next: (result: SyncResultDto) => {
        this.isSyncing.set(false);
        this.toast.success(
          `Partidas sincronizadas: ${result.eventsCreated} criadas, ${result.eventsUpdated} atualizadas`,
        );
        this.loadTournamentPage();
      },
      error: () => {
        this.isSyncing.set(false);
        this.toast.error('Erro ao sincronizar partidas.');
      },
    });
  }

  protected syncOdds(): void {
    const tournament = this.tournament();
    if (!tournament || this.isSyncing()) return;

    this.isSyncing.set(true);
    this.tournamentsApi.syncOdds(this.tournamentId).subscribe({
      next: (result: OddsImportResultDto) => {
        this.isSyncing.set(false);
        this.toast.success(
          `Odds importadas: ${result.oddsUpdated} atualizadas, ${result.marketsCreated} mercados criados`,
        );
        this.loadTournamentPage();
      },
      error: () => {
        this.isSyncing.set(false);
        this.toast.error('Erro ao importar odds.');
      },
    });
  }

  protected openStartConfigModal(): void {
    const playerCount = this.players().length;
    this.startConfigLoading.set(true);
    this.startConfigErrorMessage.set('');

    this.tournamentsApi.getGroupConfig(playerCount).subscribe({
      next: (config) => {
        this.startConfigGroupOptions.set(config.validOptions);
        this.startConfigNumberOfGroups.set(config.validOptions[0]?.groupCount ?? null);
        this.startConfigPlayersAdvancing.set(2);
        this.startConfigLoading.set(false);
      },
      error: () => {
        this.startConfigErrorMessage.set('Nao foi possivel carregar configuracoes de grupo.');
        this.startConfigLoading.set(false);
      },
    });

    this.isStartConfigModalOpen.set(true);
  }

  protected closeStartConfigModal(): void {
    this.isStartConfigModalOpen.set(false);
    this.startConfigGroupOptions.set([]);
    this.startConfigNumberOfGroups.set(null);
    this.startConfigPlayersAdvancing.set(null);
    this.startConfigLoading.set(false);
    this.startConfigErrorMessage.set('');
  }

  protected setStartConfigNumberOfGroups(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    this.startConfigNumberOfGroups.set(Number.isFinite(value) && value > 0 ? value : null);
  }

  protected setStartConfigPlayersAdvancing(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.startConfigPlayersAdvancing.set(Number.isFinite(value) && value > 0 ? value : null);
  }

  protected startTournamentWithConfig(): void {
    const numberOfGroups = this.startConfigNumberOfGroups();
    const playersAdvancingPerGroup = this.startConfigPlayersAdvancing();

    if (!numberOfGroups || !playersAdvancingPerGroup) {
      this.startConfigErrorMessage.set('Preencha todos os campos.');
      return;
    }

    const totalAdvancing = numberOfGroups * playersAdvancingPerGroup;
    if (totalAdvancing < 2) {
      this.startConfigErrorMessage.set('Pelo menos 2 jogadores devem avancar ao mata-mata.');
      return;
    }

    this.startConfigLoading.set(true);
    this.startConfigErrorMessage.set('');

    this.tournamentsApi.start(this.tournamentId, { numberOfGroups, playersAdvancingPerGroup }).subscribe({
      next: () => {
        this.isStartConfigModalOpen.set(false);
        this.loadTournamentPage();
      },
      error: () => {
        this.startConfigLoading.set(false);
        this.startConfigErrorMessage.set('Nao foi possivel iniciar o torneio.');
      },
    });
  }

  protected openAdvanceModal(): void {
    this.advanceErrorMessage.set('');
    this.isAdvanceModalOpen.set(true);
  }

  protected closeAdvanceModal(): void {
    this.isAdvanceModalOpen.set(false);
    this.advanceErrorMessage.set('');
  }

  protected setUpdateTournamentName(event: Event): void {
    this.updateTournamentName.set((event.target as HTMLInputElement).value);
  }

  protected updateTournament(): void {
    const name = this.updateTournamentName().trim();
    if (!name || this.isUpdatingTournament()) return;

    this.isUpdatingTournament.set(true);
    this.tournamentsApi.update(this.tournamentId, { name }).subscribe({
      next: (tournament) => {
        this.tournament.set(tournament);
        this.isUpdatingTournament.set(false);
        this.toast.success('Torneio atualizado com sucesso.');
      },
      error: () => {
        this.isUpdatingTournament.set(false);
        this.toast.error('Nao foi possivel atualizar o torneio.');
      }
    });
  }

  protected cancelTournament(): void {
    if (this.isCancelingTournament()) return;
    if (!confirm('Deseja realmente cancelar este torneio?')) return;

    this.isCancelingTournament.set(true);
    this.tournamentsApi.cancel(this.tournamentId).subscribe({
      next: (tournament) => {
        this.tournament.set(tournament);
        this.isCancelingTournament.set(false);
        this.toast.success('Torneio cancelado com sucesso.');
      },
      error: () => {
        this.isCancelingTournament.set(false);
        this.toast.error('Nao foi possivel cancelar o torneio.');
      }
    });
  }

  protected deleteTournament(): void {
    if (this.isDeletingTournament()) return;
    if (!confirm('Deseja realmente excluir este torneio? Esta acao eh irreversivel e apagara todos os dados relacionados.')) return;

    this.isDeletingTournament.set(true);
    this.tournamentsApi.delete(this.tournamentId).subscribe({
      next: () => {
        this.toast.success('Torneio excluido com sucesso.');
        void this.router.navigate(['/']);
      },
      error: () => {
        this.isDeletingTournament.set(false);
        this.toast.error('Nao foi possivel excluir o torneio.');
      }
    });
  }

  protected advanceToBracket(): void {
    this.isAdvancing.set(true);
    this.advanceErrorMessage.set('');

    const hasCreatedEvents = this.events().some((event) => event.status === 'CREATED');
    const action = hasCreatedEvents
      ? this.tournamentsApi.forceAdvanceToBracket(this.tournamentId)
      : this.tournamentsApi.advanceToBracket(this.tournamentId);

    action.subscribe({
      next: () => {
        this.isAdvanceModalOpen.set(false);
        this.isAdvancing.set(false);
        this.loadTournamentPage();
      },
      error: () => {
        this.isAdvancing.set(false);
        this.advanceErrorMessage.set('Nao foi possivel avancar ao mata-mata.');
      },
    });
  }

  protected openAddPlayerModal(): void {
    this.selectedPlayerId.set(this.availablePlayers()[0]?.id ?? null);
    this.isAddPlayerModalOpen.set(true);
  }

  protected closeAddPlayerModal(): void {
    this.isAddPlayerModalOpen.set(false);
    this.selectedPlayerId.set(null);
    this.newPlayerName.set('');
  }

  protected setSelectedPlayer(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    this.selectedPlayerId.set(Number.isFinite(value) && value > 0 ? value : null);
  }

  protected setNewPlayerName(event: Event): void {
    this.newPlayerName.set((event.target as HTMLInputElement).value);
  }

  protected addPlayerToTournament(): void {
    const playerId = this.selectedPlayerId();
    if (!playerId) {
      return;
    }

    this.tournamentsApi.addPlayer(this.tournamentId, { playerId }).subscribe({
      next: () => {
        this.closeAddPlayerModal();
        this.loadTournamentPage();
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel adicionar o jogador ao torneio.');
      },
    });
  }

  protected createAndAddPlayer(): void {
    const name = this.newPlayerName().trim();
    if (!name) {
      return;
    }

    this.playersApi.create({ name }).subscribe({
      next: (createdPlayer) => {
        this.tournamentsApi.addPlayer(this.tournamentId, { playerId: createdPlayer.id }).subscribe({
          next: () => {
            this.closeAddPlayerModal();
            this.loadTournamentPage();
          },
          error: () => {
            this.status.set('error');
            this.errorMessage.set('Jogador criado, mas nao foi possivel adiciona-lo ao torneio.');
          },
        });
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel criar o jogador.');
      },
    });
  }

  protected openCreateMatchModal(): void {
    if (!this.canCreateManualMatch()) {
      this.toast.info('Criacao manual de partidas esta disponivel apenas para torneios manuais.');
      return;
    }

    this.selectedRoundId.set(this.rounds()[0] ? this.roundId(this.rounds()[0]) : null);
    this.selectedHomePlayerId.set(this.players()[0] ? this.tournamentPlayerPlayerId(this.players()[0]) : null);
    this.selectedAwayPlayerId.set(this.players()[1] ? this.tournamentPlayerPlayerId(this.players()[1]) : null);
    this.selectedGameDateTime.set(this.localDateTimeNow());
    this.isCreateCompletedEvent.set(false);
    this.isCreateByeMatch.set(false);
    this.completedHomeScoreInput.set('0');
    this.completedAwayScoreInput.set('0');
    this.createMatchErrorMessage.set('');
    this.isCreateMatchModalOpen.set(true);
  }

  protected closeCreateMatchModal(): void {
    this.isCreateMatchModalOpen.set(false);
    this.selectedRoundId.set(null);
    this.selectedHomePlayerId.set(null);
    this.selectedAwayPlayerId.set(null);
    this.selectedGameDateTime.set('');
    this.isCreateCompletedEvent.set(false);
    this.isCreateByeMatch.set(false);
    this.completedHomeScoreInput.set('0');
    this.completedAwayScoreInput.set('0');
    this.createMatchErrorMessage.set('');
  }

  protected setSelectedRound(event: Event): void {
    this.selectedRoundId.set(this.toPositiveNumber((event.target as HTMLSelectElement).value));
  }

  protected setSelectedHomePlayer(event: Event): void {
    this.selectedHomePlayerId.set(this.toPositiveNumber((event.target as HTMLSelectElement).value));
  }

  protected setSelectedAwayPlayer(event: Event): void {
    this.selectedAwayPlayerId.set(this.toPositiveNumber((event.target as HTMLSelectElement).value));
  }

  protected setSelectedGameDateTime(event: Event): void {
    this.selectedGameDateTime.set((event.target as HTMLInputElement).value);
  }

  protected setCreateCompletedEvent(event: Event): void {
    this.isCreateCompletedEvent.set((event.target as HTMLInputElement).checked);
  }

  protected setCreateByeMatch(event: Event): void {
    this.isCreateByeMatch.set((event.target as HTMLInputElement).checked);
  }

  protected setCompletedHomeScore(event: Event): void {
    this.completedHomeScoreInput.set((event.target as HTMLInputElement).value);
  }

  protected setCompletedAwayScore(event: Event): void {
    this.completedAwayScoreInput.set((event.target as HTMLInputElement).value);
  }

  protected createMatch(): void {
    if (!this.canCreateManualMatch()) {
      this.createMatchErrorMessage.set('Criacao manual de partidas esta disponivel apenas para torneios manuais.');
      return;
    }

    if (this.isCreateCompletedEvent()) {
      this.createCompletedMatch();
      return;
    }

    const roundId = this.selectedRoundId() ?? this.firstRoundId();
    const playerHomeId = this.selectedHomePlayerId() ?? this.firstTournamentPlayerId();
    const playerAwayId = this.selectedAwayPlayerId() ?? this.secondTournamentPlayerId();

    const validationError = this.createMatchValidationError(roundId, playerHomeId, playerAwayId);
    if (validationError) {
      this.createMatchErrorMessage.set(validationError);
      return;
    }

    this.eventsApi
      .create({
        tournamentId: this.tournamentId,
        roundId: roundId!,
        playerHomeId: playerHomeId!,
        playerAwayId: playerAwayId!,
        gameDatetime: this.toApiDateTime(this.selectedGameDateTime()) ?? undefined,
      })
      .subscribe({
        next: () => {
          this.closeCreateMatchModal();
          this.loadTournamentPage();
        },
        error: () => {
          this.createMatchErrorMessage.set('Nao foi possivel criar a partida. Verifique se a rodada e os jogadores pertencem a este torneio.');
        },
      });
  }

  private createCompletedMatch(): void {
    const roundId = this.selectedRoundId() ?? this.firstRoundId();
    const playerHomeId = this.selectedHomePlayerId();
    const playerAwayId = this.selectedAwayPlayerId();
    const homeScore = Number(this.completedHomeScoreInput());
    const awayScore = Number(this.completedAwayScoreInput());
    const isBye = this.isCreateByeMatch();

    const validationError = this.createCompletedMatchValidationError(roundId, playerHomeId, playerAwayId, homeScore, awayScore, isBye);
    if (validationError) {
      this.createMatchErrorMessage.set(validationError);
      return;
    }

    const request: CompletedEventRequestDto = {
      tournamentId: this.tournamentId,
      roundId: roundId!,
      homeScore,
      awayScore,
      isBye,
    };

    if (playerHomeId !== null) {
      request.playerHomeId = playerHomeId;
    }

    if (playerAwayId !== null) {
      request.playerAwayId = playerAwayId;
    }

    const gameDatetime = this.toApiDateTime(this.selectedGameDateTime());
    if (gameDatetime) {
      request.gameDatetime = gameDatetime;
    }

    this.eventsApi.createCompleted(request).subscribe({
      next: () => {
        this.closeCreateMatchModal();
        this.loadTournamentPage();
      },
      error: () => {
        this.createMatchErrorMessage.set('Nao foi possivel criar a partida finalizada. Verifique os dados informados.');
      },
    });
  }

  protected openTeamModal(player: TournamentPlayerResponseDto): void {
    this.teamPlayer.set(player);
    this.selectedTeamId.set(player.teamId ?? this.teams()[0]?.id ?? null);
    this.teamErrorMessage.set('');
    this.isTeamModalOpen.set(true);
  }

  protected closeTeamModal(): void {
    this.isTeamModalOpen.set(false);
    this.teamPlayer.set(null);
    this.selectedTeamId.set(null);
    this.teamErrorMessage.set('');
  }

  protected setSelectedTeam(event: Event): void {
    this.selectedTeamId.set(this.toPositiveNumber((event.target as HTMLSelectElement).value));
  }

  protected savePlayerTeam(): void {
    const player = this.teamPlayer();
    const teamId = this.selectedTeamId();

    if (!player || !teamId) {
      this.teamErrorMessage.set('Selecione um time.');
      return;
    }

    this.tournamentsApi
      .updatePlayerTeam(this.tournamentId, this.tournamentPlayerPlayerId(player), { teamId })
      .subscribe({
        next: () => {
          this.closeTeamModal();
          this.loadTournamentPage();
        },
        error: () => {
          this.teamErrorMessage.set('Nao foi possivel atualizar o time do jogador.');
        },
      });
  }

  protected removePlayer(): void {
    const player = this.teamPlayer();

    if (!player) {
      return;
    }

    if (confirm(`Tem certeza que deseja remover ${player.playerName} do torneio?`)) {
      this.tournamentsApi
        .removePlayer(this.tournamentId, this.tournamentPlayerPlayerId(player))
        .subscribe({
          next: () => {
            this.closeTeamModal();
            this.loadTournamentPage();
          },
          error: () => {
            this.teamErrorMessage.set('Nao foi possivel remover o jogador.');
          },
        });
    }
  }


  protected playerName(playerId: number | null): string {
    if (this.isUnresolvedSlot(playerId)) return 'A definir';
    return this.players().find((player) => this.tournamentPlayerPlayerId(player) === playerId)?.playerName ?? `Player #${playerId}`;
  }

  protected matchResultMarket(eventId: number): MarketResponseDto | null {
    const markets = this.markets()[eventId];
    if (!markets || markets.length === 0) return null;
    return markets.find((m) => m.marketType === 'MATCH_RESULT') ?? markets[0];
  }

  protected shouldShowOdds(event: EventResponseDto, market: MarketResponseDto): boolean {
    if (event.status !== 'CREATED' || market.status !== 'OPEN') return false;

    const tournamentType = this.tournament()?.type;
    if (tournamentType === 'REAL_FOOTBALL') {
      return this.isPreKickoff(event.gameDatetime);
    }

    return tournamentType === 'FIFA_MATCH';
  }

  protected oddsSourceLabel(): string {
    return this.isRealFootball() ? 'Mercado aberto' : 'Odds Elo';
  }

  protected eventPlayerLabel(event: EventResponseDto, side: 'home' | 'away'): string {
    if (this.isRealFootball()) {
      return side === 'home'
        ? this.translatedTeamName(event.teamHomeName, 'Casa')
        : this.translatedTeamName(event.teamAwayName, 'Fora');
    }
    const playerId = side === 'home' ? event.playerHomeId : event.playerAwayId;
    return this.isUnresolvedSlot(playerId) ? this.resolveSlotLabel(event, side) : this.playerName(playerId);
  }

  protected playerTeam(playerId: number | null): string {
    if (this.isUnresolvedSlot(playerId)) return 'Sem time';
    return this.translatedTeamName(
      this.players().find((player) => this.tournamentPlayerPlayerId(player) === playerId)?.teamName,
      'Sem time',
    );
  }

  protected translatedTeamName(value: string | null | undefined, fallback = ''): string {
    if (!value) return fallback;
    return TEAM_TRANSLATIONS[value] ?? value;
  }

  protected playerById(playerId: number | null): TournamentPlayerResponseDto | null {
    if (this.isUnresolvedSlot(playerId)) return null;
    return this.players().find((player) => this.tournamentPlayerPlayerId(player) === playerId) ?? null;
  }

  protected teamBadgeUrl(player: TournamentPlayerResponseDto | null): string | null {
    if (!player?.teamId) {
      return null;
    }

    return this.teams().find((team) => team.id === player.teamId)?.badgeUrl ?? null;
  }

  protected teamBadgeUrlById(teamId: number | null): string | null {
    if (!teamId) return null;
    return this.teams().find((team) => team.id === teamId)?.badgeUrl ?? null;
  }

  protected teamAbbreviationById(teamId: number | null): string {
    if (!teamId) return '--';
    return this.teams().find((team) => team.id === teamId)?.abbreviation ?? '--';
  }

  protected playerInitials(player: TournamentPlayerResponseDto | null): string {
    const source = player?.playerName ?? '?';
    return source
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  }

  protected teamAbbreviation(player: TournamentPlayerResponseDto | null): string {
    if (!player?.teamId) {
      return '--';
    }

    const translatedName = this.translatedTeamName(player.teamName);
    const abbreviation = this.teams().find((team) => team.id === player.teamId)?.abbreviation;
    return abbreviation ?? (translatedName.slice(0, 3).toUpperCase() || '--');
  }

  protected statusLabel(status: string): string {
    const labels: Record<string, string> = {
      CREATED: 'Criado',
      IN_PROGRESS: 'Em andamento',
      PENALTIES: 'Penaltis',
      COMPLETED: 'Finalizado',
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

  protected groupLabel(groupNumber: number | null): string {
    if (groupNumber === null || groupNumber === undefined) return '';
    return `Grupo ${String.fromCharCode(64 + groupNumber)}`;
  }

  protected groupDisplayLabel(group: GroupStandingsDto): string {
    if (this.isSingleLeagueBracketGroup()) return 'Liga';
    return group.groupName || this.groupLabel(group.groupNumber);
  }

  protected tournamentGroupLabel(groupNumber: number | null): string {
    if (this.isSingleLeagueBracketGroup()) return 'Liga';
    return this.groupLabel(groupNumber);
  }

  protected isQualifyingPosition(index: number): boolean {
    const tournament = this.tournament();
    if (tournament?.format !== 'LEAGUE_BRACKET' || !tournament.playersAdvancingPerGroup) return false;
    return index < tournament.playersAdvancingPerGroup;
  }

  protected eliminationRoundLabel(eliminationRound: string): string {
    const labels: Record<string, string> = {
      'Champion': 'Campeao',
      'Runner-up': 'Vice-campeao',
      '3rd Place': '3º lugar',
      '4th Place': '4º lugar',
      'Semi-Final': 'Semi-finalista',
      'Quarter-Final': 'Quartas de final',
      'Round of 16': 'Oitavas de final',
      'Round of 32': '16-avos de final',
    };
    return labels[eliminationRound] ?? eliminationRound;
  }

  protected matchWinner(event: EventResponseDto): 'home' | 'away' | 'none' {
    if (event.status !== 'COMPLETED') return 'none';
    if (event.penaltiesHome !== null && event.penaltiesAway !== null) {
      return event.penaltiesHome > event.penaltiesAway ? 'home' : 'away';
    }
    if (event.homeScore !== null && event.awayScore !== null) {
      if (event.homeScore > event.awayScore) return 'home';
      if (event.awayScore > event.homeScore) return 'away';
    }
    return 'none';
  }

  protected isUnresolvedSlot(playerId: number | null): boolean {
    return playerId === null || playerId === 0;
  }

  protected resolveSlotLabel(event: EventResponseDto, side: 'home' | 'away'): string {
    const sourceEvent = this.sourceEventForSlot(event, side);
    if (!sourceEvent) return 'A definir';

    const sourceWinner = this.matchWinner(sourceEvent);
    if (sourceWinner !== 'none') {
      if (event.thirdPlaceMatch) {
        return sourceWinner === 'home'
          ? (sourceEvent.playerAwayName ?? this.playerName(sourceEvent.playerAwayId))
          : (sourceEvent.playerHomeName ?? this.playerName(sourceEvent.playerHomeId));
      }

      return sourceWinner === 'home'
        ? (sourceEvent.playerHomeName ?? this.playerName(sourceEvent.playerHomeId))
        : (sourceEvent.playerAwayName ?? this.playerName(sourceEvent.playerAwayId));
    }

    const sourceHomeName = this.eventSideName(sourceEvent, 'home');
    const sourceAwayName = this.eventSideName(sourceEvent, 'away');
    if (sourceHomeName && sourceAwayName) {
      return `${sourceHomeName} ou ${sourceAwayName}`;
    }

    const sourceRound = this.rounds().find((round) => this.roundId(round) === sourceEvent.roundId);
    const roundName = sourceRound?.name ?? 'rodada anterior';
    return `${event.thirdPlaceMatch ? 'Perdedor' : 'Vencedor'} de ${roundName}`;
  }

  private eventSideName(event: EventResponseDto, side: 'home' | 'away'): string | null {
    const playerName = side === 'home' ? event.playerHomeName : event.playerAwayName;
    const playerId = side === 'home' ? event.playerHomeId : event.playerAwayId;
    if (playerName) return playerName;
    if (!this.isUnresolvedSlot(playerId)) return this.playerName(playerId);
    return null;
  }

  private sourceEventForSlot(event: EventResponseDto, side: 'home' | 'away'): EventResponseDto | null {
    const explicitSourceId = side === 'home' ? event.homeSourceEventId : event.awaySourceEventId;
    const sideIndex = side === 'home' ? 0 : 1;

    if (explicitSourceId) {
      return this.events().find((source) => source.id === explicitSourceId) ?? null;
    }

    const inferredSources = this.inferredSourceEvents(event, explicitSourceId);
    return inferredSources[sideIndex] ?? (explicitSourceId ? this.events().find((source) => source.id === explicitSourceId) ?? null : null);
  }

  private inferredSourceEvents(event: EventResponseDto, sourceEventId: number | null): EventResponseDto[] {
    const directSources = this.events()
      .filter((source) => source.nextRoundEventId === event.id)
      .sort((a, b) => a.id - b.id);

    if (directSources.length >= 2) {
      return directSources;
    }

    const sourceEvent = sourceEventId ? this.events().find((source) => source.id === sourceEventId) : null;
    if (!sourceEvent) return directSources;

    return this.events()
      .filter((source) => source.roundId === sourceEvent.roundId)
      .sort((a, b) => a.id - b.id);
  }

  protected formatDateTime(value: string | null): string {
    if (!value) {
      return 'Sem data';
    }

    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }

  private loadMarkets(events: EventResponseDto[]): void {
    const createdEvents = events.filter((e) => e.status === 'CREATED');
    if (createdEvents.length === 0) return;

    forkJoin(
      createdEvents.map((event) =>
        this.marketsApi.findByEventId(event.id).pipe(
          catchError(() => of([] as MarketResponseDto[])),
        ),
      ),
    ).subscribe({
      next: (marketsList) => {
        const marketMap: Record<number, MarketResponseDto[]> = {};
        marketsList.forEach((markets, index) => {
          if (markets.length > 0) {
            marketMap[createdEvents[index].id] = markets;
          }
        });
        this.markets.set(marketMap);
      },
    });
  }

  private compareRoundEvents(a: EventResponseDto, b: EventResponseDto): number {
    const statusDiff = this.eventStatusSortValue(a) - this.eventStatusSortValue(b);
    if (statusDiff !== 0) return statusDiff;

    const dateDiff = this.eventDateSortValue(b) - this.eventDateSortValue(a);
    if (dateDiff !== 0) return dateDiff;

    return b.id - a.id;
  }

  private eventStatusSortValue(event: EventResponseDto): number {
    return event.status === 'CREATED' ? 0 : 1;
  }

  private eventDateSortValue(event: EventResponseDto): number {
    if (!event.gameDatetime) return 0;
    const value = new Date(event.gameDatetime).getTime();
    return Number.isFinite(value) ? value : 0;
  }

  private isPreKickoff(value: string | null): boolean {
    if (!value) return false;
    const kickoff = new Date(value).getTime();
    return Number.isFinite(kickoff) && kickoff > Date.now();
  }

  private loadScoreboard(): void {
    this.tournamentsApi.getScoreboard(this.tournamentId).pipe(
      catchError(() => of(null)),
    ).subscribe({
      next: (scoreboard) => this.scoreboard.set(scoreboard),
    });
  }

  protected loadBettingRanking(): void {
    this.bettingRankingStatus.set('loading');
    this.bettingRankingError.set('');

    this.tournamentsApi.getRanking(this.tournamentId).pipe(
      catchError(() => {
        this.bettingRankingStatus.set('error');
        this.bettingRankingError.set('Nao foi possivel carregar o ranking de apostas.');
        return of([]);
      })
    ).subscribe({
      next: (ranking) => {
        this.bettingRanking.set(ranking);
        if (this.bettingRankingStatus() !== 'error') {
          this.bettingRankingStatus.set('success');
        }
      },
    });
  }

  private isPlayerActive(player: PlayerResponseDto): boolean {
    return player.active ?? player.isActive ?? false;
  }

  private toPositiveNumber(value: string): number | null {
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  protected roundId(round: TournamentRoundResponseDto): number {
    return round.roundId ?? round.id ?? (round as unknown as { round_id?: number }).round_id ?? 0;
  }

  protected tournamentPlayerPlayerId(player: TournamentPlayerResponseDto): number {
    return player.playerId ?? (player as unknown as { player_id?: number }).player_id ?? 0;
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

  private firstRoundId(): number | null {
    const first = this.rounds()[0];
    return first ? this.roundId(first) : null;
  }

  private firstTournamentPlayerId(): number | null {
    const first = this.players()[0];
    return first ? this.tournamentPlayerPlayerId(first) : null;
  }

  private secondTournamentPlayerId(): number | null {
    const second = this.players()[1];
    return second ? this.tournamentPlayerPlayerId(second) : null;
  }

  protected isOutcomeSelected(eventId: number, marketType: string, outcomeId: number): boolean {
    return this.betCart.entries().some(
      (e) => e.eventId === eventId && e.marketType === marketType && e.outcomeId === outcomeId,
    );
  }

  protected addOutcomeToCart(
    event: EventResponseDto,
    market: MarketResponseDto,
    outcome: { id: number; name: string; odd: number },
  ): void {
    const tournament = this.tournament();
    if (!tournament) return;

    const homeLabel = this.isRealFootball()
      ? this.translatedTeamName(event.teamHomeName, 'Casa')
      : (event.playerHomeName ?? this.playerName(event.playerHomeId));
    const awayLabel = this.isRealFootball()
      ? this.translatedTeamName(event.teamAwayName, 'Fora')
      : (event.playerAwayName ?? this.playerName(event.playerAwayId));

    const entry: BetCartEntry = {
      eventId: event.id,
      marketId: market.id,
      outcomeId: outcome.id,
      outcomeName: this.outcomeName(outcome.name, event),
      odd: outcome.odd,
      eventLabel: `${homeLabel} vs ${awayLabel}`,
      tournamentId: tournament.id,
      tournamentName: tournament.name,
      marketType: market.marketType,
    };

    this.betCart.addEntry(entry);
  }

  protected outcomeName(outcomeName: string, event: EventResponseDto): string {
    if (this.isRealFootball()) {
      const labels: Record<string, string> = {
        'Vitória Casa': this.translatedTeamName(event.teamHomeName, 'Casa'),
        'Empate': 'Empate',
        'Vitória Fora': this.translatedTeamName(event.teamAwayName, 'Fora'),
      };
      return labels[outcomeName] ?? this.translatedTeamName(outcomeName);
    }
    const labels: Record<string, string> = {
      'Vitória Casa': event.playerHomeName ?? this.playerName(event.playerHomeId),
      'Empate': 'Empate',
      'Vitória Fora': event.playerAwayName ?? this.playerName(event.playerAwayId),
    };
    return labels[outcomeName] ?? outcomeName;
  }

  protected bracketSideLabel(event: EventResponseDto, side: 'home' | 'away'): string {
    if (this.isRealFootball()) return this.eventPlayerLabel(event, side);
    const playerId = side === 'home' ? event.playerHomeId : event.playerAwayId;
    return this.isUnresolvedSlot(playerId) ? this.resolveSlotLabel(event, side) : this.playerName(playerId);
  }

  protected bracketScoreLabel(event: EventResponseDto, side: 'home' | 'away'): string {
    if (event.status !== 'COMPLETED') return '—';
    const score = side === 'home' ? event.homeScore : event.awayScore;
    return score === null ? '—' : String(score);
  }

  protected isFinalBracketColumn(roundId: number): boolean {
    const columns = this.bracketColumns();
    return columns.length > 0 && this.roundId(columns[columns.length - 1].round) === roundId;
  }

  protected isChampionMatch(roundId: number, event: EventResponseDto): boolean {
    if (!this.isFinalBracketColumn(roundId)) return false;

    const finalColumn = this.bracketColumns().find((column) => this.roundId(column.round) === roundId);
    if (!finalColumn || finalColumn.events.length === 0) return false;

    const nonThirdPlaceEvents = finalColumn.events.filter((item) => !item.thirdPlaceMatch);
    const finalEvent = nonThirdPlaceEvents[nonThirdPlaceEvents.length - 1] ?? finalColumn.events[finalColumn.events.length - 1];
    return finalEvent.id === event.id;
  }

  private isSingleLeagueBracketGroup(): boolean {
    const tournament = this.tournament();
    return tournament?.format === 'LEAGUE_BRACKET' && tournament.numberOfGroups === 1;
  }

  private createMatchValidationError(
    roundId: number | null,
    playerHomeId: number | null,
    playerAwayId: number | null,
  ): string {
    if (!roundId) {
      return 'A rodada selecionada nao possui id. O backend precisa retornar roundId em GET /tournaments/{id}/rounds.';
    }

    if (!playerHomeId) {
      return 'Selecione o jogador mandante.';
    }

    if (!playerAwayId) {
      return 'Selecione o jogador visitante.';
    }

    if (playerHomeId === playerAwayId) {
      return 'Mandante e visitante precisam ser jogadores diferentes.';
    }

    return '';
  }

  private createCompletedMatchValidationError(
    roundId: number | null,
    playerHomeId: number | null,
    playerAwayId: number | null,
    homeScore: number,
    awayScore: number,
    isBye: boolean,
  ): string {
    if (!roundId) {
      return 'A rodada selecionada nao possui id. O backend precisa retornar roundId em GET /tournaments/{id}/rounds.';
    }

    if (!Number.isInteger(homeScore) || !Number.isInteger(awayScore) || homeScore < 0 || awayScore < 0) {
      return 'Informe placares validos.';
    }

    if (!playerHomeId) {
      return 'Selecione o jogador mandante.';
    }

    if (!playerAwayId) {
      return 'Selecione o jogador visitante.';
    }

    if (playerHomeId === playerAwayId) {
      return 'Mandante e visitante precisam ser jogadores diferentes.';
    }

    return '';
  }
}
