import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';

import { BetSlipComponent } from './components/bet-slip/bet-slip';
import { AppShellComponent } from './components/layout/app-shell';
import { BottomNavComponent } from './components/layout/bottom-nav';
import { MobileHeaderComponent } from './components/layout/mobile-header';
import { Login } from './components/login/login';
import { GroupSwitchResponseDto } from './services/api/api.models';
import { GroupsApi } from './services/api/groups-api';
import { ToastHost } from './components/toast-host/toast-host';
import { ResenhaBetState } from './services/resenhabet-state';
import { AuthService } from './services/auth.service';
import { FabService } from './services/fab.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule, RouterOutlet, Login, ToastHost, BetSlipComponent, AppShellComponent, MobileHeaderComponent, BottomNavComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly state = inject(ResenhaBetState);
  protected readonly fab = inject(FabService);
  private readonly auth = inject(AuthService);
  private readonly groupsApi = inject(GroupsApi);
  private readonly router = inject(Router);

  protected joinCode = '';
  protected isJoiningGroup = false;
  protected joinGroupError = '';
  
  protected gateMode = 'join';
  protected newGroupName = '';
  protected isCreatingGroup = false;
  protected createGroupError = '';

  protected isClaimPlayerRoute(): boolean {
    const url = this.router.url.split('?')[0].split('#')[0];
    return /^\/groups\/\d+\/claim-player(?:\/)?$/.test(url);
  }

  protected isEventRoute(): boolean {
    const url = this.router.url.split('?')[0].split('#')[0];
    return /^\/events\/\d+(?:\/)?$/.test(url);
  }



  protected logout(): void {
    this.state.logout();
    void this.router.navigate(['/']);
  }

  protected submitJoinGroup(): void {
    const code = this.joinCode.trim();
    if (!code || code.length !== 6 || this.isJoiningGroup) {
      this.joinGroupError = 'O código deve ter 6 caracteres.';
      return;
    }

    this.isJoiningGroup = true;
    this.joinGroupError = '';
    
    this.groupsApi.joinGroup({ code }).subscribe({
      next: (response) => {
        this.isJoiningGroup = false;
        this.joinCode = '';
        this.resolveCurrentGroup(response.id);
      },
      error: (err) => {
        this.isJoiningGroup = false;
        if (err.status === 404) {
          this.joinGroupError = 'Código inválido. Verifique e tente novamente.';
        } else if (err.status === 409) {
          this.joinGroupError = 'Você já faz parte deste grupo.';
        } else {
          this.joinGroupError = 'Não foi possível entrar no grupo.';
        }
      }
    });
  }

  protected setGateMode(mode: 'join' | 'create'): void {
    this.gateMode = mode;
    this.joinGroupError = '';
    this.createGroupError = '';
    this.joinCode = '';
    this.newGroupName = '';
  }

  protected submitCreateGroup(): void {
    const name = this.newGroupName.trim();
    if (!name || this.isCreatingGroup) {
      this.createGroupError = 'O nome do grupo é obrigatório.';
      return;
    }

    this.isCreatingGroup = true;
    this.createGroupError = '';

    this.groupsApi.create({ name }).subscribe({
      next: (response) => {
        this.isCreatingGroup = false;
        this.newGroupName = '';
        this.resolveCurrentGroup(response.id);
      },
      error: () => {
        this.isCreatingGroup = false;
        this.createGroupError = 'Não foi possível criar o grupo.';
      }
    });
  }

  protected resolveCurrentGroup(groupId: number | null): void {
    if (!groupId) {
      void this.router.navigate(['/']);
      return;
    }

    this.groupsApi.switchGroup(groupId).subscribe({
      next: (group: GroupSwitchResponseDto) => {
        const currentGroup = this.auth.setCurrentGroupFromSwitch(group);
        if (!currentGroup) {
          this.state.loginStatus.set('error');
          this.state.loginError.set('Nao foi possivel carregar o contexto do grupo.');
          void this.router.navigate(['/']);
          return;
        }

        this.state.loadGroups();
        void this.router.navigate(
          currentGroup.playerClaimed ? ['/'] : ['/groups', currentGroup.id, 'claim-player'],
        );
      },
      error: () => {
        this.auth.resetSession();
        this.state.loginStatus.set('error');
        this.state.loginError.set('Nao foi possivel carregar o contexto do grupo.');
        void this.router.navigate(['/']);
      },
    });
  }
}
