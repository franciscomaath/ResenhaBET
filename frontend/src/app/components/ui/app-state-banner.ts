import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-state-banner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="rounded-[var(--radius-md)] border px-4 py-3 text-sm"
      [ngClass]="{
        'border-red-900 bg-red-950/60 text-red-200': type === 'error',
        'border-yellow-900 bg-yellow-950/60 text-yellow-200': type === 'warn',
        'border-cyan-900 bg-cyan-950/60 text-cyan-200': type === 'info'
      }"
    >
      {{ message }}
    </div>
  `,
})
export class AppStateBannerComponent {
  @Input() type: 'error' | 'warn' | 'info' = 'info';
  @Input({ required: true }) message!: string;
}
