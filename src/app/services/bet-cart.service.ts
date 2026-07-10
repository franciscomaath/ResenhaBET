import { Injectable, signal, computed } from '@angular/core';
import { inject } from '@angular/core';
import { catchError, of } from 'rxjs';

import { CreateBetSlipRequestDto } from './api/api.models';
import { BetsApi } from './api/bets-api';
import { WalletApi } from './api/wallet-api';
import { AuthService } from './auth.service';
import { ResenhaBetState } from './resenhabet-state';
import { ToastService } from './toast.service';

export interface BetCartEntry {
  eventId: number;
  marketId: number;
  outcomeId: number;
  outcomeName: string;
  odd: number;
  eventLabel: string;
  tournamentId: number;
  tournamentName: string;
  marketType?: string;
}

@Injectable({ providedIn: 'root' })
export class BetCartService {
  private readonly betsApi = inject(BetsApi);
  private readonly walletApi = inject(WalletApi);
  private readonly auth = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly state = inject(ResenhaBetState);

  readonly entries = signal<BetCartEntry[]>([]);
  readonly stakeAmount = signal<number>(0);
  readonly isExpanded = signal<boolean>(false);
  readonly isPlacingBet = signal<boolean>(false);
  readonly betSlipError = signal<string>('');
  readonly pendingWarning = signal<string>('');
  readonly crossTournamentWarning = signal<string>('');
  readonly walletBalance = signal<number>(0);

  readonly combinedOdd = computed(() => {
    const items = this.entries();
    if (items.length === 0) return 1;
    return items.reduce((acc, item) => acc * item.odd, 1);
  });

  readonly potentialReturn = computed(() => {
    const stake = this.stakeAmount();
    return stake * this.combinedOdd();
  });

  readonly itemCount = computed(() => this.entries().length);

  readonly tournamentId = computed(() => {
    const items = this.entries();
    return items.length > 0 ? items[0].tournamentId : null;
  });

  readonly canPlaceBet = computed(() => {
    return (
      this.entries().length > 0 &&
      this.stakeAmount() > 0 &&
      !this.isPlacingBet()
    );
  });

  readonly isCartVisible = computed(() => this.entries().length > 0);

  addEntry(entry: BetCartEntry): void {
    const currentEntries = this.entries();
    const existingDifferentTournament = currentEntries.length > 0 && currentEntries[0].tournamentId !== entry.tournamentId;

    if (existingDifferentTournament) {
      this.entries.set([]);
      this.crossTournamentWarning.set('Carrinho limpo: so e possivel apostar em eventos do mesmo torneio.');
      this.toastService.info('Carrinho limpo: so e possivel apostar em eventos do mesmo torneio.');
    } else {
      this.crossTournamentWarning.set('');
    }

    const existingIndex = currentEntries.findIndex(
      (e) => e.eventId === entry.eventId && e.marketType === entry.marketType,
    );
    if (existingIndex >= 0) {
      if (currentEntries[existingIndex].outcomeId === entry.outcomeId) {
        this.entries.update((entries) =>
          entries.filter((e) => !(e.eventId === entry.eventId && e.marketType === entry.marketType)),
        );
        return;
      }
      this.entries.update((entries) => {
        const updated = [...entries];
        updated[existingIndex] = entry;
        return updated;
      });
    } else {
      this.entries.update((entries) => [...entries, entry]);
    }

    this.checkPendingWarning(entry.eventId);
    this.refreshWallet(this.tournamentId() ?? undefined);
  }

  removeEntry(eventId: number): void {
    this.entries.update((entries) => entries.filter((e) => e.eventId !== eventId));
    this.checkPendingWarningForAll();
    if (this.entries().length === 0) {
      this.stakeAmount.set(0);
      this.isExpanded.set(false);
    }
  }

  clearCart(): void {
    this.entries.set([]);
    this.stakeAmount.set(0);
    this.betSlipError.set('');
    this.pendingWarning.set('');
    this.crossTournamentWarning.set('');
    this.isExpanded.set(false);
  }

  setStake(amount: number): void {
    this.stakeAmount.set(Math.max(0, amount));
  }

  setMaxStake(): void {
    this.refreshWallet(this.tournamentId() ?? undefined);
    this.stakeAmount.set(Math.floor(this.walletBalance()));
  }

  toggleExpanded(): void {
    this.isExpanded.update((v) => !v);
  }

  placeBet(): void {
    const items = this.entries();
    const stake = this.stakeAmount();
    const tournamentId = this.tournamentId();

    if (items.length === 0 || !stake || stake <= 0 || !tournamentId) {
      this.betSlipError.set('Selecione pelo menos um resultado e informe o valor da aposta.');
      return;
    }

    this.isPlacingBet.set(true);
    this.betSlipError.set('');

    const request: CreateBetSlipRequestDto = {
      tournamentId,
      stake,
      items: items.map((item) => ({
        eventId: item.eventId,
        marketId: item.marketId,
        outcomeId: item.outcomeId,
      })),
    };

    this.betsApi.placeBet(request).subscribe({
      next: (betSlip) => {
        this.isPlacingBet.set(false);
        this.clearCart();
        this.toastService.success(
          `Aposta realizada com sucesso! Retorno potencial: R$ ${betSlip.potentialReturn.toFixed(2)}`,
        );
        this.refreshWallet(tournamentId ?? undefined);
        this.state.refreshWallet(undefined, tournamentId ?? undefined);
      },
      error: (err) => {
        this.isPlacingBet.set(false);
        const body = err.error as { message?: string } | undefined;
        this.betSlipError.set(body?.message ?? 'Erro ao realizar aposta.');
      },
    });
  }

  private checkPendingWarning(eventId: number): void {
    const userId = this.auth.currentUser()?.id;
    if (!userId) return;

    this.betsApi.getMyBets().subscribe({
      next: (bets) => {
        const hasPending = bets.some(
          (bet) =>
            bet.status === 'PENDING' &&
            bet.items.some((item) => item.eventId === eventId),
        );
        if (hasPending) {
          this.pendingWarning.set('Voce ja possui uma aposta pendente para este evento.');
        } else {
          this.pendingWarning.set('');
        }
      },
    });
  }

  private checkPendingWarningForAll(): void {
    const userId = this.auth.currentUser()?.id;
    if (!userId || this.entries().length === 0) {
      this.pendingWarning.set('');
      return;
    }

    const eventIds = this.entries().map((e) => e.eventId);
    this.betsApi.getMyBets().subscribe({
      next: (bets) => {
        const hasPending = bets.some(
          (bet) =>
            bet.status === 'PENDING' &&
            bet.items.some((item) => eventIds.includes(item.eventId)),
        );
        if (hasPending) {
          this.pendingWarning.set('Voce ja possui uma aposta pendente para um dos eventos selecionados.');
        } else {
          this.pendingWarning.set('');
        }
      },
    });
  }

  refreshWallet(tournamentId?: number): void {
    const userId = this.auth.currentUser()?.id;
    if (!userId || !tournamentId) return;
    this.walletApi.getBalance(userId, tournamentId).pipe(
      catchError(() => of({ userId, tournamentId, balance: 0 })),
    ).subscribe({
      next: (wallet) => this.walletBalance.set(wallet.balance),
    });
  }
}
