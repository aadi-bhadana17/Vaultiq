import { useBetSlip } from '../../context/BetSlipContext';
import './betting.css';

interface OddsButtonProps {
  fixtureId: string;
  homeTeam: string;
  awayTeam: string;
  outcome: string;
  odds: number;
}

export function OddsButton({ fixtureId, homeTeam, awayTeam, outcome, odds }: OddsButtonProps) {
  const { selections, addSelection, removeSelection } = useBetSlip();
  
  const selectionId = `${fixtureId}_${outcome}`;
  const isSelected = selections.some(s => s.id === selectionId);

  const handleClick = () => {
    if (isSelected) {
      removeSelection(selectionId);
    } else {
      addSelection({
        id: selectionId,
        fixtureId,
        homeTeam,
        awayTeam,
        outcome,
        odds
      });
    }
  };

  return (
    <button 
      className={`odds-btn ${isSelected ? 'selected' : ''}`}
      onClick={handleClick}
      aria-pressed={isSelected}
      aria-label={`${homeTeam} vs ${awayTeam} ${outcome.replace(/_/g, ' ')} odds ${odds.toFixed(2)}`}
    >
      <span className="odds-val">{odds.toFixed(2)}</span>
    </button>
  );
}
