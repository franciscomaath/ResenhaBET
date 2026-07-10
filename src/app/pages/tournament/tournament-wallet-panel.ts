import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-tournament-wallet-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="flex items-center gap-3 rounded-[var(--radius-lg)] border border-[var(--border-strong)] bg-[var(--color-gray-800)] px-4 py-3 shadow-brand-card">
      <div class="flex h-[42px] w-[42px] shrink-0 items-center justify-center rounded-[var(--radius-md)] bg-green-500/10">
        <span class="material-icons text-[22px] text-[var(--money-positive)]">account_balance_wallet</span>
      </div>

      <div class="min-w-0 flex-1">
        <p class="text-[10.5px] font-extrabold uppercase tracking-[0.12em] text-[var(--text-muted)]">Sua carteira</p>
        <p class="font-display mt-0.5 truncate text-2xl font-black leading-tight tabular-nums text-[var(--money)]">
          {{ formattedBalance }}
        </p>
      </div>

      <div class="shrink-0 text-right">
        <p class="text-[10.5px] font-bold text-[var(--text-muted)]">Saldo no torneio</p>
        <p class="mt-0.5 max-w-[120px] truncate text-[11px] font-bold text-[var(--text-secondary)]">
          {{ displayTournamentName }}
        </p>
      </div>
    </article>
  `,
})
export class TournamentWalletPanelComponent {
  @Input() balance = 0;
  @Input() tournamentName: string | null | undefined;

  get formattedBalance(): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(this.balance);
  }

  get displayTournamentName(): string {
    return this.tournamentName?.trim() || 'Torneio atual';
  }
}
