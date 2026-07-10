import { Directive, TemplateRef, ViewContainerRef, effect, inject } from '@angular/core';

import { AdminVisibility } from '../services/admin-visibility';

@Directive({
  selector: '[appAdminOnly]',
})
export class AdminOnlyDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly adminVisibility = inject(AdminVisibility);
  private hasView = false;

  constructor() {
    effect(() => {
      if (this.adminVisibility.canManage()) {
        this.show();
        return;
      }

      this.hide();
    });
  }

  private show(): void {
    if (this.hasView) {
      return;
    }

    this.viewContainer.createEmbeddedView(this.templateRef);
    this.hasView = true;
  }

  private hide(): void {
    if (!this.hasView) {
      return;
    }

    this.viewContainer.clear();
    this.hasView = false;
  }
}
