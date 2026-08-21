import { useEffect, useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { api } from '../services/apiClient';
import { Modal } from '../components/ui/Modal';
import { Toast } from '../components/ui/Toast';
import '../components/betting/betting.css';

interface League { id: string; name: string; description: string; createdByUsername: string; seasonCount: number; }
interface Season { id: string; name: string; active: boolean; startDate: string; }
interface Team { id: string; name: string; strength: number; }
interface FixtureOddsAdmin {
  homeWinOdds: number; drawOdds: number; awayWinOdds: number;
  over15Odds: number; under15Odds: number; over25Odds: number; under25Odds: number;
  over35Odds: number; under35Odds: number; bttsYesOdds: number; bttsNoOdds: number;
  totalHomeStake: number; totalDrawStake: number; totalAwayStake: number;
}
interface Fixture {
  id: string; homeTeamName: string; awayTeamName: string; matchMinute: number;
  status: string; scheduledAt: string; platformProfit?: number;
  matchResult?: { homeScore: number; awayScore: number; isFinal: boolean; };
}
interface LiveItem { fixture: Fixture; odds: FixtureOddsAdmin | null; }

interface BetSuspicionFlag {
  id: string;
  userId: string;
  username: string;
  betId: string | null;
  reason: string;
  details: string;
  resolved: boolean;
  createdAt: string;
}

export function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'MODERATION'>('OVERVIEW');

  // Overview State
  const [leagues, setLeagues] = useState<League[]>([]);
  const [selectedLeague, setSelectedLeague] = useState<League | null>(null);
  const [season, setSeason] = useState<Season | null>(null);
  const [teams, setTeams] = useState<Team[]>([]);
  const [fixtures, setFixtures] = useState<LiveItem[]>([]);
  const [finishedFixtures, setFinishedFixtures] = useState<Fixture[]>([]);
  
  // Moderation State
  const [flags, setFlags] = useState<BetSuspicionFlag[]>([]);
  const [resolvedFlags, setResolvedFlags] = useState<BetSuspicionFlag[]>([]);
  const [modSubTab, setModSubTab] = useState<'ACTIVE' | 'RESOLVED'>('ACTIVE');
  const [resolvingFlagId, setResolvingFlagId] = useState<string | null>(null);
  const [unrestrictUser, setUnrestrictUser] = useState(true);
  const [msg, setMsg] = useState('');
  const [msgTone, setMsgTone] = useState<'success' | 'error'>('success');
  
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const l = await api.get<League[]>('/leagues');
        setLeagues(l);
        if (l.length > 0) selectLeague(l[0]);
        await loadFlags();
      } catch { }
      setLoading(false);
    }
    load();
  }, []);

  async function loadFlags() {
    try {
      const data = await api.get<BetSuspicionFlag[]>('/admin/suspicion-flags');
      setFlags(data);
      const resData = await api.get<BetSuspicionFlag[]>('/admin/suspicion-flags/resolved');
      setResolvedFlags(resData);
    } catch { }
  }

  async function selectLeague(lg: League) {
    setSelectedLeague(lg);
    try {
      const seasons = await api.get<Season[]>(`/seasons/league/${lg.id}`);
      const active = seasons.find(s => s.active);
      setSeason(active || null);
      if (active) {
        const t = await api.get<Team[]>(`/teams/season/${active.id}`);
        setTeams(t);
        const allFeed = await api.get<LiveItem[]>('/fixtures/feed/admin');
        setFixtures(allFeed);
        const finished = await api.get<Fixture[]>(`/fixtures/season/${active.id}/status/FINISHED`);
        setFinishedFixtures(finished);
      } else {
        setTeams([]);
        setFixtures([]);
        setFinishedFixtures([]);
      }
    } catch { }
  }

  async function handleResolveFlag(e: React.FormEvent) {
    e.preventDefault();
    if (!resolvingFlagId) return;
    try {
      await api.post(`/admin/suspicion-flags/${resolvingFlagId}/resolve`, { unrestrictUser });
      setMsg('Flag resolved successfully.');
      setMsgTone('success');
      setResolvingFlagId(null);
      await loadFlags();
    } catch (e: any) {
      setMsg(e.message || 'Failed to resolve flag');
      setMsgTone('error');
    }
  }

  if (loading) return <DashboardLayout><div className="feed-loading"><div className="loading-pulse" /><p>Loading platform...</p></div></DashboardLayout>;

  return (
    <DashboardLayout>
      <div style={{ marginBottom: '1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700 }}>Admin Control Panel</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Platform-wide overview of all leagues, seasons, and financial exposure.</p>
        </div>
      </div>

      {msg && <Toast message={msg} tone={msgTone} onClose={() => setMsg('')} />}

      {/* Main Tabs */}
      <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid var(--panel-border)', marginBottom: '1.5rem' }}>
        <button 
          onClick={() => setActiveTab('OVERVIEW')}
          style={{ ...tabBtnStyle, borderBottom: activeTab === 'OVERVIEW' ? '2px solid var(--accent)' : '2px solid transparent', color: activeTab === 'OVERVIEW' ? 'var(--accent)' : 'var(--text-muted)' }}>
          Platform Overview
        </button>
        <button 
          onClick={() => setActiveTab('MODERATION')}
          style={{ ...tabBtnStyle, borderBottom: activeTab === 'MODERATION' ? '2px solid var(--accent)' : '2px solid transparent', color: activeTab === 'MODERATION' ? 'var(--accent)' : 'var(--text-muted)' }}>
          Moderation & Flags {flags.length > 0 && <span style={{ backgroundColor: 'var(--danger)', color: 'white', padding: '0.125rem 0.375rem', borderRadius: '1rem', fontSize: '0.75rem', marginLeft: '0.5rem' }}>{flags.length}</span>}
        </button>
      </div>

      {activeTab === 'OVERVIEW' && (
        <>
          {/* League Selector */}
          <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
            {leagues.map(lg => (
              <button key={lg.id}
                onClick={() => selectLeague(lg)}
                style={{
                  padding: '0.5rem 1rem', borderRadius: '0.375rem', cursor: 'pointer', fontFamily: 'inherit', fontSize: '0.875rem',
                  backgroundColor: selectedLeague?.id === lg.id ? 'var(--accent)' : 'var(--panel)',
                  color: selectedLeague?.id === lg.id ? 'white' : 'var(--text-muted)',
                  border: `1px solid ${selectedLeague?.id === lg.id ? 'var(--accent)' : 'var(--panel-border)'}`,
                }}>
                {lg.name}
              </button>
            ))}
          </div>

          {selectedLeague && (
            <div className="fixture-card" style={{ marginBottom: '1.5rem', flexDirection: 'column', alignItems: 'flex-start', gap: '0.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
                <div>
                  <h3 style={{ fontWeight: 700 }}>{selectedLeague.name}</h3>
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.8125rem' }}>Created by: {selectedLeague.createdByUsername} · {selectedLeague.seasonCount} season(s)</p>
                </div>
                {season && <span style={{ color: 'var(--success)', fontWeight: 600, fontSize: '0.875rem' }}>Active: {season.name}</span>}
              </div>
            </div>
          )}

          {/* Teams */}
          {teams.length > 0 && (
            <section style={{ marginBottom: '2rem' }}>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '0.75rem' }}>Teams ({teams.length})</h3>
              <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                {teams.map(t => (
                  <div key={t.id} style={{ backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)', borderRadius: '0.5rem', padding: '0.75rem 1.25rem', display: 'flex', gap: '1rem', alignItems: 'center' }}>
                    <span style={{ fontWeight: 600 }}>{t.name}</span>
                    <span style={{ color: 'var(--accent)', fontSize: '0.8125rem', fontWeight: 600 }}>Elo {t.strength}</span>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Fixtures with financial exposure */}
          <section>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '0.75rem' }}>Fixtures & Financial Exposure</h3>
            {fixtures.length === 0 && <p style={{ color: 'var(--text-muted)' }}>No active fixtures.</p>}
            <div className="fixture-grid">
              {fixtures.map(({ fixture: f, odds }) => (
                <div key={f.id} className={`fixture-card ${['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.status) ? 'live' : ''}`} style={{ flexDirection: 'column', alignItems: 'stretch', gap: '1rem' }}>
                  {/* Match Header */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div className="fixture-teams">
                      <span>{f.homeTeamName}</span><span className="fixture-vs">v</span><span>{f.awayTeamName}</span>
                    </div>
                    <span style={{ color: ['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.status) ? '#22c55e' : 'var(--text-muted)', fontWeight: 600, fontSize: '0.75rem' }}>
                      {['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.status) ? `● LIVE ${f.matchMinute}'` : f.status}
                    </span>
                  </div>

                  {f.matchResult && (
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, textAlign: 'center' }}>
                      {f.matchResult.homeScore} - {f.matchResult.awayScore}
                    </div>
                  )}

                  {/* Odds Row */}
                  {odds && (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.5rem', fontSize: '0.8125rem' }}>
                      <div style={oddsCell}>1: {odds.homeWinOdds.toFixed(2)}</div>
                      <div style={oddsCell}>X: {odds.drawOdds.toFixed(2)}</div>
                      <div style={oddsCell}>2: {odds.awayWinOdds.toFixed(2)}</div>
                    </div>
                  )}

                  {/* Financial Exposure — Admin Only */}
                  {odds && (odds as FixtureOddsAdmin).totalHomeStake !== undefined && (
                    <div style={{ borderTop: '1px solid var(--panel-border)', paddingTop: '0.75rem' }}>
                      <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.5rem', fontWeight: 600 }}>💰 STAKE POOLS (Admin-Only)</p>
                      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.5rem', fontSize: '0.8125rem' }}>
                        <div style={stakeCell}>
                          <span style={{ color: 'var(--text-muted)' }}>Home</span>
                          <span style={{ fontWeight: 700, color: 'var(--success)' }}>£{((odds as FixtureOddsAdmin).totalHomeStake || 0).toLocaleString()}</span>
                        </div>
                        <div style={stakeCell}>
                          <span style={{ color: 'var(--text-muted)' }}>Draw</span>
                          <span style={{ fontWeight: 700, color: 'var(--success)' }}>£{((odds as FixtureOddsAdmin).totalDrawStake || 0).toLocaleString()}</span>
                        </div>
                        <div style={stakeCell}>
                          <span style={{ color: 'var(--text-muted)' }}>Away</span>
                          <span style={{ fontWeight: 700, color: 'var(--success)' }}>£{((odds as FixtureOddsAdmin).totalAwayStake || 0).toLocaleString()}</span>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </section>

          {/* Finished Fixtures & Realized Profit */}
          <section style={{ marginTop: '2rem' }}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '0.75rem' }}>Finished Fixtures & Realized Platform Profit</h3>
            {finishedFixtures.length === 0 && <p style={{ color: 'var(--text-muted)' }}>No finished fixtures yet.</p>}
            <div className="fixture-grid">
              {finishedFixtures.map((f) => (
                <div key={f.id} className="fixture-card" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '1rem', border: '1px solid var(--panel-border)' }}>
                  {/* Match Header */}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div className="fixture-teams">
                      <span>{f.homeTeamName}</span><span className="fixture-vs">v</span><span>{f.awayTeamName}</span>
                    </div>
                    <span style={{ color: 'var(--text-muted)', fontWeight: 600, fontSize: '0.75rem' }}>
                      FINISHED
                    </span>
                  </div>

                  {f.matchResult && (
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, textAlign: 'center' }}>
                      {f.matchResult.homeScore} - {f.matchResult.awayScore}
                    </div>
                  )}

                  {/* Realized Profit */}
                  <div style={{ borderTop: '1px solid var(--panel-border)', paddingTop: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>PLATFORM NET</span>
                    <span style={{ 
                      fontWeight: 700, 
                      color: (f.platformProfit || 0) >= 0 ? 'var(--success)' : 'var(--danger)' 
                    }}>
                      £{(f.platformProfit || 0).toLocaleString()}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </>
      )}

      {activeTab === 'MODERATION' && (
        <section>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div style={{ display: 'flex', gap: '1rem' }}>
              <button 
                onClick={() => setModSubTab('ACTIVE')}
                style={{ ...tabBtnStyle, borderBottom: modSubTab === 'ACTIVE' ? '2px solid var(--accent)' : '2px solid transparent', color: modSubTab === 'ACTIVE' ? 'var(--accent)' : 'var(--text-muted)' }}>
                Active Flags
              </button>
              <button 
                onClick={() => setModSubTab('RESOLVED')}
                style={{ ...tabBtnStyle, borderBottom: modSubTab === 'RESOLVED' ? '2px solid var(--accent)' : '2px solid transparent', color: modSubTab === 'RESOLVED' ? 'var(--accent)' : 'var(--text-muted)' }}>
                Resolved Flags
              </button>
            </div>
            <button className="btn" onClick={loadFlags}>↻ Refresh</button>
          </div>
          
          {modSubTab === 'ACTIVE' ? (
            flags.length === 0 ? (
              <div className="feed-empty">
                <h3>No Active Flags</h3>
                <p>The automated monitoring system has not flagged any recent betting activity.</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {flags.map(flag => (
                  <div key={flag.id} style={{ 
                    backgroundColor: 'var(--panel)', border: '1px solid var(--danger)', 
                    borderRadius: '0.5rem', padding: '1.25rem',
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem'
                  }}>
                    <div>
                      <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', marginBottom: '0.5rem' }}>
                        <span style={{ backgroundColor: 'var(--danger)', color: 'white', padding: '0.125rem 0.5rem', borderRadius: '0.25rem', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase' }}>
                          {flag.reason.replace(/_/g, ' ')}
                        </span>
                        <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>{new Date(flag.createdAt).toLocaleString()}</span>
                      </div>
                      <div style={{ fontSize: '0.9375rem', marginBottom: '0.25rem' }}>
                        <strong>User:</strong> {flag.username}
                      </div>
                      <div style={{ fontSize: '0.9375rem' }}>
                        <strong>Details:</strong> {flag.details}
                      </div>
                      {flag.betId && (
                        <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.5rem', fontFamily: 'monospace' }}>
                          Bet ID: {flag.betId}
                        </div>
                      )}
                    </div>
                    <div>
                      <button 
                        className="btn btn-primary" 
                        onClick={() => { setResolvingFlagId(flag.id); setUnrestrictUser(true); }}
                        style={{ backgroundColor: 'var(--danger)', color: 'white', border: 'none' }}>
                        Review & Resolve
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )
          ) : (
            resolvedFlags.length === 0 ? (
              <div className="feed-empty">
                <h3>No Resolved Flags</h3>
                <p>There is no history of resolved flags.</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {resolvedFlags.map(flag => (
                  <div key={flag.id} style={{ 
                    backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)', 
                    borderRadius: '0.5rem', padding: '1.25rem',
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem'
                  }}>
                    <div>
                      <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', marginBottom: '0.5rem' }}>
                        <span style={{ backgroundColor: 'var(--success)', color: 'white', padding: '0.125rem 0.5rem', borderRadius: '0.25rem', fontSize: '0.75rem', fontWeight: 600, textTransform: 'uppercase' }}>
                          RESOLVED: {flag.reason.replace(/_/g, ' ')}
                        </span>
                        <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>Created: {new Date(flag.createdAt).toLocaleString()}</span>
                      </div>
                      <div style={{ fontSize: '0.9375rem', marginBottom: '0.25rem' }}>
                        <strong>User:</strong> {flag.username}
                      </div>
                      <div style={{ fontSize: '0.9375rem' }}>
                        <strong>Details:</strong> {flag.details}
                      </div>
                    </div>
                    <div>
                      <button 
                        className="btn btn-primary" 
                        onClick={() => { setResolvingFlagId(flag.id); setUnrestrictUser(true); }}
                        style={{ backgroundColor: 'var(--success)', color: 'white', border: 'none' }}>
                        Review & Unrestrict
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )
          )}
        </section>
      )}

      {/* Resolution Modal */}
      <Modal open={Boolean(resolvingFlagId)} title="Resolve Suspicion Flag" onClose={() => setResolvingFlagId(null)}>
        <form onSubmit={handleResolveFlag} style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div>
            <p style={{ marginBottom: '1rem', color: 'var(--text-muted)' }}>
              Resolving this flag will mark it as reviewed. If the user's account was automatically restricted from betting by the engine, you can choose to unrestrict them here.
            </p>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', padding: '1rem', backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)', borderRadius: '0.5rem' }}>
              <input 
                type="checkbox" 
                checked={unrestrictUser} 
                onChange={(e) => setUnrestrictUser(e.target.checked)} 
                style={{ width: '1.25rem', height: '1.25rem', accentColor: 'var(--accent)' }}
              />
              <span style={{ fontWeight: 600 }}>Unrestrict User Account</span>
            </label>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '0.5rem', marginLeft: '1.75rem' }}>
              Check this box to lift the betting ban and allow the user to place bets again.
            </p>
          </div>
          
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" style={{ flex: 1, backgroundColor: 'var(--success)', border: 'none', color: 'white' }}>Confirm Resolution</button>
            <button type="button" className="btn" style={{ flex: 1 }} onClick={() => setResolvingFlagId(null)}>Cancel</button>
          </div>
        </form>
      </Modal>

    </DashboardLayout>
  );
}

const oddsCell: React.CSSProperties = { textAlign: 'center', backgroundColor: 'var(--background)', padding: '0.5rem', borderRadius: '0.375rem', border: '1px solid var(--panel-border)' };
const stakeCell: React.CSSProperties = { display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.25rem', backgroundColor: 'var(--background)', padding: '0.5rem', borderRadius: '0.375rem', border: '1px solid var(--panel-border)' };
const tabBtnStyle: React.CSSProperties = { background: 'none', border: 'none', padding: '0.5rem 0', fontWeight: 600, fontSize: '1rem', cursor: 'pointer', fontFamily: 'inherit' };
