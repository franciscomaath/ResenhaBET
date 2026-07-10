import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { AppToast, ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast-host',
  imports: [CommonModule],
  templateUrl: './toast-host.html',
})
export class ToastHost {
  protected readonly toastService = inject(ToastService);

  protected dismiss(id: number): void {
    this.toastService.dismiss(id);
  }

  protected toastClasses(toast: AppToast): string {
    const base = 'pointer-events-auto flex min-h-11 w-full items-start justify-between gap-3 rounded-md border px-4 py-3 text-sm font-semibold shadow-xl';
    const variants: Record<AppToast['variant'], string> = {
      error: 'border-red-500/50 bg-red-950 text-red-100',
      success: 'border-green-500/50 bg-green-950 text-green-100',
      info: 'border-cyan-500/50 bg-cyan-950 text-cyan-100',
    };

    return `${base} ${variants[toast.variant]}`;
  }
}
