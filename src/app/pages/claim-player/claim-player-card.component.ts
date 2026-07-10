import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { AvailablePlayerResponseDto } from '../../services/api/api.models';

@Component({
  selector: 'app-claim-player-card',
  imports: [CommonModule],
  templateUrl: './claim-player-card.component.html',
})
export class ClaimPlayerCardComponent {
  readonly player = input.required<AvailablePlayerResponseDto>();
  readonly selected = input.required<boolean>();
  readonly toggle = output<number>();

  protected selectPlayer(): void {
    this.toggle.emit(this.player().id);
  }
}
