import { createContext, useContext, useState } from 'react';
import type { ReactNode } from 'react';

export type BetMode = 'SINGLES' | 'BUILDER';

export interface BetSelection {
  id: string; // Unique identifier: fixtureId_outcome
  fixtureId: string;
  homeTeam: string;
  awayTeam: string;
  outcome: string; // e.g. 'HOME_WIN', 'DRAW', 'AWAY_WIN'
  odds: number;
}

interface BetSlipContextType {
  selections: BetSelection[];
  addSelection: (selection: BetSelection) => void;
  removeSelection: (id: string) => void;
  stake: number;
  setStake: (stake: number) => void;
  potentialPayout: number;
  totalStake: number;
  clearSlip: () => void;
  betMode: BetMode;
  setBetMode: (mode: BetMode) => void;
  builderOdds: number;
}

const BetSlipContext = createContext<BetSlipContextType | undefined>(undefined);

export function BetSlipProvider({ children }: { children: ReactNode }) {
  const [selections, setSelections] = useState<BetSelection[]>([]);
  const [stake, setStake] = useState<number>(0);
  const [betMode, setBetMode] = useState<BetMode>('SINGLES');

  const addSelection = (selection: BetSelection) => {
    setSelections((prev) => {
      if (prev.find((s) => s.id === selection.id)) return prev;
      return [...prev, selection];
    });
  };

  const removeSelection = (id: string) => {
    setSelections((prev) => prev.filter(s => s.id !== id));
  };

  const clearSlip = () => {
    setSelections([]);
    setStake(0);
  };

  // Builder odds = product of all individual odds
  const builderOdds = selections.length >= 2
    ? selections.reduce((acc, curr) => acc * curr.odds, 1)
    : 0;

  // Calculate for Multi-Singles
  // If stake is "stake per bet", total cost is stake * length
  // Potential payout is the sum of payouts for all individual bets
  const totalStake = betMode === 'SINGLES'
    ? stake * selections.length
    : stake; // Builder is a single bet with one stake

  const potentialPayout = betMode === 'SINGLES'
    ? selections.reduce((acc, curr) => acc + (curr.odds * stake), 0)
    : builderOdds * stake; // Builder: combined odds × stake

  return (
    <BetSlipContext.Provider value={{
      selections,
      addSelection,
      removeSelection,
      stake,
      setStake,
      potentialPayout,
      totalStake,
      clearSlip,
      betMode,
      setBetMode,
      builderOdds,
    }}>
      {children}
    </BetSlipContext.Provider>
  );
}

export function useBetSlip() {
  const context = useContext(BetSlipContext);
  if (context === undefined) {
    throw new Error('useBetSlip must be used within a BetSlipProvider');
  }
  return context;
}
