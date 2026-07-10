import { NO_ERRORS_SCHEMA, computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { ActiveGroupSwitcherComponent } from './components/active-group-switcher/active-group-switcher';
import { HomePage } from './pages/home/home-page';
import { MyBetsPage } from './pages/my-bets/my-bets-page';
import { GroupRole, TournamentResponseDto } from './services/api/api.models';
import { CompetitionsApi } from './services/api/competitions-api';
import { EventsApi } from './services/api/events-api';
import { GroupsApi } from './services/api/groups-api';
import { BetsApi } from './services/api/bets-api';
import { TournamentsApi } from './services/api/tournaments-api';
import { ResenhaBetState } from './services/resenhabet-state';
import { AuthService } from './services/auth.service';
import { BetCartService } from './services/bet-cart.service';

function createTournament(id: number, name: string, type: 'FIFA_MATCH' | 'REAL_FOOTBALL' = 'FIFA_MATCH'): TournamentResponseDto {
  return {
    id,
    uuid: `uuid-${id}`,
    name,
    format: type === 'REAL_FOOTBALL' ? 'LEAGUE_BRACKET' : 'LEAGUE',
    type,
    status: 'CREATED',
    startDate: null,
    endDate: null,
    generationMode: 'AUTO',
    hasThirdPlaceMatch: false,
    numberOfGroups: null,
    playersAdvancingPerGroup: null,
    groupTournamentId: type === 'REAL_FOOTBALL' ? 77 : null,
  };
}

describe('Phase 7 validations', () => {
  describe('ActiveGroupSwitcherComponent', () => {
    it('clears cart and notifies on successful switch', async () => {
      const groups = signal([
        { id: 1, name: 'Alpha', role: GroupRole.OWNER },
        { id: 2, name: 'Beta', role: GroupRole.MEMBER },
      ]);
      const currentGroupId = signal<number | null>(1);
      const currentGroupName = signal('Alpha');
      const currentGroupRole = signal<GroupRole | null>(GroupRole.OWNER);

      const auth = {
        currentGroupId: currentGroupId.asReadonly(),
        currentGroupName: currentGroupName.asReadonly(),
        currentGroupRole: currentGroupRole.asReadonly(),
        setCurrentGroup: vi.fn((group: { id: number; name: string; role: GroupRole } | null) => {
          currentGroupId.set(group?.id ?? null);
          currentGroupName.set(group?.name ?? '');
          currentGroupRole.set(group?.role ?? null);
        }),
      } as unknown as AuthService;

      const state = {
        groups,
        activeGroup: computed(() => {
          const id = currentGroupId();
          return id ? groups().find((group) => group.id === id) ?? null : null;
        }),
        notifyGroupSwitch: vi.fn(),
        loadGroups: vi.fn(),
      } as unknown as ResenhaBetState;

      const groupsApi = {
        switchGroup: vi.fn().mockReturnValue(of({ id: 2, name: 'Beta', role: GroupRole.MEMBER })),
      } as any;

      const betCart = { clearCart: vi.fn() } as unknown as BetCartService;
      const router = { navigate: vi.fn().mockResolvedValue(true) } as unknown as Router;

      vi.spyOn(window, 'confirm').mockReturnValue(true);

      await TestBed.configureTestingModule({
        imports: [ActiveGroupSwitcherComponent],
        providers: [
          { provide: GroupsApi, useValue: groupsApi },
          { provide: AuthService, useValue: auth },
          { provide: ResenhaBetState, useValue: state },
          { provide: BetCartService, useValue: betCart },
          { provide: Router, useValue: router },
        ],
        schemas: [NO_ERRORS_SCHEMA],
      }).compileComponents();

      const fixture = TestBed.createComponent(ActiveGroupSwitcherComponent);
      const switcher = fixture.componentInstance as unknown as {
        setSelectedGroupId(value: number | null): void;
        switchGroup(): void;
      };

      switcher.setSelectedGroupId(2);
      switcher.switchGroup();

      expect(groupsApi.switchGroup).toHaveBeenCalledWith(2);
      expect(auth.setCurrentGroup).toHaveBeenCalledWith({ id: 2, name: 'Beta', role: GroupRole.MEMBER });
      expect(state.notifyGroupSwitch).toHaveBeenCalled();
      expect(betCart.clearCart).toHaveBeenCalled();
      expect(state.loadGroups).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/']);
    });
  });

  describe('HomePage', () => {
    it('sends FIFA_MATCH create payload with market types', async () => {
      const tournamentsApi = {
        findAll: vi.fn().mockReturnValue(of({ content: [], totalPages: 0, totalElements: 0, last: true, first: true, size: 100, number: 0, numberOfElements: 0, empty: true })),
        findPlayersSummary: vi.fn().mockReturnValue(of({ playerCount: 0, players: [] })),
        create: vi.fn().mockReturnValue(of(createTournament(9, 'Copa FIFA', 'FIFA_MATCH'))),
      } as any;

      const eventsApi = { getLiveEvents: vi.fn().mockReturnValue(of([])) } as any;

      const competitionsApi = { findAll: vi.fn().mockReturnValue(of([])) } as any;

      const auth = { canManageGroup: signal(false) } as any;

      const state = { groupSwitchVersion: signal(0) } as unknown as ResenhaBetState;

      await TestBed.configureTestingModule({
        imports: [HomePage],
        providers: [
          { provide: TournamentsApi, useValue: tournamentsApi },
          { provide: EventsApi, useValue: eventsApi },
          { provide: CompetitionsApi, useValue: competitionsApi },
          { provide: AuthService, useValue: auth },
          { provide: ResenhaBetState, useValue: state },
        ],
        schemas: [NO_ERRORS_SCHEMA],
      }).compileComponents();

      const fixture = TestBed.createComponent(HomePage);
      const component = fixture.componentInstance as unknown as {
        newTournamentName: { set(value: string): void };
        selectedMarketTypes: { set(value: string[]): void };
        createTournament(): void;
      };

      component.newTournamentName.set('Copa FIFA');
      component.selectedMarketTypes.set(['MATCH_RESULT', 'BTTS']);
      component.createTournament();

      expect(tournamentsApi.create).toHaveBeenCalledWith({
        name: 'Copa FIFA',
        format: 'LEAGUE',
        type: 'FIFA_MATCH',
        marketTypes: ['MATCH_RESULT', 'BTTS'],
        generationMode: 'AUTO',
        hasThirdPlaceMatch: false,
        startDate: undefined,
        endDate: undefined,
        competitionId: undefined,
      });
    });

    it('blocks REAL_FOOTBALL creation without a competition', async () => {
      const tournamentsApi = {
        findAll: vi.fn().mockReturnValue(of({ content: [], totalPages: 0, totalElements: 0, last: true, first: true, size: 100, number: 0, numberOfElements: 0, empty: true })),
        findPlayersSummary: vi.fn().mockReturnValue(of({ playerCount: 0, players: [] })),
        create: vi.fn().mockReturnValue(of(createTournament(9, 'Copa Real', 'REAL_FOOTBALL'))),
      } as any;

      const eventsApi = { getLiveEvents: vi.fn().mockReturnValue(of([])) } as any;

      const competitionsApi = { findAll: vi.fn().mockReturnValue(of([])) } as any;

      const auth = { canManageGroup: signal(false) } as any;

      const state = { groupSwitchVersion: signal(0) } as unknown as ResenhaBetState;

      await TestBed.configureTestingModule({
        imports: [HomePage],
        providers: [
          { provide: TournamentsApi, useValue: tournamentsApi },
          { provide: EventsApi, useValue: eventsApi },
          { provide: CompetitionsApi, useValue: competitionsApi },
          { provide: AuthService, useValue: auth },
          { provide: ResenhaBetState, useValue: state },
        ],
        schemas: [NO_ERRORS_SCHEMA],
      }).compileComponents();

      const fixture = TestBed.createComponent(HomePage);
      const component = fixture.componentInstance as unknown as {
        newTournamentName: { set(value: string): void };
        newTournamentType: { set(value: string): void };
        selectedCompetitionId: { set(value: number | null): void };
        createTournament(): void;
        status(): string;
        errorMessage(): string;
      };

      component.newTournamentName.set('Copa Real');
      component.newTournamentType.set('REAL_FOOTBALL');
      component.selectedCompetitionId.set(null);
      component.createTournament();

      expect(tournamentsApi.create).not.toHaveBeenCalled();
      expect(component.status()).toBe('error');
      expect(component.errorMessage()).toContain('Selecione uma competicao');
    });

    it('sends competition-driven REAL_FOOTBALL create payload', async () => {
      const tournamentsApi = {
        findAll: vi.fn().mockReturnValue(of({ content: [], totalPages: 0, totalElements: 0, last: true, first: true, size: 100, number: 0, numberOfElements: 0, empty: true })),
        findPlayersSummary: vi.fn().mockReturnValue(of({ playerCount: 0, players: [] })),
        create: vi.fn().mockReturnValue(of(createTournament(9, 'Copa Real', 'REAL_FOOTBALL'))),
      } as any;

      const eventsApi = { getLiveEvents: vi.fn().mockReturnValue(of([])) } as any;

      const competitionsApi = { findAll: vi.fn().mockReturnValue(of([])) } as any;

      const auth = { canManageGroup: signal(false) } as any;

      const state = { groupSwitchVersion: signal(0) } as unknown as ResenhaBetState;

      await TestBed.configureTestingModule({
        imports: [HomePage],
        providers: [
          { provide: TournamentsApi, useValue: tournamentsApi },
          { provide: EventsApi, useValue: eventsApi },
          { provide: CompetitionsApi, useValue: competitionsApi },
          { provide: AuthService, useValue: auth },
          { provide: ResenhaBetState, useValue: state },
        ],
        schemas: [NO_ERRORS_SCHEMA],
      }).compileComponents();

      const fixture = TestBed.createComponent(HomePage);
      const component = fixture.componentInstance as unknown as {
        newTournamentName: { set(value: string): void };
        newTournamentType: { set(value: string): void };
        selectedCompetitionId: { set(value: number | null): void };
        selectedMarketTypes: { set(value: string[]): void };
        createTournament(): void;
        createNotice(): string;
      };

      component.newTournamentType.set('REAL_FOOTBALL');
      component.selectedCompetitionId.set(12);
      component.selectedMarketTypes.set(['MATCH_RESULT', 'BTTS']);
      component.createTournament();

      expect(tournamentsApi.create).toHaveBeenCalledWith({
        name: 'Tournament',
        format: 'LEAGUE_BRACKET',
        type: 'REAL_FOOTBALL',
        marketTypes: ['MATCH_RESULT', 'BTTS'],
        generationMode: 'MANUAL',
        hasThirdPlaceMatch: true,
        startDate: undefined,
        endDate: undefined,
        competitionId: 12,
      });
      expect(component.createNotice()).toContain('Grupo conectado');
    });
  });

  describe('MyBetsPage', () => {
    it('renders the tournament name for each bet slip', async () => {
      const betsApi = { getMyBets: vi.fn().mockReturnValue(of([
        {
          id: 1,
          userId: 1,
          tournamentId: 1,
          stake: 10,
          combinedOdd: 2,
          potentialReturn: 20,
          status: 'PENDING',
          createdAt: '2026-01-01T10:00:00Z',
          items: [],
        },
      ])) } as any;

      const tournamentsApi = { findAll: vi.fn().mockReturnValue(of({
        content: [createTournament(1, 'Copa da Resenha')],
        totalPages: 1,
        totalElements: 1,
        last: true,
        first: true,
        size: 100,
        number: 0,
        numberOfElements: 1,
        empty: false,
      })) } as any;

      const activatedRoute = { snapshot: { paramMap: new Map() } } as any;

      const state = { groupSwitchVersion: signal(0) } as unknown as ResenhaBetState;

      await TestBed.configureTestingModule({
        imports: [MyBetsPage],
        providers: [
          { provide: BetsApi, useValue: betsApi },
          { provide: TournamentsApi, useValue: tournamentsApi },
          { provide: ActivatedRoute, useValue: activatedRoute },
          { provide: ResenhaBetState, useValue: state },
        ],
        schemas: [NO_ERRORS_SCHEMA],
      }).compileComponents();

      const fixture = TestBed.createComponent(MyBetsPage);
      fixture.detectChanges();

      const text = fixture.nativeElement.textContent as string;
      expect(text).toContain('Copa da Resenha');
      expect(text).not.toContain('Torneio #1');
    });
  });
});
