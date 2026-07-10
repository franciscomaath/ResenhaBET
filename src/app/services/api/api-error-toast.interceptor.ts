import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth.service';
import { ToastService } from '../toast.service';

export const apiErrorToastInterceptor: HttpInterceptorFn = (request, next) => {
  const requestWithNgrokBypassHeader = addNgrokBrowserWarningHeader(request);
  const toastService = inject(ToastService);
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(requestWithNgrokBypassHeader).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (handleSessionRoutingError(error, request.url, authService, router)) {
          return throwError(() => error);
        }

        toastService.error(apiErrorMessage(error));
      }

      return throwError(() => error);
    }),
  );
};

function addNgrokBrowserWarningHeader(request: Parameters<HttpInterceptorFn>[0]) {
  if (!isNgrokHost(request.url)) {
    return request;
  }

  return request.clone({
    setHeaders: {
      'ngrok-skip-browser-warning': 'true',
    },
  });
}

function isNgrokHost(url: string): boolean {
  try {
    return new URL(url).hostname.endsWith('ngrok-free.app');
  } catch {
    return false;
  }
}

function apiErrorMessage(error: HttpErrorResponse): string {
  const backendMessage = extractBackendMessage(error.error);
  if (backendMessage) {
    return backendMessage;
  }

  if (error.status === 0) {
    return 'Nao foi possivel conectar ao backend.';
  }

  if (error.status === 401 || error.status === 403) {
    return 'Voce nao tem permissao para executar esta acao.';
  }

  if (error.status >= 500) {
    return 'Erro no servidor. Tente novamente em instantes.';
  }

  return 'Nao foi possivel concluir a acao.';
}

function handleSessionRoutingError(
  error: HttpErrorResponse,
  requestUrl: string,
  authService: AuthService,
  router: Router,
): boolean {
  if (requestUrl.includes('/auth/login') || error.status !== 401) {
    return false;
  }

  const message = extractBackendMessage(error.error);
  if (message === 'Token de sessao invalido ou expirado.') {
    authService.resetSession();
    void router.navigate(['/']);
    return true;
  }

  if (message === 'Grupo ativo nao selecionado.') {
    void router.navigate(['/groups']);
    return true;
  }

  return false;
}

function extractBackendMessage(errorBody: unknown): string | null {
  if (!errorBody || typeof errorBody !== 'object') {
    return null;
  }

  const body = errorBody as { message?: unknown; error?: unknown };
  if (typeof body.message === 'string' && body.message.trim()) {
    return body.message;
  }

  if (typeof body.error === 'string' && body.error.trim()) {
    return body.error;
  }

  return null;
}
