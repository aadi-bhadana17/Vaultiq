import { useEffect, useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { api } from '../services/apiClient';
import { Modal } from '../components/ui/Modal';
import { Toast } from '../components/ui/Toast';
import { Skeleton } from '../components/ui/Skeleton';
import '../components/betting/betting.css';

interface UserProfile { id: string; username: string; }

interface TipsterProfile {
  id: string;
  userId: string;
  username: string;
  totalBets: number;
  totalWins: number;
  winRate: number;
  credibilityScore: number;
  cutPercentage: number;
  eligible: boolean;
}

interface TipsterBetPreview {
  betId: string;
  fixtureId: string;
  homeTeamName: string;
  awayTeamName: string;
  betType: string;
  fixtureStatus: string;
  betStatus: string;
}

interface TipsterFollower {
  tipsterUserId: string;
  tipsterUsername: string;
  active: boolean;
}

export function TipsterDirectory() {
  const [tipsters, setTipsters] = useState<TipsterProfile[]>([]);
  const [following, setFollowing] = useState<Set<string>>(new Set());
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [isTipster, setIsTipster] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [betPreviews, setBetPreviews] = useState<TipsterBetPreview[]>([]);
  const [loadingBets, setLoadingBets] = useState(false);
  const [copyBetId, setCopyBetId] = useState<string | null>(null);
  const [copyStake, setCopyStake] = useState('10');
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState('');
  const [msgTone, setMsgTone] = useState<'success' | 'error' | 'info'>('info');

  useEffect(() => { loadData(); }, []);

  async function loadData() {
    setLoading(true);
    try {
      const user = await api.get<UserProfile>('/auth/me');
      setCurrentUser(user);

      try {
        await api.get<TipsterProfile>(`/tipsters/${user.id}`);
        setIsTipster(true);
      } catch {
        setIsTipster(false);
      }

      const [tipsterList, followedList] = await Promise.all([
        api.get<TipsterProfile[]>('/tipsters'),
        api.get<TipsterFollower[]>('/tipsters/following'),
      ]);
      setTipsters(tipsterList);
      setFollowing(new Set(followedList.filter(f => f.active).map(f => f.tipsterUserId)));
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
    setLoading(false);
  }

  async function handleRegister() {
    try {
      await api.post('/tipsters/register', {});
      setIsTipster(true);
      setMsg('Registered as tipster!');
      setMsgTone('success');
      loadData();
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  async function toggleFollow(userId: string) {
    try {
      if (following.has(userId)) {
        await api.delete(`/tipsters/${userId}/follow`);
        setFollowing(prev => { const next = new Set(prev); next.delete(userId); return next; });
        setMsg('Unfollowed tipster');
      } else {
        await api.post(`/tipsters/${userId}/follow`, {});
        setFollowing(prev => new Set(prev).add(userId));
        setMsg('Now following tipster');
      }
      setMsgTone('success');
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  async function expandTipster(userId: string) {
    if (expandedId === userId) {
      setExpandedId(null);
      setBetPreviews([]);
      return;
    }
    setExpandedId(userId);
    setLoadingBets(true);
    try {
      const bets = await api.get<TipsterBetPreview[]>(`/tipsters/${userId}/bets`);
      setBetPreviews(bets);
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
      setBetPreviews([]);
    }
    setLoadingBets(false);
  }

  async function handleCopyBet(e: React.FormEvent) {
    e.preventDefault();
    if (!copyBetId) return;
    try {
      await api.post('/copy-bets', { originalBetId: copyBetId, stake: parseFloat(copyStake) });
      setMsg('Bet copied successfully!');
      setMsgTone('success');
      setCopyBetId(null);
      setCopyStake('10');
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  return (
    <DashboardLayout>
      <div className="feed-section-header">
        <span className="feed-section-dot live" />
        <h2>Tipster Directory</h2>
        <span className="feed-section-count">{tipsters.length}</span>
      </div>

      {!isTipster && currentUser && (
        <div className="fixture-card" style={{ marginBottom: '1.5rem', flexDirection: 'column', alignItems: 'flex-start', gap: '0.75rem' }}>
          <div>
            <h3 style={{ fontWeight: 600, marginBottom: '0.25rem' }}>Become a Tipster</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              Share your picks and earn a cut when followers copy your winning bets.
            </p>
          </div>
          <button className="btn btn-primary" onClick={handleRegister}>Register as Tipster</button>
        </div>
      )}

      {msg && <Toast message={msg} tone={msgTone} onClose={() => setMsg('')} />}

      {loading && (
        <div className="feed-loading">
          <Skeleton height={100} />
          <Skeleton height={100} />
        </div>
      )}

      {!loading && tipsters.length === 0 && (
        <div className="feed-empty">
          <h3>No Eligible Tipsters</h3>
          <p>No tipsters meet the eligibility criteria yet. Check back later!</p>
        </div>
      )}

      <div className="fixture-grid">
        {tipsters.map(t => (
          <div key={t.userId} className="fixture-card" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{t.username}</div>
                <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem', fontSize: '0.8125rem', color: 'var(--text-muted)', flexWrap: 'wrap' }}>
                  <span>Win Rate: <strong style={{ color: 'var(--foreground)' }}>{Number(t.winRate).toFixed(1)}%</strong></span>
                  <span>Credibility: <strong style={{ color: 'var(--foreground)' }}>{Number(t.credibilityScore).toFixed(1)}</strong></span>
                  <span>Bets: <strong style={{ color: 'var(--foreground)' }}>{t.totalBets}</strong></span>
                  <span>Cut: <strong style={{ color: 'var(--foreground)' }}>{Number(t.cutPercentage).toFixed(1)}%</strong></span>
                </div>
              </div>
              {currentUser?.id !== t.userId && (
                <button
                  className={`btn ${following.has(t.userId) ? '' : 'btn-primary'}`}
                  style={{ fontSize: '0.8125rem', padding: '0.375rem 0.875rem', flexShrink: 0,
                    ...(following.has(t.userId) ? { backgroundColor: 'var(--panel-border)', color: 'var(--foreground)' } : {}) }}
                  onClick={() => toggleFollow(t.userId)}
                >
                  {following.has(t.userId) ? 'Following' : 'Follow'}
                </button>
              )}
            </div>

            <button
              className="btn"
              style={{ fontSize: '0.8125rem', alignSelf: 'flex-start', backgroundColor: 'transparent', border: '1px solid var(--panel-border)', color: 'var(--accent)' }}
              onClick={() => expandTipster(t.userId)}
            >
              {expandedId === t.userId ? 'Hide Bets' : 'View Bets'}
            </button>

            {expandedId === t.userId && (
              <div style={{ borderTop: '1px solid var(--panel-border)', paddingTop: '0.75rem' }}>
                {loadingBets && <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Loading bets...</p>}
                {!loadingBets && betPreviews.length === 0 && (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>No pending bets to preview.</p>
                )}
                {betPreviews.map(bet => (
                  <div key={bet.betId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.625rem 0', borderBottom: '1px solid var(--panel-border)' }}>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '0.9375rem' }}>{bet.homeTeamName} vs {bet.awayTeamName}</div>
                      <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                        Type: {bet.betType.replace(/_/g, ' ')} · Outcome: <em>Hidden</em> · {bet.fixtureStatus}
                      </div>
                    </div>
                    {bet.betStatus === 'PENDING' && ['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(bet.fixtureStatus) && currentUser?.id !== t.userId && (
                      <button className="btn btn-primary" style={{ fontSize: '0.75rem', padding: '0.375rem 0.75rem' }}
                        onClick={() => setCopyBetId(bet.betId)}>
                        Copy
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>

      <Modal open={Boolean(copyBetId)} title="Copy bet" onClose={() => setCopyBetId(null)}>
        <h3 style={{ marginBottom: '1rem' }}>Copy Bet</h3>
        <form onSubmit={handleCopyBet} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <label style={{ fontSize: '0.875rem' }}>
            Stake (£)
            <input
              type="number" min="0.01" step="0.01" value={copyStake}
              onChange={e => setCopyStake(e.target.value)} required
              style={{ width: '100%', padding: '0.75rem', marginTop: '0.25rem', backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)', borderRadius: '0.375rem', color: 'var(--foreground)', fontFamily: 'inherit' }}
            />
          </label>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Place Copy Bet</button>
            <button type="button" className="btn" style={{ flex: 1, backgroundColor: 'var(--panel-border)', color: 'var(--foreground)' }} onClick={() => setCopyBetId(null)}>Cancel</button>
          </div>
        </form>
      </Modal>
    </DashboardLayout>
  );
}
