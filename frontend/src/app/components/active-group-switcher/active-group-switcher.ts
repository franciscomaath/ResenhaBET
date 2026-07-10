import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ResenhaBetState } from '../../services/resenhabet-state';

@Component({
  selector: 'app-active-group-switcher',
  imports: [CommonModule, RouterLink],
  templateUrl: './active-group-switcher.html',
})
export class ActiveGroupSwitcherComponent {
  private readonly state = inject(ResenhaBetState);

  protected readonly activeGroup = computed(() => this.state.activeGroup());
}
