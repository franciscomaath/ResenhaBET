import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';

import { MatchHistory } from '../../models/resenhabet.models';

@Component({
  selector: 'app-global-history',
  imports: [CommonModule],
  templateUrl: './global-history.html',
})
export class GlobalHistory {
  readonly history = input.required<MatchHistory[]>();
}
