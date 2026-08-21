import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { OddsButton } from './OddsButton';
import './betting.css';

// Mirrors the backend LiveFeedResponse DTO shape
export interface LiveFixture {
  fixture: {
    id: string;
    seasonId: string;
    homeTeamName: string;
    homeTeamId: string;
    awayTeamName: string;
    awayTeamId: string;
    matchMinute: number;
    status: string; // SCHEDULED | OPEN | LOCKED | FINISHED
    scheduledAt: string;
    matchResult?: {
      homeScore: number;
      awayScore: number;
      homeRedCards: number;
      awayRedCards: number;
      homeYellowCards: number;
      awayYellowCards: number;
      isFinal: boolean;
    };
  };
  odds: {
    homeWinOdds: number;
    drawOdds: number;
    awayWinOdds: number;
    over15Odds: number;
    under15Odds: number;
    over25Odds: number;
    under25Odds: number;
    over35Odds: number;
    under35Odds: number;
    bttsYesOdds: number;
    bttsNoOdds: number;
  } | null;
}

function CountdownTimer({ scheduledAt }: { scheduledAt: string }) {
  const [timeLeft, setTimeLeft] = useState('');

  useEffect(() => {
    function calculate() {
      const diff = new Date(scheduledAt).getTime() - Date.now();
      if (diff <= 0) {
        setTimeLeft('Starting...');
        return;
      }
      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const secs = Math.floor((diff % (1000 * 60)) / 1000);

      if (days > 0) {
        setTimeLeft(`${days}d ${hours}h ${mins}m`);
      } else if (hours > 0) {
        setTimeLeft(`${hours}h ${mins}m ${secs}s`);
      } else {
        setTimeLeft(`${mins}m ${secs}s`);
      }
    }

    calculate();
    const interval = setInterval(calculate, 1000);
    return () => clearInterval(interval);
  }, [scheduledAt]);

  return <span className="countdown">{timeLeft}</span>;
}

export function FixtureCard({ data }: { data: LiveFixture }) {
  const { fixture, odds } = data;
  const navigate = useNavigate();
  
  const isLive = ['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(fixture.status);
  const isLocked = fixture.status === 'LOCKED';
  const isScheduled = fixture.status === 'SCHEDULED';
  const mr = fixture.matchResult;

  const score = mr ? `${mr.homeScore} - ${mr.awayScore}` : '-';

  return (
    <div className={`fixture-card ${isLive ? 'live' : ''} ${isLocked ? 'locked' : ''}`}>
      <div 
        className="fixture-info" 
        onClick={() => navigate(`/fixture/${fixture.id}`)}
        style={{ cursor: 'pointer' }}
      >
        <div className="fixture-teams">
          <span>{fixture.homeTeamName}</span>
          <span className="fixture-vs">v</span>
          <span>{fixture.awayTeamName}</span>
        </div>
        <div className="fixture-meta">
          {isLive && (
            <>
              <span className="fixture-live-badge">● LIVE</span>
              <span className="fixture-time">{fixture.matchMinute}'</span>
            </>
          )}
          {isLocked && <span className="fixture-locked-badge">🔒 LOCKED</span>}
          {isScheduled && (
            <span className="fixture-countdown-wrap">
              <span className="countdown-label">Starts in</span>
              <CountdownTimer scheduledAt={fixture.scheduledAt} />
            </span>
          )}
          {mr && <span className="fixture-score">{score}</span>}
          {mr && mr.homeRedCards > 0 && <span className="card-indicator">🟥 {fixture.homeTeamName} ({mr.homeRedCards})</span>}
          {mr && mr.awayRedCards > 0 && <span className="card-indicator">🟥 {fixture.awayTeamName} ({mr.awayRedCards})</span>}
        </div>
      </div>

      {odds ? (
        <div className="fixture-markets">
          <div className="market-col">
            <span className="market-label">1</span>
            <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="HOME_WIN" odds={odds.homeWinOdds} />
          </div>
          <div className="market-col">
            <span className="market-label">X</span>
            <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="DRAW" odds={odds.drawOdds} />
          </div>
          <div className="market-col">
            <span className="market-label">2</span>
            <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="AWAY_WIN" odds={odds.awayWinOdds} />
          </div>
        </div>
      ) : (
        <div className="fixture-markets">
          <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Odds unavailable</span>
        </div>
      )}
    </div>
  );
}
