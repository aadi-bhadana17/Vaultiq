import { useEffect, useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { api } from '../services/apiClient';
import { Skeleton } from '../components/ui/Skeleton';
import '../components/betting/betting.css';

interface CopyBet {
  id: string;
  copyBetId: string;
  originalBetId: string;
  tipsterUsername: string;
  fixtureId: string;
  homeTeamName: string;
  awayTeamName: string;
  betType: string;
  outcome: string;
  oddsAtPlacement: number;
  stake: number;
  potentialPayout: number;
  status: string;
  tipsterCut: number | null;
  createdAt: string;
}

function statusColor(status: string): string {
  if (status === 'WON') return 'var(--success)';
  if (status === 'LOST') return 'var(--danger)';
  return 'var(--warning)';
}

function statusBg(status: string): string {
  if (status === 'WON') return 'rgba(74, 222, 128, 0.15)';
  if (status === 'LOST') return 'rgba(251, 113, 133, 0.15)';
  return 'rgba(251, 191, 36, 0.15)';
}

export function CopyBets() {
  const [copyBets, setCopyBets] = useState<CopyBet[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function load() {
      try {
        const data = await api.get<CopyBet[]>('/copy-bets');
        const sorted = data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        setCopyBets(sorted);
      } catch (err: any) {
        setError(err.message || 'Failed to load copy bets.');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  return (
    <DashboardLayout>
      <div className="feed-section-header">
        <span className="feed-section-dot ongoing" />
        <h2>My Copy Bets</h2>
        <span className="feed-section-count">{copyBets.length}</span>
      </div>

      {loading && (
        <div className="feed-loading">
          <Skeleton height={80} />
          <Skeleton height={80} />
          <Skeleton height={80} />
          <p>Loading copy bets...</p>
        </div>
      )}

      {error && (
        <div style={{ padding: '1rem', color: 'var(--danger)', textAlign: 'center' }}>{error}</div>
      )}

      {!loading && !error && copyBets.length === 0 && (
        <div className="feed-empty">
          <h3>No Copy Bets</h3>
          <p>You haven't copied any tipster bets yet. Visit the Tipster Directory to get started!</p>
        </div>
      )}

      {!loading && !error && copyBets.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0', position: 'relative', paddingLeft: '1.5rem' }}>
          {/* Timeline line */}
          <div style={{ position: 'absolute', left: '0.375rem', top: '0.5rem', bottom: '0.5rem', width: '2px', backgroundColor: 'var(--panel-border)' }} />

          {copyBets.map((cb, i) => {
            const outcomeHidden = cb.outcome === 'HIDDEN';
            return (
              <div key={cb.id} style={{ position: 'relative', paddingBottom: i < copyBets.length - 1 ? '1.25rem' : 0 }}>
                {/* Timeline dot */}
                <div style={{
                  position: 'absolute', left: '-1.125rem', top: '1.25rem',
                  width: '10px', height: '10px', borderRadius: '50%',
                  backgroundColor: statusColor(cb.status), border: '2px solid var(--background)',
                }} />

                <div className="fixture-card" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '0.75rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>
                        {new Date(cb.createdAt).toLocaleString()} · Tipster: <strong style={{ color: 'var(--accent)' }}>{cb.tipsterUsername}</strong>
                      </div>
                      <div style={{ fontWeight: 700, fontSize: '1.0625rem' }}>
                        {cb.homeTeamName} vs {cb.awayTeamName}
                      </div>
                    </div>
                    <span style={{
                      fontSize: '0.75rem', fontWeight: 700, padding: '0.25rem 0.625rem', borderRadius: '0.25rem',
                      color: statusColor(cb.status), backgroundColor: statusBg(cb.status),
                    }}>
                      {cb.status}
                    </span>
                  </div>

                  <div style={{ display: 'flex', gap: '1.5rem', fontSize: '0.875rem', flexWrap: 'wrap' }}>
                    <span>Type: <strong>{cb.betType.replace(/_/g, ' ')}</strong></span>
                    <span>
                      Outcome:{' '}
                      {outcomeHidden
                        ? <em style={{ color: 'var(--text-muted)' }}>Hidden</em>
                        : <strong>{cb.outcome.replace(/_/g, ' ')}</strong>}
                    </span>
                    <span>Odds: <strong>{Number(cb.oddsAtPlacement).toFixed(2)}</strong></span>
                  </div>

                  <div style={{ display: 'flex', gap: '1.5rem', fontSize: '0.875rem', borderTop: '1px solid var(--panel-border)', paddingTop: '0.75rem' }}>
                    <span>Stake: <strong>£{Number(cb.stake).toFixed(2)}</strong></span>
                    <span>Potential: <strong>£{Number(cb.potentialPayout).toFixed(2)}</strong></span>
                    {cb.tipsterCut != null && cb.status === 'WON' && (
                      <span style={{ color: 'var(--text-muted)' }}>Tipster cut: £{Number(cb.tipsterCut).toFixed(2)}</span>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </DashboardLayout>
  );
}
