export enum GroupRole {
  OWNER = 'OWNER',
  ADMIN = 'ADMIN',
  MEMBER = 'MEMBER',
}

export enum UserType {
  ADMIN = 'ADMIN',
  USER = 'USER',
}

export interface GroupResponseDto {
  id: number;
  name: string;
  role: GroupRole;
  groupCode?: string;
}

export interface GroupSwitchResponseDto extends Partial<GroupResponseDto> {
  groupId?: number;
  groupName?: string;
  groupRole?: GroupRole;
  playerClaimed: boolean;
}

export interface GroupMemberResponseDto {
  userId: number;
  userName: string;
  role: GroupRole;
  createdAt: string;
}

export interface GroupRequestDto {
  name: string;
}

export interface GroupJoinRequestDto {
  code: string;
}

export interface GroupMemberRequestDto {
  userId: number;
  role: GroupRole;
}

export interface PlayerResponseDto {
  id: number;
  name: string;
  active?: boolean;
  isActive?: boolean;
  userId?: number | null;
}

export interface AvailablePlayerResponseDto {
  id: number;
  name: string;
  currentElo: number;
}

export interface CreatePlayerRequestDto {
  name: string;
}

export interface UpdatePlayerRequestDto {
  name: string;
  active: boolean;
  isActive: boolean;
}

export interface ClaimPlayerRequestDto {
  playerId: number | null;
}

export interface UserResponseDto {
  id: number;
  name: string;
  username?: string | null;
  userType: UserType;
  firstLogin: boolean;
  hasPin: boolean;
  createdAt?: string | null;
}

export interface CreateUserRequestDto {
  name: string;
}

export interface LoginRequestDto {
  name: string;
  pin?: string;
}

export interface LoginResponseDto {
  token: string;
  id: number;
  name: string;
  userType: UserType;
  currentGroupId?: number | null;
  currentGroupName?: string | null;
  firstLogin: boolean;
  hasPin: boolean;
  pinRequired?: boolean;
}

export interface UpdatePinRequestDto {
  pin: string;
}

export interface TournamentResponseDto {
  id: number;
  uuid: string;
  name: string;
  format: string;
  type: 'FIFA_MATCH' | 'REAL_FOOTBALL' | string;
  status: 'CREATED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | string;
  startDate: string | null;
  endDate: string | null;
  generationMode: string;
  hasThirdPlaceMatch: boolean;
  numberOfGroups: number | null;
  playersAdvancingPerGroup: number | null;
  groupTournamentId?: number | null;
  rounds?: TournamentRoundResponseDto[];
}

export interface TournamentRoundResponseDto {
  id?: number;
  roundId: number;
  name: string;
  multiplier: number;
  roundOrder: number;
  phaseType: string;
  groupNumber: number | null;
}

export interface TournamentPlayerResponseDto {
  id?: number;
  tournamentPlayerId: number;
  tournamentId: number;
  playerId: number;
  playerName: string;
  teamId: number | null;
  teamName: string | null;
  groupNumber: number | null;
}

export interface TournamentPlayersResponseDto {
  playerCount: number;
  players: TournamentPlayerResponseDto[];
}

export interface TeamResponseDto {
  id: number;
  name: string;
  abbreviation: string;
  badgeUrl: string | null;
  gameForecastTeamId?: string | null;
}

export interface CreateTeamRequestDto {
  name: string;
  abbreviation: string;
  badgeUrl?: string | null;
}

export interface AddTournamentPlayerRequestDto {
  playerId: number;
}

export interface UpdateTournamentPlayerTeamRequestDto {
  teamId: number;
}

export interface CreateTournamentRequestDto {
  name: string;
  format: string;
  type?: string;
  marketTypes?: string[];
  generationMode?: string;
  hasThirdPlaceMatch?: boolean;
  startDate?: string | null;
  endDate?: string | null;
  competitionId?: number | null;
}

export interface EventResponseDto {
  id: number;
  tournamentId: number;
  roundId: number;
  playerHomeId: number | null;
  playerAwayId: number | null;
  playerHomeName: string | null;
  playerAwayName: string | null;
  teamHomeId: number | null;
  teamAwayId: number | null;
  teamHomeName: string | null;
  teamAwayName: string | null;
  gameDatetime: string | null;
  status: 'CREATED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'PENALTIES' | string;
  homeScore: number | null;
  awayScore: number | null;
  isKnockout: boolean;
  isBye: boolean;
  penaltiesHome: number | null;
  penaltiesAway: number | null;
  nextRoundEventId: number | null;
  homeSourceEventId: number | null;
  awaySourceEventId: number | null;
  thirdPlaceMatch: boolean;
}

export interface CreateEventRequestDto {
  tournamentId: number;
  roundId: number;
  playerHomeId: number;
  playerAwayId: number;
  gameDatetime?: string;
}

export interface CompletedEventRequestDto {
  tournamentId: number;
  roundId: number;
  playerHomeId?: number | null;
  playerAwayId?: number | null;
  homeScore: number;
  awayScore: number;
  gameDatetime?: string;
  isBye?: boolean;
}

export interface UpdateEventScoreRequestDto {
  homeScore: number;
  awayScore: number;
}

// ─── Betting / Markets / Wallet ────────────────────────────────────────

export type MarketType =
  | 'MATCH_RESULT'
  | 'OVER_UNDER_25'
  | 'OVER_UNDER_35'
  | 'BTTS'
  | 'EXACT_SCORE'
  | 'QUALIFY';

export interface MarketResponseDto {
  id: number;
  eventId: number;
  name: string;
  marketType: MarketType;
  status: 'OPEN' | 'SUSPENDED' | 'CLOSED' | 'CANCELLED' | string;
  outcomes: OutcomeResponseDto[];
}

export interface OutcomeResponseDto {
  id: number;
  name: string;
  odd: number;
}

export interface UpdateMarketStatusRequestDto {
  status: 'OPEN' | 'CLOSED';
}

export interface CreateBetSlipItemRequestDto {
  eventId: number;
  marketId: number;
  outcomeId: number;
}

export interface CreateBetSlipRequestDto {
  tournamentId: number;
  stake: number;
  items: CreateBetSlipItemRequestDto[];
}

export interface BetSlipItemResponseDto {
  id: number;
  eventId: number;
  outcomeId: number;
  outcomeName: string;
  oddSnapshot: number;
  status: 'PENDING' | 'WON' | 'LOST' | string;
  event: EventResponseDto;
}

export interface BetSlipResponseDto {
  id: number;
  userId: number;
  tournamentId: number;
  groupTournamentId?: number | null;
  stake: number;
  combinedOdd: number;
  potentialReturn: number;
  status: 'PENDING' | 'WON' | 'LOST' | 'CANCELLED' | string;
  createdAt: string;
  items: BetSlipItemResponseDto[];
}

export interface BetRankingResponseDto {
  userId: number;
  userName: string;
  balance: number;
}

export interface WalletResponseDto {
  userId: number;
  tournamentId?: number;
  groupTournamentId?: number | null;
  balance: number;
  initialBalance?: number;
}

export interface WalletDepositRequestDto {
  userId: number;
  tournamentId?: number;
  amount: number;
}

// ─── Bracket / Tournament Format ──────────────────────────────────────

export interface StartTournamentRequestDto {
  numberOfGroups?: number | null;
  playersAdvancingPerGroup?: number | null;
}

export interface TournamentGroupConfigResponseDto {
  playerCount: number;
  validOptions: TournamentGroupOptionDto[];
}

export interface TournamentGroupOptionDto {
  groupCount: number;
  playersPerGroup: number;
  remainder: number;
}

export interface PlayerStatsResponseDto {
  playerId: number;
  playerName: string;
  matchesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
  goalsScored: number;
  goalsConceded: number;
  goalDifference: number;
  points: number;
  currentElo: number;
  tournamentsWon: number;
}

export interface TeamStatsResponseDto {
  teamId: number;
  teamName: string;
  matchesPlayed: number;
  wins: number;
  losses: number;
  draws: number;
  goalsScored: number;
  goalsConceded: number;
  goalDifference: number;
  points: number;
}

export interface GroupStandingsDto {
  groupNumber: number;
  groupName?: string;
  standings: PlayerStatsResponseDto[];
  teamStandings?: TeamStatsResponseDto[];
}

export interface BracketPlacementDto {
  playerId: number;
  playerName: string;
  position: number;
  eliminationRound: string;
}

export interface TournamentScoreboardResponseDto {
  tournamentId: number;
  tournamentName: string;
  format: string;
  entries?: PlayerStatsResponseDto[];
  groups?: GroupStandingsDto[];
  placements?: BracketPlacementDto[];
}

export interface PageResponseDto<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  numberOfElements: number;
  empty: boolean;
}

export interface SyncResultDto {
  eventsCreated: number;
  eventsUpdated: number;
  teamsLinked: number;
  roundsCreated: number;
  marketsCreated: number;
  oddsImported: number;
}

export interface OddsImportResultDto {
  marketsCreated: number;
  marketsUpdated: number;
  outcomesCreated: number;
  oddsUpdated: number;
}

export interface CompetitionResponseDto {
  id: number;
  uuid: string;
  name: string;
  season: string;
  apiFootballLeagueId: string;
  apiFootballCountryId: string;
  gameForecastLeagueId: string;
  active: boolean;
  createdAt: string;
}

export interface CompetitionRequestDto {
  name: string;
  season: string;
  apiFootballLeagueId: string;
  apiFootballCountryId: string;
  gameForecastLeagueId: string;
}

export interface FinishEventRequestDto {
  penaltiesHome?: number;
  penaltiesAway?: number;
  status?: 'COMPLETED' | string;
}

export interface PatchEventPlayersRequestDto {
  playerHomeId?: number;
  playerAwayId?: number;
}

export interface PatchEventDatetimeRequestDto {
  gameDatetime: string | null;
}

export interface PatchGroupRoleRequestDto {
  role: GroupRole;
}

export interface PatchPlayerActiveRequestDto {
  active: boolean;
}

export interface PatchTeamGameForecastIdRequestDto {
  gameForecastTeamId: string | null;
}

export interface PatchGroupRequestDto {
  name: string;
  active?: boolean;
}

export interface PatchTournamentRequestDto {
  name?: string;
  status?: string;
}

export interface PatchUserRequestDto {
  name?: string;
  userType?: UserType;
}

export interface UpdateEventRequestDto {
  status?: string;
  homeScore?: number;
  awayScore?: number;
}

