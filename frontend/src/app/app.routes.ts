import { Routes } from '@angular/router';

import { HomePage } from './pages/home/home-page';
import { MyBetsPage } from './pages/my-bets/my-bets-page';
import { PlayersPage } from './pages/players/players-page';
import { GroupsPage } from './pages/groups/groups-page';
import { EventPage } from './pages/event/event-page';
import { TeamsPage } from './pages/teams/teams-page';
import { TournamentsListPage } from './pages/tournaments/tournaments-page';
import { TournamentPage } from './pages/tournament/tournament-page';
import { ClaimPlayerComponent } from './pages/claim-player/claim-player.component';
import { activeGroupGuard } from './guards/active-group.guard';
import { authGuard } from './guards/auth.guard';
import { claimPlayerGuard } from './guards/claim-player.guard';

export const routes: Routes = [
  { path: '', component: HomePage, canActivate: [activeGroupGuard] },
  { path: 'groups/:groupId/claim-player', component: ClaimPlayerComponent, canActivate: [authGuard, claimPlayerGuard] },
  { path: 'groups', component: GroupsPage, canActivate: [authGuard, activeGroupGuard] },
  { path: 'my-bets', component: MyBetsPage, canActivate: [authGuard, activeGroupGuard] },
  { path: 'players', component: PlayersPage, canActivate: [authGuard, activeGroupGuard] },
  { path: 'teams', component: TeamsPage, canActivate: [authGuard] },
  { path: 'tournaments', component: TournamentsListPage, canActivate: [authGuard, activeGroupGuard] },
  { path: 'tournaments/:id', component: TournamentPage, canActivate: [authGuard, activeGroupGuard] },
  { path: 'events/:id', component: EventPage, canActivate: [authGuard, activeGroupGuard] },
  { path: '**', redirectTo: '' },
];
