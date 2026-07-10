import { Injectable, signal } from '@angular/core';

export interface FabConfig {
  icon: string;
  label: string;
  action: () => void;
  visible: boolean;
  adminOnly?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class FabService {
  readonly config = signal<FabConfig>({
    icon: '',
    label: '',
    action: () => {},
    visible: false,
    adminOnly: false,
  });

  setConfig(config: Partial<FabConfig>) {
    this.config.set({
      icon: config.icon ?? '',
      label: config.label ?? '',
      action: config.action ?? (() => {}),
      visible: config.visible ?? false,
      adminOnly: config.adminOnly ?? false,
    });
  }

  clearConfig() {
    this.config.set({
      icon: '',
      label: '',
      action: () => {},
      visible: false,
      adminOnly: false,
    });
  }
}
