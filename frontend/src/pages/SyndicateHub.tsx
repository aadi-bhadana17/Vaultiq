import { useEffect, useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { api } from '../services/apiClient';
import { Modal } from '../components/ui/Modal';
import { Toast } from '../components/ui/Toast';
import { Skeleton } from '../components/ui/Skeleton';
import '../components/betting/betting.css';

interface UserProfile { id: string; username: string; }

interface SyndicateMember {
  username: string;
  contribution: number;
}

interface SyndicateBet {
  fixtureId: string;
  homeTeamName: string;
  awayTeamName: string;
  outcome: string;
  oddsAtPlacement: number;
  status: string;
}

interface Syndicate {
  id: string;
  name: string;
  creatorUsername: string;
  targetStake: number;
  currentPool: number;
  status: string;
  members: SyndicateMember[];
  bet: SyndicateBet | null;
  createdAt: string;
}

interface LiveFixture {
  fixture: { id: string; homeTeamName: string; awayTeamName: string; status: string };
}

const OUTCOMES = [
  'HOME_WIN', 'DRAW', 'AWAY_WIN',
  'OVER_1_5', 'UNDER_1_5', 'OVER_2_5', 'UNDER_2_5', 'OVER_3_5', 'UNDER_3_5',
  'BTTS_YES', 'BTTS_NO',
];

const MY_SYNDICATES_KEY = 'vaultiq_my_syndicates';

function trackSyndicate(id: string) {
  const stored: string[] = JSON.parse(localStorage.getItem(MY_SYNDICATES_KEY) || '[]');
  if (!stored.includes(id)) {
    localStorage.setItem(MY_SYNDICATES_KEY, JSON.stringify([...stored, id]));
  }
}

type Tab = 'browse' | 'mine';

export function SyndicateHub() {
  const [tab, setTab] = useState<Tab>('browse');
  const [openSyndicates, setOpenSyndicates] = useState<Syndicate[]>([]);
  const [mySyndicates, setMySyndicates] = useState<Syndicate[]>([]);
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [fixtures, setFixtures] = useState<LiveFixture[]>([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState('');
  const [msgTone, setMsgTone] = useState<'success' | 'error' | 'info'>('info');

  // Create form
  const [syndicateName, setSyndicateName] = useState('');
  const [targetStake, setTargetStake] = useState('100');

  // Join modal
  const [joinId, setJoinId] = useState<string | null>(null);
  const [contribution, setContribution] = useState('10');

  // Place bet form (my syndicates)
  const [betSyndicateId, setBetSyndicateId] = useState('');
  const [betFixtureId, setBetFixtureId] = useState('');
  const [betOutcome, setBetOutcome] = useState('HOME_WIN');

  useEffect(() => { loadData(); }, []);

  async function loadMySyndicates(username: string, open: Syndicate[]) {
    const storedIds: string[] = JSON.parse(localStorage.getItem(MY_SYNDICATES_KEY) || '[]');
    const openIds = open.filter(s => s.creatorUsername === username).map(s => s.id);
    const allIds = [...new Set([...storedIds, ...openIds])];

    const results = await Promise.all(
      allIds.map(id => api.get<Syndicate>(`/syndicates/${id}`).catch(() => null))
    );
    return results.filter((s): s is Syndicate => s !== null && s.creatorUsername === username);
  }

  async function loadData() {
    setLoading(true);
    try {
      const user = await api.get<UserProfile>('/auth/me');
      setCurrentUser(user);

      const [syndicates, feed] = await Promise.all([
        api.get<Syndicate[]>('/syndicates'),
        api.get<LiveFixture[]>('/fixtures/feed'),
      ]);
      setOpenSyndicates(syndicates);
      setFixtures(feed.filter(f => ['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.fixture.status)));

      const mine = await loadMySyndicates(user.username, syndicates);
      setMySyndicates(mine);
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
    setLoading(false);
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    try {
      const created = await api.post<Syndicate>('/syndicates', {
        name: syndicateName,
        targetStake: parseFloat(targetStake),
      });
      trackSyndicate(created.id);
      setMsg('Syndicate created!');
      setMsgTone('success');
      setSyndicateName('');
      setTargetStake('100');
      loadData();
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  async function handleJoin(e: React.FormEvent) {
    e.preventDefault();
    if (!joinId) return;
    try {
      await api.post(`/syndicates/${joinId}/join`, { contribution: parseFloat(contribution) });
      setMsg('Joined syndicate!');
      setMsgTone('success');
      setJoinId(null);
      setContribution('10');
      loadData();
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  async function handlePlaceBet(e: React.FormEvent) {
    e.preventDefault();
    if (!betSyndicateId || !betFixtureId) return;
    try {
      await api.post(`/syndicates/${betSyndicateId}/bet`, {
        fixtureId: betFixtureId,
        outcome: betOutcome,
      });
      setMsg('Syndicate bet placed!');
      setMsgTone('success');
      setBetSyndicateId('');
      setBetFixtureId('');
      loadData();
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  async function handleCancel(id: string) {
    if (!confirm('Cancel this syndicate and refund all members?')) return;
    try {
      await api.post(`/syndicates/${id}/cancel`, {});
      setMsg('Syndicate cancelled — members refunded.');
      setMsgTone('success');
      loadData();
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  const inputStyle: React.CSSProperties = {
    width: '100%', padding: '0.75rem', marginTop: '0.25rem',
    backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)',
    borderRadius: '0.375rem', color: 'var(--foreground)', fontFamily: 'inherit',
  };

  const tabStyle = (active: boolean): React.CSSProperties => ({
    padding: '0.5rem 1rem', borderRadius: '0.375rem', cursor: 'pointer', fontFamily: 'inherit', fontSize: '0.875rem',
    border: '1px solid var(--panel-border)',
    backgroundColor: active ? 'var(--accent)' : 'var(--panel)',
    color: active ? 'white' : 'var(--text-muted)',
    borderColor: active ? 'var(--accent)' : 'var(--panel-border)',
  });

  const openMine = mySyndicates.filter(s => s.status === 'OPEN');

  return (
    <DashboardLayout>
      <div className="feed-section-header">
        <span className="feed-section-dot live" />
        <h2>Syndicate Hub</h2>
      </div>

      {msg && <Toast message={msg} tone={msgTone} onClose={() => setMsg('')} />}

      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
        <button style={tabStyle(tab === 'browse')} onClick={() => setTab('browse')}>Browse & Create</button>
        <button style={tabStyle(tab === 'mine')} onClick={() => setTab('mine')}>My Syndicates ({openMine.length})</button>
      </div>

      {loading && (
        <div className="feed-loading"><Skeleton height={120} /><Skeleton height={120} /></div>
      )}

      {!loading && tab === 'browse' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
          {/* Open Syndicates */}
          <section>
            <h3 style={{ fontWeight: 600, marginBottom: '1rem' }}>Open Syndicates</h3>
            {openSyndicates.length === 0 && (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>No open syndicates right now.</p>
            )}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {openSyndicates.map(s => {
                const progress = Math.min(100, (s.currentPool / s.targetStake) * 100);
                const isMember = s.members.some(m => m.username === currentUser?.username);
                const isCreator = s.creatorUsername === currentUser?.username;
                return (
                  <div key={s.id} className="fixture-card" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '0.75rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <div>
                        <div style={{ fontWeight: 700 }}>{s.name}</div>
                        <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>by {s.creatorUsername}</div>
                      </div>
                      <span style={{ fontSize: '0.8125rem', color: 'var(--accent)', fontWeight: 600 }}>{s.members.length} members</span>
                    </div>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem', marginBottom: '0.375rem' }}>
                        <span>£{Number(s.currentPool).toFixed(2)} / £{Number(s.targetStake).toFixed(2)}</span>
                        <span>{progress.toFixed(0)}%</span>
                      </div>
                      <div style={{ height: '6px', backgroundColor: 'var(--panel-border)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div style={{ height: '100%', width: `${progress}%`, backgroundColor: 'var(--accent)', borderRadius: '3px', transition: 'width 0.3s' }} />
                      </div>
                    </div>
                    {!isCreator && !isMember && (
                      <button className="btn btn-primary" style={{ fontSize: '0.8125rem', alignSelf: 'flex-start' }}
                        onClick={() => { setJoinId(s.id); setContribution('10'); }}>
                        Join
                      </button>
                    )}
                    {isMember && !isCreator && (
                      <span style={{ fontSize: '0.8125rem', color: 'var(--success)' }}>✓ Joined</span>
                    )}
                  </div>
                );
              })}
            </div>
          </section>

          {/* Create Syndicate */}
          <section>
            <h3 style={{ fontWeight: 600, marginBottom: '1rem' }}>Create Syndicate</h3>
            <form onSubmit={handleCreate} className="fixture-card" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '1rem' }}>
              <label style={{ fontSize: '0.875rem' }}>
                Name
                <input value={syndicateName} onChange={e => setSyndicateName(e.target.value)} required placeholder="Weekend Warriors" style={inputStyle} />
              </label>
              <label style={{ fontSize: '0.875rem' }}>
                Target Stake (£)
                <input type="number" min="1" step="0.01" value={targetStake} onChange={e => setTargetStake(e.target.value)} required style={inputStyle} />
              </label>
              <button type="submit" className="btn btn-primary">Create Syndicate</button>
            </form>
          </section>
        </div>
      )}

      {!loading && tab === 'mine' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {mySyndicates.length === 0 && (
            <div className="feed-empty">
              <h3>No Syndicates Yet</h3>
              <p>Create a syndicate from the Browse tab to get started.</p>
            </div>
          )}

          {mySyndicates.map(s => (
            <div key={s.id} className="fixture-card" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '0.75rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{s.name}</div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                    £{Number(s.currentPool).toFixed(2)} / £{Number(s.targetStake).toFixed(2)} · {s.members.length} members
                  </div>
                </div>
                <span style={{
                  fontSize: '0.75rem', fontWeight: 600, padding: '0.25rem 0.625rem', borderRadius: '0.25rem',
                  color: s.status === 'OPEN' ? 'var(--warning)' : s.status === 'PLACED' ? 'var(--accent)' : 'var(--text-muted)',
                  backgroundColor: s.status === 'OPEN' ? 'rgba(251, 191, 36, 0.15)' : 'rgba(56, 189, 248, 0.15)',
                }}>
                  {s.status}
                </span>
              </div>

              {s.bet && (
                <div style={{ fontSize: '0.875rem', padding: '0.75rem', backgroundColor: 'var(--background-elevated)', borderRadius: '0.375rem' }}>
                  Bet: {s.bet.homeTeamName} vs {s.bet.awayTeamName} · {s.bet.outcome.replace(/_/g, ' ')} @ {Number(s.bet.oddsAtPlacement).toFixed(2)}
                </div>
              )}

              {s.status === 'OPEN' && (
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button className="btn" style={{ fontSize: '0.8125rem', backgroundColor: 'var(--danger)', color: 'white' }}
                    onClick={() => handleCancel(s.id)}>
                    Cancel & Refund
                  </button>
                </div>
              )}
            </div>
          ))}

          {openMine.length > 0 && (
            <div className="fixture-card" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '1rem' }}>
              <h3 style={{ fontWeight: 600 }}>Place Syndicate Bet</h3>
              <form onSubmit={handlePlaceBet} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <label style={{ fontSize: '0.875rem' }}>
                  Syndicate
                  <select value={betSyndicateId} onChange={e => setBetSyndicateId(e.target.value)} required style={inputStyle}>
                    <option value="">Select syndicate</option>
                    {openMine.map(s => (
                      <option key={s.id} value={s.id}>{s.name} (£{Number(s.currentPool).toFixed(2)} pooled)</option>
                    ))}
                  </select>
                </label>
                <label style={{ fontSize: '0.875rem' }}>
                  Fixture
                  <select value={betFixtureId} onChange={e => setBetFixtureId(e.target.value)} required style={inputStyle}>
                    <option value="">Select fixture</option>
                    {fixtures.map(f => (
                      <option key={f.fixture.id} value={f.fixture.id}>
                        {f.fixture.homeTeamName} vs {f.fixture.awayTeamName}
                      </option>
                    ))}
                  </select>
                </label>
                <label style={{ fontSize: '0.875rem' }}>
                  Outcome
                  <select value={betOutcome} onChange={e => setBetOutcome(e.target.value)} required style={inputStyle}>
                    {OUTCOMES.map(o => (
                      <option key={o} value={o}>{o.replace(/_/g, ' ')}</option>
                    ))}
                  </select>
                </label>
                <button type="submit" className="btn btn-primary">Place Bet</button>
              </form>
            </div>
          )}
        </div>
      )}

      <Modal open={Boolean(joinId)} title="Join syndicate" onClose={() => setJoinId(null)}>
        <h3 style={{ marginBottom: '1rem' }}>Join Syndicate</h3>
        <form onSubmit={handleJoin} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <label style={{ fontSize: '0.875rem' }}>
            Contribution (£)
            <input type="number" min="0.01" step="0.01" value={contribution}
              onChange={e => setContribution(e.target.value)} required style={inputStyle} />
          </label>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Join</button>
            <button type="button" className="btn" style={{ flex: 1, backgroundColor: 'var(--panel-border)', color: 'var(--foreground)' }} onClick={() => setJoinId(null)}>Cancel</button>
          </div>
        </form>
      </Modal>
    </DashboardLayout>
  );
}
