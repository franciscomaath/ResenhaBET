import { Injectable, signal } from '@angular/core';

export type ToastVariant = 'error' | 'success' | 'info';

export interface AppToast {
  id: number;
  message: string;
  variant: ToastVariant;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  private lastMessage = '';
  private lastMessageAt = 0;

  readonly toasts = signal<AppToast[]>([]);

  error(message: string): void {
    this.show(message, 'error');
  }

  success(message: string): void {
    this.show(message, 'success');
  }

  info(message: string): void {
    this.show(message, 'info');
  }

  dismiss(id: number): void {
    this.toasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  private show(message: string, variant: ToastVariant): void {
    const normalizedMessage = message.trim();
    if (!normalizedMessage) {
      return;
    }

    const now = Date.now();
    if (normalizedMessage === this.lastMessage && now - this.lastMessageAt < 4000) {
      return;
    }

    this.lastMessage = normalizedMessage;
    this.lastMessageAt = now;

    const toast: AppToast = {
      id: this.nextId++,
      message: normalizedMessage,
      variant,
    };

    this.toasts.update((toasts) => [...toasts.slice(-3), toast]);
    window.setTimeout(() => this.dismiss(toast.id), 5000);
  }
}
