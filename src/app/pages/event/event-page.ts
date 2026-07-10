import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';

import {
  BetSlipResponseDto,
  EventResponseDto,
  MarketResponseDto,
  OutcomeResponseDto,
  PatchEventPlayersRequestDto,
  TeamResponseDto,
  TournamentPlayerResponseDto,
  TournamentResponseDto,
  TournamentRoundResponseDto,
  WalletResponseDto,
} from '../../services/api/api.models';
import { BetsApi } from '../../services/api/bets-api';
import { EventsApi } from '../../services/api/events-api';
import { MarketsApi } from '../../services/api/markets-api';
import { TeamsApi } from '../../services/api/teams-api';
import { TournamentsApi } from '../../services/api/tournaments-api';
import { WalletApi } from '../../services/api/wallet-api';
import { AuthService } from '../../services/auth.service';
import { ResenhaBetState } from '../../services/resenhabet-state';
import { ToastService } from '../../services/toast.service';
import { WebSocketService } from '../../services/websocket.service';
import { BetCartEntry, BetCartService } from '../../services/bet-cart.service';
import { MarketAccordionComponent } from '../../components/betting/market-accordion';
import { AppButtonComponent } from '../../components/ui/app-button';
import { AppEmptyStateComponent } from '../../components/ui/app-empty-state';
import { AppSectionHeaderComponent } from '../../components/ui/app-section-header';
import { TEAM_TRANSLATIONS } from '../../pipes/team-translations';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';
type MarketGroup = 'ALL' | 'MAIN' | 'GOALS' | 'OTHER';

@Component({
  selector: 'app-event-page',
  imports: [
    CommonModule,
    RouterLink,
    MarketAccordionComponent,
    AppButtonComponent,
    AppEmptyStateComponent,
    AppSectionHeaderComponent,
  ],
  templateUrl: './event-page.html',
})
export class EventPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly eventsApi = inject(EventsApi);
  private readonly tournamentsApi = inject(TournamentsApi);
  private readonly teamsApi = inject(TeamsApi);
  private readonly marketsApi = inject(MarketsApi);
  private readonly betsApi = inject(BetsApi);
  private readonly walletApi = inject(WalletApi);
  private readonly auth = inject(AuthService);
  private readonly toastService = inject(ToastService);
  protected readonly state = inject(ResenhaBetState);
  private readonly webSocketService = inject(WebSocketService);
  protected readonly betCart = inject(BetCartService);

  protected readonly eventId = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly status = signal<LoadStatus>('idle');
  protected readonly errorMessage = signal('');
  protected readonly actionErrorMessage = signal('');
  protected readonly event = signal<EventResponseDto | null>(null);
  protected readonly tournament = signal<TournamentResponseDto | null>(null);
  protected readonly teams = signal<TeamResponseDto[]>([]);
  protected readonly players = signal<TournamentPlayerResponseDto[]>([]);
  protected readonly rounds = signal<TournamentRoundResponseDto[]>([]);
  protected readonly homeScoreInput = signal('0');
  protected readonly awayScoreInput = signal('0');
  protected readonly isSubmitting = signal(false);
  protected readonly isPenaltyModalOpen = signal(false);
  protected readonly penaltiesHomeInput = signal('0');
  protected readonly penaltiesAwayInput = signal('0');
  protected readonly selectedHomePlayerId = signal<number | null>(null);
  protected readonly selectedAwayPlayerId = signal<number | null>(null);

  protected readonly markets = signal<MarketResponseDto[]>([]);
  protected readonly selectedMarketGroup = signal<MarketGroup>('ALL');
  protected readonly marketAccordionState = signal<Record<number, boolean>>({});
  protected readonly exactScoreExpanded = signal<Record<number, boolean>>({});
  protected readonly walletBalance = signal<number>(0);
  protected readonly walletInitialBalance = signal<number>(0);
  protected readonly userBet = signal<BetSlipResponseDto | null>(null);

  protected readonly editGameDatetime = signal<string | null>(null);
  protected readonly isEditingEvent = signal(false);

  protected readonly depositUserId = signal<number | null>(null);
  protected readonly depositAmount = signal<string>('');
  protected readonly depositAllAmount = signal<string>('');
  protected readonly depositMessage = signal('');
  protected readonly depositAllMessage = signal('');
  protected readonly isDepositing = signal(false);

  protected readonly eventBets = signal<BetSlipResponseDto[]>([]);
  protected readonly eventBetsLoading = signal(false);
  protected readonly eventBetsError = signal('');

  protected readonly adminPanelOpen = signal(false);
  protected readonly walletPanelOpen = signal(false);
  protected readonly adminSectionOpen = signal<Record<'mercados' | 'partida' | 'datas' | 'zona', boolean>>({
    mercados: true,
    partida: false,
    datas: false,
    zona: false,
  });
  protected readonly confirmingDeleteEvent = signal(false);

  private scoreDraftTouched = false;

  protected readonly homePlayer = computed(() => {
    const event = this.event();
    return event ? this.findPlayer(event.playerHomeId) : null;
  });

  protected readonly awayPlayer = computed(() => {
    const event = this.event();
    return event ? this.findPlayer(event.playerAwayId) : null;
  });

  protected readonly round = computed(() => {
    const event = this.event();
    return event ? this.rounds().find((round) => this.roundId(round) === event.roundId) ?? null : null;
  });

  protected readonly isRealFootball = computed(() => this.tournament()?.type === 'REAL_FOOTBALL');

  protected readonly canManageGroup = this.auth.canManageGroup;

  protected readonly isSystemAdmin = this.auth.isSystemAdmin;

  protected readonly canManageFifaEvent = computed(() => !this.isRealFootball() && this.auth.canManageGroup());

  protected readonly canManageRealFootballEvent = computed(() => this.isRealFootball() && this.auth.isSystemAdmin());

  protected readonly canManageEvent = computed(() => this.canManageFifaEvent() || this.canManageRealFootballEvent());

  protected readonly canManageMarket = computed(() =>
    this.isRealFootball() ? this.auth.isSystemAdmin() : this.auth.canManageGroup(),
  );

  protected readonly showManagementPanel = computed(() => this.canManageEvent() || this.auth.canManageGroup());

  protected readonly canStartMatch = computed(() => this.canManageEvent() && this.event()?.status === 'CREATED');
  protected readonly canReopenMatch = computed(() => this.canManageEvent() && this.event()?.status === 'COMPLETED');
  protected readonly canResetMatch = computed(() => this.canManageEvent() && this.event()?.status === 'IN_PROGRESS');
  protected readonly canUpdateScore = computed(() => this.canManageEvent() && (this.event()?.status === 'IN_PROGRESS' || this.event()?.status === 'PENALTIES'));
  protected readonly canEndMatch = computed(() => this.canManageEvent() && (this.event()?.status === 'IN_PROGRESS' || this.event()?.status === 'PENALTIES'));
  protected readonly canSubmitPenalties = computed(() => this.canManageEvent() && this.event()?.status === 'PENALTIES');
  protected readonly penaltiesAreEqual = computed(() => Number(this.penaltiesHomeInput()) === Number(this.penaltiesAwayInput()));
  protected readonly canAssignPlayers = computed(() =>
    this.canManageFifaEvent() &&
    this.event()?.status === 'CREATED' &&
    this.event()?.homeSourceEventId == null &&
    this.event()?.awaySourceEventId == null &&
    !this.isRealFootball()
  );

  protected readonly adminScopeHint = computed(() => {
    if (this.isRealFootball()) {
      return this.canManageEvent()
        ? 'Futebol real compartilhado: ações de partida e mercados exigem admin do sistema.'
        : 'Futebol real compartilhado: apenas a recarga de carteiras do seu grupo está disponível aqui.';
    }

    return 'Partida FIFA do grupo ativo: ações de partida e mercados seguem a permissão do grupo.';
  });

  private static readonly MARKET_DISPLAY_ORDER: Record<string, number> = {
    MATCH_RESULT: 0,
    QUALIFY: 1,
  };

  protected readonly sortedMarkets = computed(() => {
    const orderOf = (market: MarketResponseDto) =>
      EventPage.MARKET_DISPLAY_ORDER[market.marketType] ?? Number.MAX_SAFE_INTEGER;

    return [...this.markets()].sort((a, b) => orderOf(a) - orderOf(b));
  });

  protected readonly visibleMarkets = computed(() => {
    const event = this.event();
    const selectedGroup = this.selectedMarketGroup();

    return this.sortedMarkets().filter((market) => {
      if (event?.isKnockout === false && market.marketType === 'QUALIFY') {
        return false;
      }

      return selectedGroup === 'ALL' || this.marketGroupForType(market.marketType) === selectedGroup;
    });
  });

  protected readonly activeMarket = computed(() =>
    this.visibleMarkets()[0] ?? this.markets()[0] ?? null
  );

  protected readonly isLive = computed(() => {
    const status = this.event()?.status;
    return status === 'IN_PROGRESS' || status === 'PENALTIES';
  });

  protected readonly liveLabel = computed(() =>
    this.event()?.status === 'PENALTIES' ? 'AO VIVO • PÊNALTIS' : 'AO VIVO',
  );

  protected readonly anyMarketOpen = computed(() => this.markets().some((market) => market.status === 'OPEN'));

  protected readonly primaryMatchLabel = computed(() => {
    switch (this.event()?.status) {
      case 'CREATED':
        return 'Iniciar Partida';
      case 'IN_PROGRESS':
        return 'Encerrar Partida';
      case 'PENALTIES':
        return 'Registrar Pênaltis';
      case 'CANCELLED':
        return 'Partida Cancelada';
      default:
        return 'Partida Encerrada';
    }
  });

  protected readonly primaryMatchDisabled = computed(() => {
    const status = this.event()?.status;
    return this.isSubmitting() || status === 'COMPLETED' || status === 'CANCELLED';
  });

  protected readonly marketsClosedNote = computed(() => {
    const event = this.event();
    const markets = this.markets();

    if (!event || markets.length === 0) {
      return null;
    }

    const anyOpen = markets.some((market) => market.status === 'OPEN');
    if (anyOpen) {
      return null;
    }

    return event.status === 'CANCELLED'
      ? 'Partida cancelada. Mercados encerrados.'
      : 'Partida finalizada. Mercados fechados.';
  });

  constructor() {
    this.loadEventPage();
    if (this.state.loginUsers().length === 0) {
      this.state.loadLoginPlayers();
    }
  }

  ngOnInit(): void {
    this.webSocketService.connect();

    this.webSocketService
      .subscribe<EventResponseDto>(`/topic/events/${this.eventId}`)
      .subscribe((updatedEvent) => {
        this.event.set(updatedEvent);
        this.selectedHomePlayerId.set(updatedEvent.playerHomeId);
        this.selectedAwayPlayerId.set(updatedEvent.playerAwayId);

        if (!this.scoreDraftTouched) {
          this.seedScoreInputs(updatedEvent);
        }

        if (this.editGameDatetime() === null) {
          this.seedDatetimeInput(updatedEvent);
        }

        if (updatedEvent.status === 'COMPLETED' || updatedEvent.status === 'PENALTIES') {
          this.checkBetResolution();
        }
      });

    this.webSocketService
      .subscribe<MarketResponseDto>(`/topic/markets/${this.eventId}`)
      .subscribe((updatedMarket) => {
        this.markets.update((current) => {
          const idx = current.findIndex((m) => m.id === updatedMarket.id);
          if (idx >= 0) {
            const updated = [...current];
            updated[idx] = updatedMarket;
            return updated;
          }
          return [...current, updatedMarket];
        });
      });

    const userId = this.auth.currentUser()?.id;
    if (userId) {
      this.webSocketService
        .subscribe<WalletResponseDto>(`/topic/wallet/${userId}`)
        .subscribe((wallet) => {
          this.walletInitialBalance.set(wallet.initialBalance ?? wallet.balance);
          this.walletBalance.set(wallet.balance);
          this.state.walletBalance.set(wallet.balance);
        });
    }
  }

  ngOnDestroy(): void {
    this.webSocketService.disconnect();
    this.state.activeTournamentId.set(null);
  }

  protected loadEventPage(): void {
    if (!Number.isFinite(this.eventId) || this.eventId <= 0) {
      this.status.set('error');
      this.errorMessage.set('Partida invalida.');
      return;
    }

    this.status.set('loading');
    this.errorMessage.set('');

    this.eventsApi.findById(this.eventId).subscribe({
      next: (event) => this.loadEventContext(event),
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel carregar a partida.');
      },
    });
  }

  protected startMatch(): void {
    this.isSubmitting.set(true);
    this.actionErrorMessage.set('');

    this.eventsApi.start(this.eventId).subscribe({
      next: (event) => {
        this.event.set(event);
        this.seedScoreInputs(event);
        this.isSubmitting.set(false);
        this.refreshMarket(event);
        this.loadEventBets();
      },
      error: () => {
        this.isSubmitting.set(false);
        this.actionErrorMessage.set('Nao foi possivel iniciar a partida.');
      },
    });
  }

  protected reopenMatch(): void {
    const event = this.event();
    if (!event || this.isSubmitting()) return;
    if (!window.confirm('Reabrir esta partida? A reversao das apostas sera aplicada.')) return;

    this.isSubmitting.set(true);
    this.actionErrorMessage.set('');

    this.eventsApi.reopen(this.eventId).subscribe({
      next: (updatedEvent) => {
        this.scoreDraftTouched = false;
        this.event.set(updatedEvent);
        this.seedScoreInputs(updatedEvent);
        this.isPenaltyModalOpen.set(false);
        this.penaltiesHomeInput.set('0');
        this.penaltiesAwayInput.set('0');
        this.isSubmitting.set(false);
        this.refreshMarket(updatedEvent);
        this.loadUserData(updatedEvent);
        this.loadEventBets();
      },
      error: () => {
        this.isSubmitting.set(false);
        this.actionErrorMessage.set('Nao foi possivel reabrir a partida.');
      },
    });
  }

  protected resetMatch(): void {
    const event = this.event();
    if (!event || this.isSubmitting()) return;
    if (!window.confirm('Resetar esta partida? O placar sera limpo e a partida voltara para Criada.')) return;

    this.isSubmitting.set(true);
    this.actionErrorMessage.set('');

    this.eventsApi.reset(this.eventId).subscribe({
      next: (updatedEvent) => {
        this.scoreDraftTouched = false;
        this.event.set(updatedEvent);
        this.seedScoreInputs(updatedEvent);
        this.isPenaltyModalOpen.set(false);
        this.penaltiesHomeInput.set('0');
        this.penaltiesAwayInput.set('0');
        this.isSubmitting.set(false);
        this.refreshMarket(updatedEvent);
        this.loadUserData(updatedEvent);
        this.loadEventBets();
      },
      error: () => {
        this.isSubmitting.set(false);
        this.actionErrorMessage.set('Nao foi possivel resetar a partida.');
      },
    });
  }

  protected assignPlayers(): void {
    const playerHomeId = this.selectedHomePlayerId();
    const playerAwayId = this.selectedAwayPlayerId();
    const request: PatchEventPlayersRequestDto = {};
    if (playerHomeId !== null) request.playerHomeId = playerHomeId;
    if (playerAwayId !== null) request.playerAwayId = playerAwayId;

    if (!request.playerHomeId && !request.playerAwayId) {
      this.actionErrorMessage.set('Selecione ao menos um jogador.');
      return;
    }

    this.isSubmitting.set(true);
    this.actionErrorMessage.set('');

    this.eventsApi.updatePlayers(this.eventId, request).subscribe({
      next: (event) => {
        this.event.set(event);
        this.isSubmitting.set(false);
        this.refreshMarket(event);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.actionErrorMessage.set('Nao foi possivel atribuir jogadores.');
      },
    });
  }

  protected updateScore(): void {
    const homeScore = Number(this.homeScoreInput());
    const awayScore = Number(this.awayScoreInput());

    if (!Number.isInteger(homeScore) || !Number.isInteger(awayScore) || homeScore < 0 || awayScore < 0) {
      this.actionErrorMessage.set('Informe placares validos.');
      return;
    }

    this.submitScore(homeScore, awayScore);
  }

  protected stepHomeScore(delta: number): void {
    const next = Math.max(0, (Number(this.homeScoreInput()) || 0) + delta);
    this.homeScoreInput.set(String(next));
    this.scoreDraftTouched = true;
    this.submitScore(next, Number(this.awayScoreInput()) || 0);
  }

  protected stepAwayScore(delta: number): void {
    const next = Math.max(0, (Number(this.awayScoreInput()) || 0) + delta);
    this.awayScoreInput.set(String(next));
    this.scoreDraftTouched = true;
    this.submitScore(Number(this.homeScoreInput()) || 0, next);
  }

  private submitScore(homeScore: number, awayScore: number): void {
    this.isSubmitting.set(true);
    this.actionErrorMessage.set('');

    this.eventsApi.updateScore(this.eventId, { homeScore, awayScore }).subscribe({
      next: (event) => {
        this.scoreDraftTouched = false;
        this.event.set(event);
        this.seedScoreInputs(event);
        this.isSubmitting.set(false);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.actionErrorMessage.set('Nao foi possivel atualizar o placar.');
      },
    });
  }

  protected primaryMatchAction(): void {
    switch (this.event()?.status) {
      case 'CREATED':
        this.startMatch();
        return;
      case 'IN_PROGRESS':
        this.endMatch();
        return;
      case 'PENALTIES':
        this.isPenaltyModalOpen.set(true);
        return;
      default:
        return;
    }
  }

  protected toggleAdminSection(section: 'mercados' | 'partida' | 'datas' | 'zona'): void {
    this.adminSectionOpen.update((current) => ({ ...current, [section]: !current[section] }));
    if (section !== 'zona') {
      this.confirmingDeleteEvent.set(false);
    }
  }

  protected endMatch(): void {
    const event = this.event();
    if (!event) return;

    const homeScore = this.scoreValue(event.homeScore);
    const awayScore = this.scoreValue(event.awayScore);
    const isKnockoutDraw = event.isKnockout && homeScore === awayScore;

    const confirmMessage = isKnockoutDraw
      ? 'Encerrar esta partida com empate? A partida ira para penaltis.'
      : 'Encerrar esta partida com o placar atual?';

    if (!window.confirm(confirmMessage)) {
      return;
    }

    this.isSubmitting.set(true);
    this.actionErrorMessage.set('');

    this.eventsApi.finish(this.eventId).subscribe({
      next: (event) => {
        this.scoreDraftTouched = false;
        this.event.set(event);
        this.seedScoreInputs(event);
        this.isSubmitting.set(false);
        this.refreshMarket(event);
        this.checkBetResolution();
        this.loadEventBets();

        // If backend transitioned to PENALTIES, open penalty modal
        if (event.status === 'PENALTIES') {
          this.penaltiesHomeInput.set('0');
          this.penaltiesAwayInput.set('0');
          this.isPenaltyModalOpen.set(true);
          this.actionErrorMessage.set('');
        }
      },
      error: () => {
        this.isSubmitting.set(false);
        this.actionErrorMessage.set('Nao foi possivel encerrar a partida.');
      },
    });
  }

  protected submitPenalties(): void {
    const pHome = Number(this.penaltiesHomeInput());
    const pAway = Number(this.penaltiesAwayInput());

    if (!Number.isInteger(pHome) || !Number.isInteger(pAway) || pHome < 0 || pAway < 0) {
      this.actionErrorMessage.set('Informe placares de penaltis validos.');
      return;
    }

    this.isSubmitting.set(true);
    this.actionErrorMessage.set('');

    this.eventsApi.recordPenalties(this.eventId, { penaltiesHome: pHome, penaltiesAway: pAway }).subscribe({
      next: (event) => {
        this.event.set(event);
        this.isSubmitting.set(false);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.actionErrorMessage.set('Nao foi possivel registrar os penaltis.');
      },
    });
  }

  protected finalizeWithPenalties(): void {
    const pHome = Number(this.penaltiesHomeInput());
    const pAway = Number(this.penaltiesAwayInput());

    if (!Number.isInteger(pHome) || !Number.isInteger(pAway) || pHome < 0 || pAway < 0) {
      this.actionErrorMessage.set('Informe placares de penaltis validos.');
      return;
    }

    if (pHome === pAway) {
      this.actionErrorMessage.set('Os penaltis precisam ser diferentes para finalizar.');
      return;
    }

    this.isSubmitting.set(true);
    this.actionErrorMessage.set('');

    this.eventsApi.recordPenalties(this.eventId, {
      penaltiesHome: pHome,
      penaltiesAway: pAway,
      status: 'COMPLETED',
    }).subscribe({
      next: (event) => {
        this.scoreDraftTouched = false;
        this.event.set(event);
        this.seedScoreInputs(event);
        this.isSubmitting.set(false);
        this.isPenaltyModalOpen.set(false);
        this.refreshMarket(event);
        this.checkBetResolution();
        this.loadEventBets();
      },
      error: () => {
        this.isSubmitting.set(false);
        this.actionErrorMessage.set('Nao foi possivel finalizar a partida.');
      },
    });
  }

  protected closePenaltyModal(): void {
    this.isPenaltyModalOpen.set(false);
    this.penaltiesHomeInput.set('0');
    this.penaltiesAwayInput.set('0');
    this.actionErrorMessage.set('');
  }

  protected setPenaltiesHome(event: Event): void {
    this.penaltiesHomeInput.set((event.target as HTMLInputElement).value);
  }

  protected setPenaltiesAway(event: Event): void {
    this.penaltiesAwayInput.set((event.target as HTMLInputElement).value);
  }

  protected setHomeScore(event: Event): void {
    this.scoreDraftTouched = true;
    this.homeScoreInput.set((event.target as HTMLInputElement).value);
  }

  protected setAwayScore(event: Event): void {
    this.scoreDraftTouched = true;
    this.awayScoreInput.set((event.target as HTMLInputElement).value);
  }

  protected setSelectedHomePlayer(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedHomePlayerId.set(value ? Number(value) : null);
  }

  protected setSelectedAwayPlayer(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedAwayPlayerId.set(value ? Number(value) : null);
  }

  protected setEditGameDatetime(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.editGameDatetime.set(value ? value : null);
  }

  protected updateDatetime(): void {
    if (this.isEditingEvent()) return;
    const datetime = this.editGameDatetime();
    
    this.isEditingEvent.set(true);
    this.eventsApi.updateDatetime(this.eventId, { gameDatetime: datetime }).subscribe({
      next: (event) => {
        this.event.set(event);
        this.isEditingEvent.set(false);
        this.toastService.success('Data da partida atualizada com sucesso.');
      },
      error: () => {
        this.isEditingEvent.set(false);
        this.toastService.error('Nao foi possivel atualizar a data da partida.');
      }
    });
  }

  protected cancelEvent(): void {
    if (this.isEditingEvent()) return;
    if (!confirm('Deseja realmente cancelar esta partida?')) return;

    this.isEditingEvent.set(true);
    this.eventsApi.cancel(this.eventId).subscribe({
      next: (event) => {
        this.event.set(event);
        this.isEditingEvent.set(false);
        this.toastService.success('Partida cancelada com sucesso.');
      },
      error: () => {
        this.isEditingEvent.set(false);
        this.toastService.error('Nao foi possivel cancelar a partida.');
      }
    });
  }

  protected askDeleteEvent(): void {
    this.confirmingDeleteEvent.set(true);
  }

  protected cancelDeleteEvent(): void {
    this.confirmingDeleteEvent.set(false);
  }

  protected deleteEvent(): void {
    if (this.isEditingEvent()) return;

    this.confirmingDeleteEvent.set(false);
    this.isEditingEvent.set(true);
    this.eventsApi.delete(this.eventId).subscribe({
      next: () => {
        this.toastService.success('Partida excluida com sucesso.');
        // Navigate back to tournament
        history.back();
      },
      error: () => {
        this.isEditingEvent.set(false);
        this.toastService.error('Nao foi possivel excluir a partida.');
      }
    });
  }

  protected statusLabel(status: string): string {
    const labels: Record<string, string> = {
      CREATED: 'Criada',
      IN_PROGRESS: 'Ao vivo',
      PENALTIES: 'Penaltis',
      COMPLETED: 'Finalizada',
      CANCELLED: 'Cancelada',
    };

    return labels[status] ?? status;
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

  protected scoreValue(value: number | null): number {
    return value ?? 0;
  }

  protected playerName(player: TournamentPlayerResponseDto | null, fallbackId: number | null | undefined): string {
    return player?.playerName ?? (fallbackId ? `Player #${fallbackId}` : 'Jogador');
  }

  protected playerTeam(player: TournamentPlayerResponseDto | null): string {
    return this.translatedTeamName(player?.teamName, 'Sem time');
  }

  protected translatedTeamName(value: string | null | undefined, fallback = ''): string {
    if (!value) return fallback;
    return TEAM_TRANSLATIONS[value] ?? value;
  }

  protected teamBadgeUrl(player: TournamentPlayerResponseDto | null): string | null {
    if (!player?.teamId) {
      return null;
    }

    return this.teams().find((team) => team.id === player.teamId)?.badgeUrl ?? null;
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

  protected winnerLabel(event: EventResponseDto): string {
    const homeScore = this.scoreValue(event.homeScore);
    const awayScore = this.scoreValue(event.awayScore);

    // If penalties were recorded, use them to determine winner
    if (event.penaltiesHome !== null && event.penaltiesAway !== null) {
      if (event.penaltiesHome === event.penaltiesAway) {
        return 'Empate (penaltis)';
      }
      return event.penaltiesHome > event.penaltiesAway
        ? this.displayHomeName()
        : this.displayAwayName();
    }

    if (homeScore === awayScore) {
      return 'Empate';
    }

    return homeScore > awayScore
      ? this.displayHomeName()
      : this.displayAwayName();
  }

  // ─── Market & Betting ────────────────────────────────────────────────

  protected outcomeLabel(name: string): string {
    const event = this.event();
    if (this.isRealFootball() && event) {
      const labels: Record<string, string> = {
        'Vitória Casa': this.translatedTeamName(event.teamHomeName, 'Casa'),
        'Empate': 'Empate',
        'Vitória Fora': this.translatedTeamName(event.teamAwayName, 'Fora'),
      };
      return labels[name] ?? this.translatedTeamName(name);
    }
    const labels: Record<string, string> = {
      'Vitória Casa': this.playerName(this.homePlayer(), event?.playerHomeId),
      'Empate': 'Empate',
      'Vitória Fora': this.playerName(this.awayPlayer(), event?.playerAwayId),
    };
    return labels[name] ?? name;
  }

  protected marketStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      OPEN: 'Aberto',
      CLOSED: 'Fechado',
      SUSPENDED: 'Suspenso',
      CANCELLED: 'Cancelado',
    };
    return labels[status] ?? status;
  }

  protected isOutcomeSelected(eventId: number, marketType: string, outcomeId: number): boolean {
    return this.betCart.entries().some(
      (e) => e.eventId === eventId && e.marketType === marketType && e.outcomeId === outcomeId,
    );
  }

  protected isMarketOpen(marketId: number): boolean {
    return this.marketAccordionState()[marketId] ?? true;
  }

  protected setMarketOpen(marketId: number, open: boolean): void {
    this.marketAccordionState.update((current) => ({ ...current, [marketId]: open }));
  }

  protected toggleMarketAccordion(marketId: number): void {
    this.setMarketOpen(marketId, !this.isMarketOpen(marketId));
  }

  protected isExactScoreExpanded(marketId: number): boolean {
    return this.exactScoreExpanded()[marketId] ?? false;
  }

  protected toggleExactScoreExpanded(marketId: number): void {
    this.exactScoreExpanded.update((current) => ({
      ...current,
      [marketId]: !this.isExactScoreExpanded(marketId),
    }));
  }

  protected visibleMarketOutcomes(market: MarketResponseDto): OutcomeResponseDto[] {
    if (market.marketType !== 'EXACT_SCORE') {
      return market.outcomes;
    }

    return this.isExactScoreExpanded(market.id)
      ? market.outcomes
      : market.outcomes.slice(0, 6);
  }

  protected marketOutcomeGridClass(market: MarketResponseDto): string {
    return this.outcomeGridClass(this.visibleMarketOutcomes(market).length);
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
      : (event.playerHomeName ?? this.playerName(this.homePlayer(), event.playerHomeId));
    const awayLabel = this.isRealFootball()
      ? this.translatedTeamName(event.teamAwayName, 'Fora')
      : (event.playerAwayName ?? this.playerName(this.awayPlayer(), event.playerAwayId));

    const entry: BetCartEntry = {
      eventId: event.id,
      marketId: market.id,
      outcomeId: outcome.id,
      outcomeName: this.outcomeLabel(outcome.name),
      odd: outcome.odd,
      eventLabel: `${homeLabel} vs ${awayLabel}`,
      tournamentId: tournament.id,
      tournamentName: tournament.name,
      marketType: market.marketType,
    };

    this.betCart.addEntry(entry);
  }

  protected toggleMarketStatus(): void {
    const m = this.activeMarket();
    if (!m || this.isSubmitting()) return;

    const newStatus = m.status === 'OPEN' ? 'CLOSED' : 'OPEN';

    this.isSubmitting.set(true);
    this.actionErrorMessage.set('');

    this.marketsApi.updateStatus(this.eventId, { status: newStatus }).subscribe({
      next: (updated) => {
        this.markets.set(updated);
        this.isSubmitting.set(false);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.actionErrorMessage.set('Nao foi possivel alterar o status do mercado.');
      },
    });
  }

  protected setDepositUser(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.depositUserId.set(value ? Number(value) : null);
  }

  protected deposit(): void {
    const amount = Number(this.depositAmount());
    const target = this.depositUserId();
    const tournamentId = this.currentTournamentId();
    if (!target || !amount || amount <= 0) return;
    if (!tournamentId) return;

    this.isDepositing.set(true);
    this.depositMessage.set('');

    this.walletApi.deposit({ userId: target, tournamentId, amount }).subscribe({
      next: () => {
        this.depositMessage.set('Deposito realizado com sucesso!');
        this.depositAmount.set('');
        this.isDepositing.set(false);
        this.refreshWallet(tournamentId);
      },
      error: () => {
        this.isDepositing.set(false);
        this.depositMessage.set('Erro ao realizar deposito.');
      },
    });
  }

  protected depositAll(): void {
    const amount = Number(this.depositAllAmount());
    const tournamentId = this.currentTournamentId();
    if (!amount || amount <= 0) return;
    if (!tournamentId) return;

    this.isDepositing.set(true);
    this.depositAllMessage.set('');

    this.walletApi.depositAll(tournamentId, amount).subscribe({
      next: () => {
        this.depositAllMessage.set('Deposito realizado para todos!');
        this.depositAllAmount.set('');
        this.isDepositing.set(false);
        this.refreshWallet(tournamentId);
      },
      error: () => {
        this.isDepositing.set(false);
        this.depositAllMessage.set('Erro ao realizar deposito para todos.');
      },
    });
  }

  private refreshWallet(tournamentId: number): void {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;
    this.walletApi.getBalance(userId, tournamentId).pipe(
      catchError(() => of({ userId, tournamentId, balance: 0, initialBalance: 0 } as WalletResponseDto)),
    ).subscribe({
      next: (wallet) => {
        this.walletInitialBalance.set(wallet.initialBalance ?? wallet.balance);
        this.walletBalance.set(wallet.balance);
        this.state.walletBalance.set(wallet.balance);
      },
    });
  }

  private loadEventContext(event: EventResponseDto): void {
    forkJoin({
      tournaments: this.tournamentsApi.findAll().pipe(map((p) => p.content)),
      teams: this.teamsApi.findAll(),
      players: this.tournamentsApi.findPlayers(event.tournamentId),
      rounds: this.tournamentsApi.findRounds(event.tournamentId),
      markets: this.marketsApi.findByEventId(event.id).pipe(
        catchError(() => of([])),
      ),
    }).subscribe({
      next: ({ tournaments, teams, players, rounds, markets }) => {
        this.event.set(event);
        this.selectedHomePlayerId.set(event.playerHomeId);
        this.selectedAwayPlayerId.set(event.playerAwayId);
        this.tournament.set(tournaments.find((item) => item.id === event.tournamentId) ?? null);
        this.state.activeTournamentId.set(event.tournamentId);
        this.teams.set(teams);
        this.players.set(players);
        this.rounds.set([...rounds].sort((a, b) => a.roundOrder - b.roundOrder));
        this.markets.set(markets);

        if (!this.scoreDraftTouched) {
          this.seedScoreInputs(event);
        }

        this.status.set('success');
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel carregar os detalhes da partida.');
      },
    });

    this.loadUserData(event);
    this.loadEventBets();
  }

  private loadUserData(event: EventResponseDto): void {
    const userId = this.auth.currentUser()?.id;
    const tournamentId = event.tournamentId;
    if (!userId) return;

    this.refreshWallet(tournamentId);

    this.betsApi.getMyBets().subscribe({
      next: (bets) => {
        const match = bets.find((bet) =>
          bet.items.some((item) => item.eventId === this.eventId),
        );
        this.userBet.set(match ?? null);
      },
    });
  }

  private currentTournamentId(): number | null {
    return this.event()?.tournamentId ?? this.tournament()?.id ?? null;
  }

  private loadEventBets(): void {
    this.eventBetsLoading.set(true);
    this.eventBetsError.set('');

    this.betsApi.getBetsByEvent(this.eventId).subscribe({
      next: (bets) => {
        this.eventBets.set(bets);
        this.eventBetsLoading.set(false);
      },
      error: () => {
        this.eventBetsLoading.set(false);
        this.eventBetsError.set('Nao foi possivel carregar o historico de apostas.');
      },
    });
  }

  protected userName(userId: number): string {
    return this.state.loginUsers().find((user) => user.id === userId)?.name ?? `Usuario #${userId}`;
  }

  protected displayHomeName(): string {
    const event = this.event();
    if (!event) return '';
    if (this.isRealFootball()) return this.translatedTeamName(event.teamHomeName, 'Casa');
    return this.playerName(this.homePlayer(), event.playerHomeId);
  }

  protected homeSubtitle(): string {
    const event = this.event();
    if (!event) return '';
    return this.isRealFootball()
      ? 'Mandante'
      : `${this.playerTeam(this.homePlayer())} · Mandante`;
  }

  protected displayAwayName(): string {
    const event = this.event();
    if (!event) return '';
    if (this.isRealFootball()) return this.translatedTeamName(event.teamAwayName, 'Fora');
    return this.playerName(this.awayPlayer(), event.playerAwayId);
  }

  protected awaySubtitle(): string {
    const event = this.event();
    if (!event) return '';
    return this.isRealFootball()
      ? 'Visitante'
      : `${this.playerTeam(this.awayPlayer())} · Visitante`;
  }

  protected homeTeamBadgeUrl(): string | null {
    const event = this.event();
    if (!event?.teamHomeId) return null;
    return this.teams().find((t) => t.id === event.teamHomeId)?.badgeUrl ?? null;
  }

  protected awayTeamBadgeUrl(): string | null {
    const event = this.event();
    if (!event?.teamAwayId) return null;
    return this.teams().find((t) => t.id === event.teamAwayId)?.badgeUrl ?? null;
  }

  protected homeTeamAbbreviation(): string {
    const event = this.event();
    if (!event?.teamHomeId) return '--';
    return this.teams().find((t) => t.id === event.teamHomeId)?.abbreviation ?? '--';
  }

  protected awayTeamAbbreviation(): string {
    const event = this.event();
    if (!event?.teamAwayId) return '--';
    return this.teams().find((t) => t.id === event.teamAwayId)?.abbreviation ?? '--';
  }

  protected homeInitials(): string {
    if (this.isRealFootball()) return this.homeTeamAbbreviation();
    return this.playerInitials(this.homePlayer());
  }

  protected awayInitials(): string {
    if (this.isRealFootball()) return this.awayTeamAbbreviation();
    return this.playerInitials(this.awayPlayer());
  }

  protected marketTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      MATCH_RESULT: 'Resultado (1X2)',
      OVER_UNDER_25: 'Over/Under 2.5',
      OVER_UNDER_35: 'Over/Under 3.5',
      BTTS: 'Ambas Marcam',
      EXACT_SCORE: 'Placar Exato',
      QUALIFY: 'Quem Avança',
    };
    return labels[type] ?? type;
  }

  protected marketGroupLabel(group: MarketGroup): string {
    const labels: Record<MarketGroup, string> = {
      ALL: 'Todos',
      MAIN: 'Principal',
      GOALS: 'Gols',
      OTHER: 'Outros',
    };

    return labels[group];
  }

  protected marketGroupForType(type: string): Exclude<MarketGroup, 'ALL'> {
    if (type === 'MATCH_RESULT' || type === 'QUALIFY') {
      return 'MAIN';
    }

    if (type === 'OVER_UNDER_25' || type === 'OVER_UNDER_35' || type === 'BTTS') {
      return 'GOALS';
    }

    return 'OTHER';
  }

  protected outcomeGridClass(outcomesCount: number): string {
    if (outcomesCount <= 2) return 'grid-cols-2';
    if (outcomesCount === 3) return 'grid-cols-3';
    return 'grid-cols-2 sm:grid-cols-3 md:grid-cols-4';
  }

  protected setSelectedMarketGroup(group: MarketGroup): void {
    this.selectedMarketGroup.set(group);
  }

  private refreshMarket(event: EventResponseDto): void {
    this.marketsApi.findByEventId(event.id).pipe(
      catchError(() => of([])),
    ).subscribe({
      next: (markets) => this.markets.set(markets),
    });
  }

  private checkBetResolution(): void {
    this.betsApi.getMyBets().subscribe({
      next: (bets) => {
        const bet = bets.find((b) =>
          b.items.some((item) => item.eventId === this.eventId),
        );
        if (!bet) return;

        const previousStatus = this.userBet()?.status;

        this.userBet.set(bet);

        if (previousStatus === 'PENDING' || !previousStatus) {
          if (bet.status === 'WON') {
            this.toastService.success(
              `Voce ganhou R$ ${bet.potentialReturn.toFixed(2)}!`,
            );
            const tournamentId = this.currentTournamentId();
            if (tournamentId) {
              this.refreshWallet(tournamentId);
            }
          } else if (bet.status === 'LOST') {
            this.toastService.info('Sua aposta nao foi dessa vez.');
          }
        }
      },
    });
  }

  private seedScoreInputs(event: EventResponseDto): void {
    this.homeScoreInput.set(String(this.scoreValue(event.homeScore)));
    this.awayScoreInput.set(String(this.scoreValue(event.awayScore)));
  }

  private seedDatetimeInput(event: EventResponseDto): void {
    if (event.gameDatetime) {
      // Format to yyyy-MM-ddThh:mm (datetime-local format)
      const d = new Date(event.gameDatetime);
      const pad = (n: number) => n.toString().padStart(2, '0');
      this.editGameDatetime.set(
        `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
      );
    } else {
      this.editGameDatetime.set(null);
    }
  }

  private findPlayer(playerId: number | null): TournamentPlayerResponseDto | null {
    if (!playerId) return null;
    return this.players().find((player) => this.tournamentPlayerPlayerId(player) === playerId) ?? null;
  }

  private roundId(round: TournamentRoundResponseDto): number {
    return round.roundId ?? round.id ?? (round as unknown as { round_id?: number }).round_id ?? 0;
  }

  private tournamentPlayerPlayerId(player: TournamentPlayerResponseDto): number {
    return player.playerId ?? (player as unknown as { player_id?: number }).player_id ?? 0;
  }
}
