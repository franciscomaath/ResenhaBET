import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';

import { BetCartService } from '../../services/bet-cart.service';
import { FabService } from '../../services/fab.service';
import { ResenhaBetState } from '../../services/resenhabet-state';

@Component({
  selector: 'app-bet-slip',
  imports: [CommonModule],
  templateUrl: './bet-slip.html',
  styleUrl: './bet-slip.css',
})
export class BetSlipComponent {
  protected readonly cart = inject(BetCartService);
  protected readonly fab = inject(FabService);
  protected readonly state = inject(ResenhaBetState);

  protected readonly hasFab = computed(() => this.fab.config().visible && (!this.fab.config().adminOnly || this.state.canManageGroup()));
  protected readonly isEmpty = computed(() => this.cart.itemCount() === 0);

  protected readonly hasWarnings = computed(() =>
    this.cart.pendingWarning() || this.cart.crossTournamentWarning(),
  );

  protected setStake(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.cart.setStake(value);
  }

  protected setMaxStake(): void {
    this.cart.setMaxStake();
  }

  protected removeEntry(eventId: number): void {
    this.cart.removeEntry(eventId);
  }

  protected clearCart(): void {
    this.cart.clearCart();
  }

  protected toggleExpanded(): void {
    this.cart.toggleExpanded();
  }

  protected placeBet(): void {
    this.cart.placeBet();
  }
}
