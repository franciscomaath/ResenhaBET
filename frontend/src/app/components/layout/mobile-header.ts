import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ActiveGroupSwitcherComponent } from '../active-group-switcher/active-group-switcher';

@Component({
  selector: 'app-mobile-header',
  standalone: true,
  imports: [CommonModule, RouterLink, ActiveGroupSwitcherComponent],
  templateUrl: './mobile-header.html',
})
export class MobileHeaderComponent {
  @Output() logout = new EventEmitter<void>();
}
