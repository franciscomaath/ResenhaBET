import { Component, Input } from '@angular/core';
import { GroupRole } from '../../services/api/api.models';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-role-chip',
  imports: [CommonModule],
  template: `
    <span
      class="rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-[0.2em]"
      [ngClass]="{
        'bg-cyan-950 text-cyan-300': role === GroupRole.OWNER || role === GroupRole.ADMIN,
        'bg-gray-800 text-gray-300': role === GroupRole.MEMBER
      }"
    >
      {{ label }}
    </span>
  `,
})
export class AppRoleChipComponent {
  @Input({ required: true }) role!: GroupRole;

  get GroupRole() {
    return GroupRole;
  }

  get label(): string {
    switch (this.role) {
      case GroupRole.OWNER:
        return 'Dono';
      case GroupRole.ADMIN:
        return 'Admin';
      case GroupRole.MEMBER:
        return 'Membro';
      default:
        return 'Sem papel';
    }
  }
}
