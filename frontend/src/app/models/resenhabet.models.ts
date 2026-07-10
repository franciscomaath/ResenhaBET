export type MainTab = 'betting' | 'history';
export type BetTarget = 'home' | 'draw' | 'away';

export interface Player {
  name: string;
  aura: number;
}

export interface Bettor {
  name: string;
  wallet: number;
}

export interface Bet {
  bettor: string;
  target: BetTarget;
  label: string;
  amount: number;
}

export interface Match {
  home: Player;
  away: Player;
  scoreHome: number;
  scoreAway: number;
  phase: string;
  elapsed: string;
  isKnockout: boolean;
  bettingOpen: boolean;
  odds: {
    home: number;
    draw: number | null;
    away: number;
  };
}

export interface ResolvedBet extends Bet {
  outcome: 'win' | 'loss';
  payout: number;
}

export interface MatchHistory {
  matchLabel: string;
  score: string;
  winner: string;
  bets: ResolvedBet[];
}

export interface ScorePayload {
  home: number;
  away: number;
}
