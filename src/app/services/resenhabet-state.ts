import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { catchError, Observable, of, tap } from 'rxjs';

import {
  Bet,
  BetTarget,
  Bettor,
  MainTab,
  Match,
  MatchHistory,
  Player,
  ResolvedBet,
  ScorePayload,
} from '../models/resenhabet.models';
import {
  GroupResponseDto,
  LoginResponseDto,
  UserResponseDto,
  WalletResponseDto,
} from './api/api.models';
import { GroupsApi } from './api/groups-api';
import { UsersApi } from './api/users-api';
import { WalletApi } from './api/wallet-api';
import { AuthService } from './auth.service';

type LoginPlayersStatus = 'idle' | 'loading' | 'success' | 'error';
type LoginStatus = 'idle' | 'loading' | 'error';

@Injectable({
  providedIn: 'root',
})
export class ResenhaBetState {
  private readonly usersApi = inject(UsersApi);
  private readonly groupsApi = inject(GroupsApi);
  private readonly walletApi = inject(WalletApi);
  private readonly auth = inject(AuthService);

  readonly players = signal<Player[]>([
    { name: 'Francisco', aura: 1275 },
    { name: 'Tadeu', aura: 1210 },
    { name: 'Caio', aura: 1085 },
    { name: 'Lucas', aura: 1030 },
  ]);

  readonly bettors = signal<Bettor[]>([
    { name: 'Francisco', wallet: 50 },
    { name: 'Tadeu', wallet: 50 },
    { name: 'Caio', wallet: 50 },
    { name: 'Lucas', wallet: 50 },
  ]);

  readonly currentUser = signal('');
  readonly selectedUserId = signal<number | null>(this.readStoredSelectedUserId());
  readonly walletBalance = signal<number>(0);

  readonly loginUsers = signal<UserResponseDto[]>([]);
  readonly loginPlayersStatus = signal<LoginPlayersStatus>('idle');
  readonly loginPlayersError = signal('');
  readonly loginStatus = signal<LoginStatus>('idle');
  readonly loginError = signal('');

  readonly groups = signal<GroupResponseDto[]>([]);
  readonly groupSwitchVersion = signal(0);
  readonly activeTab = signal<MainTab>('betting');
  readonly betAmount = signal(5);
  readonly currentBets = signal<Bet[]>([]);
  readonly history = signal<MatchHistory[]>([]);
  readonly activeTournamentId = signal<number | null>(null);

  readonly currentMatch = signal<Match | null>(
    this.createMatch(this.players()[0], this.players()[1], false),
  );

  readonly currentBettor = computed(() =>
    this.bettors().find((bettor) => bettor.name === this.currentUser()),
  );

  readonly sortedBettors = computed(() =>
    [...this.bettors()].sort((a, b) => b.wallet - a.wallet),
  );

  readonly myCurrentBet = computed(() =>
    this.currentBets().find((bet) => bet.bettor === this.currentUser()),
  );

  readonly activeLoginPlayers = computed(() =>
    this.loginUsers().sort((a, b) => a.name.localeCompare(b.name)),
  );

  readonly activeGroup = computed(() => {
    const groupId = this.auth.currentGroupId();
    if (!groupId) {
      return null;
    }

    const playerClaimed = this.auth.currentGroupPlayerClaimed();

    const group = this.groups().find((candidate) => candidate.id === groupId);
    if (group) {
      return {
        ...group,
        playerClaimed,
      };
    }

    return {
      id: groupId,
      name: this.auth.currentGroupName() ?? '',
      role: this.auth.currentGroupRole(),
      playerClaimed,
    };
  });

  readonly isAdmin = this.auth.isAdmin;
  readonly isSystemAdmin = this.auth.isAdmin;
  readonly canManageGroup = this.auth.canManageGroup;
  readonly isGroupOwner = this.auth.isGroupOwner;
  readonly isGroupAdmin = this.auth.isGroupAdmin;

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      this.currentUser.set(user?.name ?? '');
      this.selectedUserId.set(user?.id ?? null);
    });
    effect(() => {
      const userId = this.selectedUserId();
      if (userId) {
        this.refreshWallet(userId);
      }
    });
    effect(() => {
      if (!this.auth.isLoggedIn()) {
        this.groups.set([]);
        return;
      }

      this.loadGroups();
    });
    this.auth.restoreSession();
  }

  loadLoginPlayers(): void {
    this.loginPlayersStatus.set('loading');
    this.loginPlayersError.set('');

    this.usersApi.getAll().subscribe({
      next: (users) => {
        this.loginUsers.set(users);
        this.loginPlayersStatus.set('success');
      },
      error: () => {
        this.loginPlayersStatus.set('error');
        this.loginPlayersError.set('Nao foi possivel carregar os usuarios. Verifique se a API esta rodando.');
      },
    });
  }

  loadGroups(): void {
    this.groupsApi.listMine().subscribe({
      next: (groups) => {
        this.groups.set(groups);

        const currentGroupId = this.auth.currentGroupId();
        if (!currentGroupId) {
          return;
        }

        const currentGroup = groups.find((group) => group.id === currentGroupId);
        if (currentGroup) {
          this.auth.setCurrentGroup(currentGroup);
          return;
        }

        this.auth.setCurrentGroup(null);
      },
      error: () => {
        this.groups.set([]);
      },
    });
  }

  logout(): void {
    this.auth.logout().subscribe();
    this.currentBets.set([]);
  }

  clearGroupScopedState(): void {
    this.walletBalance.set(0);
    this.currentBets.set([]);
    this.history.set([]);
  }

  notifyGroupSwitch(): void {
    this.groupSwitchVersion.update((version) => version + 1);
    this.clearGroupScopedState();
  }

  refreshWallet(userId?: number, tournamentId?: number): void {
    const id = userId ?? this.auth.currentUser()?.id;
    if (!id || !tournamentId) return;

    this.walletApi.getBalance(id, tournamentId).pipe(
      catchError(() => of({ userId: id, tournamentId, balance: 0 } as WalletResponseDto)),
    ).subscribe({
      next: (w) => this.walletBalance.set(w.balance),
    });
  }

  setTab(tab: MainTab): void {
    this.activeTab.set(tab);
  }

  setBetAmount(amount: number): void {
    this.betAmount.set(amount);
  }

  setMaxBet(): void {
    this.betAmount.set(Math.floor(this.currentBettor()?.wallet ?? 0));
  }

  placeBet(target: BetTarget): void {
    const match = this.currentMatch();
    const bettor = this.currentBettor();
    const amount = this.betAmount();

    if (!match || !bettor || !match.bettingOpen || amount <= 0 || bettor.wallet < amount) {
      return;
    }

    const label = this.getTargetLabel(target, match);
    const existingBet = this.currentBets().find((bet) => bet.bettor === bettor.name);
    const refund = existingBet?.amount ?? 0;

    this.currentBets.update((bets) => [
      ...bets.filter((bet) => bet.bettor !== bettor.name),
      { bettor: bettor.name, target, label, amount },
    ]);

    this.bettors.update((bettors) =>
      bettors.map((item) =>
        item.name === bettor.name ? { ...item, wallet: item.wallet + refund - amount } : item,
      ),
    );
  }

  toggleBetting(open: boolean): void {
    this.currentMatch.update((match) => (match ? { ...match, bettingOpen: open } : match));
  }

  updateScore(score: ScorePayload): void {
    this.currentMatch.update((match) =>
      match ? { ...match, scoreHome: score.home, scoreAway: score.away } : match,
    );
  }

  resolveMatch(target: BetTarget): void {
    const match = this.currentMatch();
    if (!match) {
      return;
    }

    const resolvedBets = this.currentBets().map((bet) => {
      const odd = this.getOddForTarget(bet.target, match);
      const isWinner = bet.target === target;
      return {
        ...bet,
        outcome: isWinner ? 'win' : 'loss',
        payout: isWinner ? bet.amount * odd : 0,
      } as ResolvedBet;
    });

    this.payWinningBets(resolvedBets);

    this.history.update((items) => [
      {
        matchLabel: `${match.home.name} x ${match.away.name}`,
        score: `${match.scoreHome} x ${match.scoreAway}`,
        winner: this.getTargetLabel(target, match),
        bets: resolvedBets,
      },
      ...items,
    ]);

    this.currentBets.set([]);
    this.currentMatch.set(null);
  }

  createDemoMatch(isKnockout: boolean): void {
    const [home, away] = this.players();
    this.currentBets.set([]);
    this.currentMatch.set(this.createMatch(home, away, isKnockout));
  }

  private payWinningBets(resolvedBets: ResolvedBet[]): void {
    this.bettors.update((bettors) =>
      bettors.map((bettor) => {
        const won = resolvedBets
          .filter((bet) => bet.bettor === bettor.name && bet.outcome === 'win')
          .reduce((sum, bet) => sum + bet.payout, 0);
        return won > 0 ? { ...bettor, wallet: bettor.wallet + won } : bettor;
      }),
    );
  }

  private createMatch(home: Player, away: Player, isKnockout: boolean): Match {
    return {
      home,
      away,
      scoreHome: 0,
      scoreAway: 0,
      phase: 'Nao Iniciado',
      elapsed: '00:00',
      isKnockout,
      bettingOpen: false,
      odds: this.calculateOdds(home.aura, away.aura, isKnockout),
    };
  }

  private calculateOdds(homeAura: number, awayAura: number, isKnockout: boolean): Match['odds'] {
    const homeProbability = 1 / (1 + Math.pow(10, (awayAura - homeAura) / 400));
    const awayProbability = 1 - homeProbability;

    if (isKnockout) {
      return {
        home: this.toOdd(homeProbability),
        draw: null,
        away: this.toOdd(awayProbability),
      };
    }

    const drawProbability = 0.28;
    const winProbability = 1 - drawProbability;

    return {
      home: this.toOdd(homeProbability * winProbability),
      draw: this.toOdd(drawProbability),
      away: this.toOdd(awayProbability * winProbability),
    };
  }

  private toOdd(probability: number): number {
    return Number(Math.max(1.05, 1 / probability).toFixed(2));
  }

  private getOddForTarget(target: BetTarget, match: Match): number {
    return target === 'home' ? match.odds.home : target === 'away' ? match.odds.away : match.odds.draw ?? 1;
  }

  private getTargetLabel(target: BetTarget, match: Match): string {
    if (target === 'home') {
      return match.home.name;
    }

    if (target === 'away') {
      return match.away.name;
    }

    return 'Empate';
  }

  private readStoredSelectedUserId(): number | null {
    const storedValue = this.readStorage('sessionUser');
    if (!storedValue) {
      return null;
    }

    let parsedUser: UserResponseDto;
    try {
      parsedUser = JSON.parse(storedValue) as UserResponseDto;
    } catch {
      return null;
    }

    const parsed = Number(parsedUser.id);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private readStorage(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

}
