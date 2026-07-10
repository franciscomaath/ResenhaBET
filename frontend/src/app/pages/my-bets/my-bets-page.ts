import { CommonModule } from '@angular/common';
import { Component, effect, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { BetSlipResponseDto, EventResponseDto } from '../../services/api/api.models';
import { BetsApi } from '../../services/api/bets-api';
import { TournamentsApi } from '../../services/api/tournaments-api';
import { ResenhaBetState } from '../../services/resenhabet-state';
import { TEAM_TRANSLATIONS } from '../../pipes/team-translations';
import { AppSectionHeaderComponent } from '../../components/ui/app-section-header';

type LoadStatus = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-my-bets-page',
  imports: [CommonModule, AppSectionHeaderComponent],
  templateUrl: './my-bets-page.html',
})
export class MyBetsPage implements OnInit {
  private readonly betsApi = inject(BetsApi);
  private readonly tournamentsApi = inject(TournamentsApi);
  private readonly state = inject(ResenhaBetState);

  protected readonly status = signal<LoadStatus>('idle');
  protected readonly errorMessage = signal('');
  protected readonly betSlips = signal<BetSlipResponseDto[]>([]);
  protected readonly tournamentNames = signal<Record<number, string>>({});

  constructor() {
    effect(() => {
      this.state.groupSwitchVersion();
      this.loadTournaments();
      this.loadBets();
    });
  }

  ngOnInit(): void {}

  protected loadBets(): void {
    this.status.set('loading');
    this.errorMessage.set('');

    this.betsApi.getMyBets().subscribe({
      next: (bets) => {
        this.betSlips.set(bets);
        this.status.set('success');
      },
      error: () => {
        this.status.set('error');
        this.errorMessage.set('Nao foi possivel carregar suas apostas.');
      },
    });
  }

  protected tournamentName(tournamentId: number): string {
    return this.tournamentNames()[tournamentId] ?? `Torneio #${tournamentId}`;
  }

  private loadTournaments(): void {
    this.tournamentsApi.findAll(0, 100).subscribe({
      next: (page) => {
        this.tournamentNames.set(
          Object.fromEntries(page.content.map((tournament) => [tournament.id, tournament.name])),
        );
      },
      error: () => {
        this.tournamentNames.set({});
      },
    });
  }

  protected statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'Pendente',
      WON: 'Ganha',
      LOST: 'Perdida',
      CANCELLED: 'Cancelada',
    };
    return labels[status] ?? status;
  }

  protected itemStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'Aguardando',
      WON: 'Ganhou',
      LOST: 'Perdeu',
    };
    return labels[status] ?? status;
  }

  protected formatDatetime(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }

  protected homeLabel(event: EventResponseDto): string {
    return this.translatedTeamName(event.teamHomeName) || (event.playerHomeName ?? 'A definir');
  }

  protected awayLabel(event: EventResponseDto): string {
    return this.translatedTeamName(event.teamAwayName) || (event.playerAwayName ?? 'A definir');
  }

  protected outcomeLabel(outcomeName: string): string {
    return this.translatedTeamName(outcomeName) || outcomeName;
  }

  private translatedTeamName(value: string | null | undefined): string {
    if (!value) return '';
    return TEAM_TRANSLATIONS[value] ?? value;
  }
}
