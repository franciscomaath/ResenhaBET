import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, switchMap } from 'rxjs';

import { LoginResponseDto } from '../../services/api/api.models';
import { UsersApi } from '../../services/api/users-api';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly usersApi = inject(UsersApi);

  @Output() loginSuccess = new EventEmitter<LoginResponseDto>();

  protected readonly step = signal<'name' | 'pin' | 'register' | 'setup-pin'>('name');
  protected readonly nameValue = signal('');
  protected readonly pinValue = signal('');
  protected readonly newPinValue = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  private pendingLoginResponse: LoginResponseDto | null = null;

  protected submit(): void {
    if (this.loading()) {
      return;
    }

    const name = this.nameValue().trim();
    if (!name) {
      return;
    }

    const isPinStep = this.step() === 'pin';
    const pin = isPinStep ? this.pinValue() : undefined;

    if (isPinStep && !this.isPinValid()) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.auth.login(name, pin)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          if (response.pinRequired) {
            this.step.set('pin');
          } else {
            this.handleLoginSuccess(response);
          }
        },
        error: (err) => {
          if (err.status === 404) {
            this.error.set('Usuário não encontrado');
          } else if (err.status === 401) {
            this.error.set('PIN incorreto');
            this.pinValue.set('');
          } else {
            this.error.set('Erro ao conectar. Tente novamente.');
          }
        }
      });
  }

  protected showRegister(): void {
    this.step.set('register');
    this.nameValue.set('');
    this.error.set(null);
  }

  protected backToLogin(): void {
    this.step.set('name');
    this.nameValue.set('');
    this.pinValue.set('');
    this.newPinValue.set('');
    this.error.set(null);
    this.pendingLoginResponse = null;
  }

  protected submitRegister(): void {
    if (this.loading()) {
      return;
    }

    const name = this.nameValue().trim();
    if (!name) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.usersApi.create({ name }).pipe(
      switchMap(() => this.auth.login(name)),
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: (response) => {
        this.handleLoginSuccess(response);
      },
      error: (err) => {
        if (err.status === 409) {
          this.error.set('Esse nome já está em uso.');
        } else {
          this.error.set('Erro ao criar conta. Tente novamente.');
        }
      },
    });
  }

  protected skipPinSetup(): void {
    if (this.pendingLoginResponse) {
      this.auth.finalizeLogin(this.pendingLoginResponse);
      this.loginSuccess.emit(this.pendingLoginResponse);
      this.pendingLoginResponse = null;
    }
  }

  protected submitPinSetup(): void {
    if (this.loading() || !this.isNewPinValid() || !this.pendingLoginResponse) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.auth.setPin(this.newPinValue(), this.pendingLoginResponse.token).pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: () => {
        if (this.pendingLoginResponse) {
          this.pendingLoginResponse.hasPin = true;
          this.auth.finalizeLogin(this.pendingLoginResponse);
          this.loginSuccess.emit(this.pendingLoginResponse);
          this.pendingLoginResponse = null;
        }
      },
      error: () => {
        this.error.set('Erro ao definir PIN. Tente novamente.');
      },
    });
  }

  protected isPinValid(): boolean {
    return /^\d{4}$/.test(this.pinValue());
  }

  protected isNewPinValid(): boolean {
    return /^\d{4}$/.test(this.newPinValue());
  }

  private handleLoginSuccess(response: LoginResponseDto): void {
    if (!response.hasPin) {
      this.pendingLoginResponse = response;
      this.step.set('setup-pin');
    } else {
      this.loginSuccess.emit(response);
    }
  }
}
