import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { OddsButton } from '../components/betting/OddsButton';
import { api } from '../services/apiClient';
import '../components/betting/betting.css';

interface FixtureDetailState {
  fixture: any;
  odds: any;
}

export function FixtureDetails() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [data, setData] = useState<FixtureDetailState | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function load() {
      try {
        const [fixData, oddsData] = await Promise.all([
          api.get<any>(`/fixtures/${id}`),
          api.get<any>(`/odds/fixture/${id}`).catch(() => null) // Returns 404/null if no odds
        ]);
        
        setData({ fixture: fixData, odds: oddsData });
      } catch (err: any) {
        setError(err.message || 'Failed to load fixture details.');
      } finally {
        setLoading(false);
      }
    }
    load();
    
    // Poll for live odds/score if open
    const interval = setInterval(load, 15000);
    return () => clearInterval(interval);
  }, [id]);

  if (loading && !data) return <DashboardLayout><div className="feed-loading"><div className="loading-pulse" /><p>Loading markets...</p></div></DashboardLayout>;
  if (error) return <DashboardLayout><div style={{ padding: '1rem', color: 'var(--danger)', textAlign: 'center' }}>{error}</div></DashboardLayout>;
  if (!data?.fixture) return <DashboardLayout><div>Fixture not found.</div></DashboardLayout>;

  const { fixture, odds } = data;
  const isLive = ['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(fixture.status);
  const isLocked = fixture.status === 'LOCKED';
  const mr = fixture.matchResult;
  const score = mr ? `${mr.homeScore} - ${mr.awayScore}` : 'v';

  return (
    <DashboardLayout>
      <div className="fixture-details-header" style={{
        background: 'var(--card)', borderRadius: '1rem', padding: '2rem', textAlign: 'center', marginBottom: '2rem', border: '1px solid var(--border)', position: 'relative'
      }}>
        <button onClick={() => navigate(-1)} style={{
          position: 'absolute', top: '1.5rem', left: '1.5rem', background: 'transparent', color: 'var(--text-muted)', border: 'none', cursor: 'pointer', fontSize: '1rem'
        }}>← Back</button>
        
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '2rem', fontSize: '1.5rem', fontWeight: 600 }}>
          <div style={{ flex: 1, textAlign: 'right' }}>{fixture.homeTeamName}</div>
          <div style={{ padding: '1rem 2rem', background: 'var(--surface)', borderRadius: '0.5rem', fontSize: '2rem' }}>{score}</div>
          <div style={{ flex: 1, textAlign: 'left' }}>{fixture.awayTeamName}</div>
        </div>
        <div style={{ marginTop: '1rem', color: 'var(--text-muted)' }}>
          {isLive && <span style={{ color: 'var(--primary)' }}>● LIVE {fixture.matchMinute}'</span>}
          {isLocked && <span style={{ color: 'var(--warning)' }}>🔒 LOCKED</span>}
          {!isLive && !isLocked && <span>{new Date(fixture.scheduledAt).toLocaleString()}</span>}
        </div>
      </div>

      {!odds ? (
        <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
          Odds are currently unavailable for this fixture.
        </div>
      ) : (
        <div className="markets-grid" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          
          <div className="market-group" style={{ background: 'var(--card)', padding: '1.5rem', borderRadius: '1rem', border: '1px solid var(--border)' }}>
            <h3 style={{ marginBottom: '1rem' }}>Match Result</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
              <div className="market-col">
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Home Win</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="HOME_WIN" odds={odds.homeWinOdds} />
              </div>
              <div className="market-col">
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Draw</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="DRAW" odds={odds.drawOdds} />
              </div>
              <div className="market-col">
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Away Win</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="AWAY_WIN" odds={odds.awayWinOdds} />
              </div>
            </div>
          </div>

          <div className="market-group" style={{ background: 'var(--card)', padding: '1.5rem', borderRadius: '1rem', border: '1px solid var(--border)' }}>
            <h3 style={{ marginBottom: '1rem' }}>Total Goals (Over / Under)</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
              <div>
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Over 1.5</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="OVER_1_5" odds={odds.over15Odds} />
              </div>
              <div>
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Under 1.5</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="UNDER_1_5" odds={odds.under15Odds} />
              </div>
              <div>
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Over 2.5</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="OVER_2_5" odds={odds.over25Odds} />
              </div>
              <div>
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Under 2.5</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="UNDER_2_5" odds={odds.under25Odds} />
              </div>
              <div>
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Over 3.5</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="OVER_3_5" odds={odds.over35Odds} />
              </div>
              <div>
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Under 3.5</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="UNDER_3_5" odds={odds.under35Odds} />
              </div>
            </div>
          </div>

          <div className="market-group" style={{ background: 'var(--card)', padding: '1.5rem', borderRadius: '1rem', border: '1px solid var(--border)' }}>
            <h3 style={{ marginBottom: '1rem' }}>Both Teams To Score</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Yes</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="BTTS_YES" odds={odds.bttsYesOdds} />
              </div>
              <div>
                <span className="market-label" style={{ display: 'block', marginBottom: '0.5rem' }}>No</span>
                <OddsButton fixtureId={fixture.id} homeTeam={fixture.homeTeamName} awayTeam={fixture.awayTeamName} outcome="BTTS_NO" odds={odds.bttsNoOdds} />
              </div>
            </div>
          </div>

        </div>
      )}
    </DashboardLayout>
  );
}
