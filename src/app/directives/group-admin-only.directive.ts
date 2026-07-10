import { Directive, TemplateRef, ViewContainerRef, effect, inject } from '@angular/core';

import { AuthService } from '../services/auth.service';

@Directive({
  selector: '[appGroupAdminOnly]',
})
export class GroupAdminOnlyDirective {
  private readonly templateRef = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly auth = inject(AuthService);
  private hasView = false;

  constructor() {
    effect(() => {
      if (this.auth.canManageGroup()) {
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
