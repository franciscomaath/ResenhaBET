import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal, OnDestroy } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  GroupMemberResponseDto,
  GroupRole,
  GroupResponseDto,
  GroupRequestDto,
  GroupSwitchResponseDto,
  UserResponseDto,
  PlayerStatsResponseDto,
} from '../../services/api/api.models';
import { GroupsApi } from '../../services/api/groups-api';
import { UsersApi } from '../../services/api/users-api';
import { ResenhaBetState } from '../../services/resenhabet-state';
import { AuthService } from '../../services/auth.service';
import { BetCartService } from '../../services/bet-cart.service';
import { ToastService } from '../../services/toast.service';
import { FabService } from '../../services/fab.service';
import { Router } from '@angular/router';

import { AppCardComponent } from '../../components/ui/app-card';
import { AppRoleChipComponent } from '../../components/ui/app-role-chip';
import { AppEmptyStateComponent } from '../../components/ui/app-empty-state';
import { AppSectionHeaderComponent } from '../../components/ui/app-section-header';
import { AppStateBannerComponent } from '../../components/ui/app-state-banner';
import { AdminOnlyDirective } from '../../directives/admin-only.directive';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';
type GroupsTab = 'ranking' | 'overview' | 'members' | 'settings';

@Component({
  selector: 'app-groups-page',
  imports: [CommonModule, AppCardComponent, AppRoleChipComponent, AppEmptyStateComponent, AppSectionHeaderComponent, AppStateBannerComponent, RouterLink, AdminOnlyDirective],
  templateUrl: './groups-page.html',
})
export class GroupsPage implements OnDestroy {
  private readonly groupsApi = inject(GroupsApi);
  private readonly usersApi = inject(UsersApi);
  private readonly auth = inject(AuthService);
  private readonly state = inject(ResenhaBetState);
  private readonly betCart = inject(BetCartService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly fab = inject(FabService);

  protected readonly activeTab = signal<GroupsTab>('ranking');
  
  protected readonly groups = computed(() => [...this.state.groups()].sort((a, b) => a.name.localeCompare(b.name)));
  protected readonly activeGroup = computed(() => this.state.activeGroup());
  protected readonly canManageGroup = this.auth.canManageGroup;
  protected readonly isGroupOwner = this.auth.isGroupOwner;
  protected readonly isSystemAdmin = this.auth.isSystemAdmin;
  protected readonly canViewSettings = computed(() => this.canManageGroup() || this.isSystemAdmin());
  protected readonly allowedMemberRoles = computed(() =>
    this.isGroupOwner()
      ? [GroupRole.OWNER, GroupRole.ADMIN, GroupRole.MEMBER]
      : [GroupRole.ADMIN, GroupRole.MEMBER],
  );
  protected readonly status = signal<LoadStatus>('idle');
  protected readonly errorMessage = signal('');
  protected readonly newGroupName = signal('');
  protected readonly switchingGroupId = signal<number | null>(null);
  protected readonly isCreating = signal(false);
  protected readonly members = signal<GroupMemberResponseDto[]>([]);
  protected readonly memberStatus = signal<LoadStatus>('idle');
  protected readonly memberError = signal('');
  protected readonly ranking = signal<PlayerStatsResponseDto[]>([]);
  protected readonly rankingStatus = signal<LoadStatus>('idle');
  protected readonly rankingError = signal('');
  protected readonly users = signal<UserResponseDto[]>([]);
  protected readonly selectedMemberUserId = signal<number | null>(null);
  protected readonly selectedMemberRole = signal<GroupRole>(GroupRole.MEMBER);
  protected readonly isAddingMember = signal(false);
  
  protected readonly updateGroupName = signal('');
  protected readonly isUpdatingGroup = signal(false);
  protected readonly isDeletingGroup = signal(false);
  protected readonly isRecalculatingElo = signal(false);

  protected readonly showGroupModal = signal(false);
  protected readonly groupModalMode = signal<'default' | 'create'>('default');
  protected readonly joinGroupCode = signal('');
  protected readonly isJoiningGroup = signal(false);
  protected readonly joinGroupError = signal('');

  // Expose enum to template
  protected get GroupRole() {
    return GroupRole;
  }

  private readonly loadedMembersGroupId = signal<number | null>(null);

  constructor() {
    this.loadGroups();
    this.loadUsers();

    effect(() => {
      const groupId = this.activeGroup()?.id ?? null;
      if (!groupId) {
        this.members.set([]);
        this.memberStatus.set('idle');
        this.memberError.set('');
        this.ranking.set([]);
        this.rankingStatus.set('idle');
        this.rankingError.set('');
        this.loadedMembersGroupId.set(null);
        return;
      }

      if (this.loadedMembersGroupId() === groupId) {
        return;
      }

      this.updateGroupName.set(this.activeGroup()?.name ?? '');
      this.loadedMembersGroupId.set(groupId);
      this.loadMembers(groupId);
      this.loadRanking(groupId);
    });

    effect(() => {
      this.updateFabConfig();
    });
  }

  ngOnDestroy(): void {
    this.fab.setConfig({ visible: false });
  }

  protected setTab(tab: GroupsTab): void {
    this.activeTab.set(tab);
  }

  private updateFabConfig(): void {
    const tab = this.activeTab();
    if (tab === 'overview') {
      this.fab.setConfig({
        icon: 'add',
        label: 'Adicionar Grupo',
        visible: true,
        action: () => {
          this.groupModalMode.set('default');
          this.joinGroupCode.set('');
          this.joinGroupError.set('');
          this.newGroupName.set('');
          this.showGroupModal.set(true);
        }
      });
    } else if (tab === 'members') {
      this.fab.setConfig({
        icon: 'person_add',
        label: 'Adicionar Membro',
        visible: true,
        adminOnly: true,
        action: () => {
          const id = prompt('ID do usuário para adicionar:');
          if (id) {
            this.selectedMemberUserId.set(Number(id));
            this.selectedMemberRole.set(GroupRole.MEMBER);
            this.addMember();
          }
        }
      });
    } else {
      this.fab.setConfig({ visible: false });
    }
  }

  protected loadGroups(): void {
    this.status.set('loading');
    this.errorMessage.set('');

    this.groupsApi.listMine().subscribe({
      next: (groups) => {
        this.state.groups.set(groups);
        this.status.set('success');
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel carregar os grupos.');
      },
    });
  }

  protected loadUsers(): void {
    this.usersApi.getAll().subscribe({
      next: (users) => this.users.set(users),
    });
  }

  protected loadMembers(groupId: number = this.activeGroup()?.id ?? 0): void {
    if (!groupId) {
      this.members.set([]);
      this.memberStatus.set('idle');
      return;
    }

    this.memberStatus.set('loading');
    this.memberError.set('');

    this.groupsApi.listMembers(groupId).subscribe({
      next: (members) => {
        this.members.set(members);
        this.memberStatus.set('success');
      },
      error: () => {
        this.memberStatus.set('error');
        this.memberError.set('Nao foi possivel carregar os membros do grupo.');
      },
    });
  }

  protected loadRanking(groupId: number = this.activeGroup()?.id ?? 0): void {
    if (!groupId) {
      this.ranking.set([]);
      this.rankingStatus.set('idle');
      return;
    }

    this.rankingStatus.set('loading');
    this.rankingError.set('');

    this.groupsApi.getRanking(groupId).subscribe({
      next: (ranking) => {
        this.ranking.set(ranking);
        this.rankingStatus.set('success');
      },
      error: () => {
        this.rankingStatus.set('error');
        this.rankingError.set('Nao foi possivel carregar o ranking do grupo.');
      },
    });
  }

  protected setNewGroupName(event: Event): void {
    this.newGroupName.set((event.target as HTMLInputElement).value);
  }

  protected createGroup(): void {
    const name = this.newGroupName().trim();
    if (!name || this.isCreating()) {
      return;
    }

    this.isCreating.set(true);
    this.groupsApi.create({ name } satisfies GroupRequestDto).subscribe({
      next: (group) => {
        this.auth.setCurrentGroup(group);
        this.isCreating.set(false);
        this.newGroupName.set('');
        this.showGroupModal.set(false);
        this.loadGroups();
      },
      error: () => {
        this.isCreating.set(false);
        this.toast.error('Nao foi possivel criar o grupo.');
      },
    });
  }

  protected setJoinGroupCode(event: Event): void {
    this.joinGroupCode.set((event.target as HTMLInputElement).value);
  }

  protected joinGroup(): void {
    const code = this.joinGroupCode().trim();
    if (!code || code.length !== 6 || this.isJoiningGroup()) {
      this.joinGroupError.set('O código deve ter 6 caracteres.');
      return;
    }

    this.isJoiningGroup.set(true);
    this.joinGroupError.set('');

    this.groupsApi.joinGroup({ code }).subscribe({
      next: (response) => {
        this.isJoiningGroup.set(false);
        this.showGroupModal.set(false);
        this.toast.success('Grupo adicionado com sucesso!');
        this.loadGroups();
        this.switchGroup(response);
      },
      error: (err) => {
        this.isJoiningGroup.set(false);
        if (err.status === 404) {
          this.joinGroupError.set('Código inválido. Verifique e tente novamente.');
        } else if (err.status === 409) {
          this.joinGroupError.set('Você já faz parte deste grupo.');
        } else {
          this.joinGroupError.set('Não foi possível entrar no grupo.');
        }
      }
    });
  }

  protected closeGroupModal(): void {
    this.showGroupModal.set(false);
  }

  protected setGroupModalMode(mode: 'default' | 'create'): void {
    this.groupModalMode.set(mode);
  }

  protected setSelectedMemberUserId(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    this.selectedMemberUserId.set(Number.isFinite(value) && value > 0 ? value : null);
  }

  protected setSelectedMemberRole(event: Event): void {
    this.selectedMemberRole.set((event.target as HTMLSelectElement).value as GroupRole);
  }

  protected addMember(): void {
    const groupId = this.activeGroup()?.id;
    const userId = this.selectedMemberUserId();
    const role = this.selectedMemberRole();

    if (!groupId || !userId || this.isAddingMember()) {
      return;
    }

    this.isAddingMember.set(true);
    this.groupsApi.addMember(groupId, { userId, role }).subscribe({
      next: () => {
        this.isAddingMember.set(false);
        this.selectedMemberUserId.set(null);
        this.selectedMemberRole.set(GroupRole.MEMBER);
        this.toast.success('Membro adicionado ao grupo.');
        this.loadMembers(groupId);
      },
      error: () => {
        this.isAddingMember.set(false);
        this.toast.error('Nao foi possivel adicionar o membro.');
      },
    });
  }

  protected switchGroup(group: GroupResponseDto): void {
    if (this.switchingGroupId() === group.id || this.activeGroup()?.id === group.id) {
      return;
    }

    if (!confirm(`Trocar para o grupo ${group.name}?`)) {
      return;
    }

    this.switchingGroupId.set(group.id);
    this.groupsApi.switchGroup(group.id).subscribe({
      next: (currentGroup: GroupSwitchResponseDto) => {
        const normalizedGroup = this.auth.setCurrentGroupFromSwitch(currentGroup);
        if (!normalizedGroup) {
          this.switchingGroupId.set(null);
          this.toast.error('Nao foi possivel trocar de grupo.');
          return;
        }
        this.state.notifyGroupSwitch();
        this.betCart.clearCart();
        this.switchingGroupId.set(null);
        this.loadGroups();
        void this.router.navigate(
          normalizedGroup.playerClaimed ? ['/'] : ['/groups', normalizedGroup.id, 'claim-player'],
        );
      },
      error: () => {
        this.switchingGroupId.set(null);
        this.toast.error('Nao foi possivel trocar de grupo.');
      },
    });
  }

  protected roleLabel(role: GroupRole | null): string {
    const labels: Record<GroupRole, string> = {
      [GroupRole.OWNER]: 'Dono',
      [GroupRole.ADMIN]: 'Admin',
      [GroupRole.MEMBER]: 'Membro',
    };

    return role ? labels[role] : 'Sem papel';
  }

  protected isActive(group: GroupResponseDto): boolean {
    return this.activeGroup()?.id === group.id;
  }

  protected availableUsers(): UserResponseDto[] {
    const memberIds = new Set(this.members().map((member) => member.userId));
    return this.users()
      .filter((user) => !memberIds.has(user.id))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  protected memberLabel(member: GroupMemberResponseDto): string {
    return `${member.userName} · ${this.roleLabel(member.role)}`;
  }

  protected createdAtLabel(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }

  protected setUpdateGroupName(event: Event): void {
    this.updateGroupName.set((event.target as HTMLInputElement).value);
  }

  protected updateActiveGroup(): void {
    const groupId = this.activeGroup()?.id;
    const name = this.updateGroupName().trim();
    if (!groupId || !name || this.isUpdatingGroup()) {
      return;
    }

    this.isUpdatingGroup.set(true);
    this.groupsApi.update(groupId, { name }).subscribe({
      next: (group) => {
        this.isUpdatingGroup.set(false);
        this.auth.setCurrentGroup(group);
        this.toast.success('Grupo atualizado com sucesso.');
        this.loadGroups();
      },
      error: () => {
        this.isUpdatingGroup.set(false);
        this.toast.error('Nao foi possivel atualizar o grupo.');
      }
    });
  }

  protected deleteActiveGroup(): void {
    const groupId = this.activeGroup()?.id;
    if (!groupId || this.isDeletingGroup()) {
      return;
    }

    if (!confirm('Deseja realmente excluir este grupo? Esta acao eh irreversivel e apagara todos os dados relacionados (jogadores, torneios, apostas).')) {
      return;
    }

    this.isDeletingGroup.set(true);
    this.groupsApi.delete(groupId).subscribe({
      next: () => {
        this.isDeletingGroup.set(false);
        this.auth.setCurrentGroup(null);
        void this.router.navigate(['/']);
        this.toast.success('Grupo excluido com sucesso.');
        this.loadGroups();
      },
      error: () => {
        this.isDeletingGroup.set(false);
        this.toast.error('Nao foi possivel excluir o grupo.');
      }
    });
  }

  protected recalculateElo(): void {
    const groupId = this.activeGroup()?.id;
    if (!groupId || this.isRecalculatingElo()) {
      return;
    }

    this.isRecalculatingElo.set(true);
    this.groupsApi.recalculateElo(groupId).subscribe({
      next: () => {
        this.isRecalculatingElo.set(false);
        this.toast.success('Elo recalculado com sucesso.');
      },
      error: () => {
        this.isRecalculatingElo.set(false);
        this.toast.error('Nao foi possivel recalcular o Elo.');
      }
    });
  }

  protected updateMemberRole(userId: number, event: Event): void {
    const groupId = this.activeGroup()?.id;
    if (!groupId) {
      return;
    }

    const role = (event.target as HTMLSelectElement).value as GroupRole;
    this.groupsApi.updateMemberRole(groupId, userId, { role }).subscribe({
      next: () => {
        this.toast.success('Papel do membro atualizado.');
        this.loadMembers(groupId);
      },
      error: () => {
        this.toast.error('Nao foi possivel atualizar o papel do membro.');
        this.loadMembers(groupId); // reload to revert select value
      }
    });
  }

  protected removeMember(userId: number): void {
    const groupId = this.activeGroup()?.id;
    if (!groupId) {
      return;
    }

    if (!confirm('Deseja remover este membro do grupo?')) {
      return;
    }

    this.groupsApi.removeMember(groupId, userId).subscribe({
      next: () => {
        this.toast.success('Membro removido com sucesso.');
        this.loadMembers(groupId);
      },
      error: () => {
        this.toast.error('Nao foi possivel remover o membro.');
      }
    });
  }
}
