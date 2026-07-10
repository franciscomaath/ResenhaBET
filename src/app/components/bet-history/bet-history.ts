import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';

import { MatchHistory } from '../../models/resenhabet.models';

@Component({
  selector: 'app-bet-history',
  imports: [CommonModule],
  templateUrl: './bet-history.html',
})
export class BetHistory {
  readonly history = input.required<MatchHistory[]>();
  readonly currentUser = input.required<string>();
}
