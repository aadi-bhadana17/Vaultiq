import { useEffect, useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { api } from '../services/apiClient';
import { Modal } from '../components/ui/Modal';
import { Toast } from '../components/ui/Toast';
import { Skeleton } from '../components/ui/Skeleton';
import '../components/betting/betting.css';

/* ── Types ─────────────────────────────────────────────── */

interface Team { id: string; name: string; strength: number; }
interface LiveFixture {
  fixture: { id: string; homeTeamName: string; awayTeamName: string; status: string; matchMinute: number };
  odds: any;
}

interface AutoBetRule {
  id: string;
  teamId: string;
  teamName: string;
  outcome: string;
  minOdds: number;
  stake: number;
  active: boolean;
  lastTriggeredAt: string | null;
  createdAt: string;
}

interface AutoCashoutRule {
  id: string;
  betId: string;
  profitTarget: number | null;
  lossLimit: number | null;
  active: boolean;
  triggeredAt: string | null;
  createdAt: string;
}

interface BetLimits {
  maxSingleBet: number;
  maxDailyTotal: number;
  riskScore: number;
  lastRecalculatedAt: string;
}

const OUTCOMES = [
  'HOME_WIN', 'DRAW', 'AWAY_WIN',
  'OVER_1_5', 'UNDER_1_5', 'OVER_2_5', 'UNDER_2_5', 'OVER_3_5', 'UNDER_3_5',
  'BTTS_YES', 'BTTS_NO',
];

type Tab = 'auto-bet' | 'auto-cashout' | 'limits';

/* ── Component ─────────────────────────────────────────── */

export function AutomationHub() {
  const [tab, setTab] = useState<Tab>('auto-bet');
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState('');
  const [msgTone, setMsgTone] = useState<'success' | 'error' | 'info'>('info');

  // Auto-Bet
  const [rules, setRules] = useState<AutoBetRule[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [fixtures, setFixtures] = useState<LiveFixture[]>([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [ruleTeamId, setRuleTeamId] = useState('');
  const [ruleOutcome, setRuleOutcome] = useState('HOME_WIN');
  const [ruleMinOdds, setRuleMinOdds] = useState('1.50');
  const [ruleStake, setRuleStake] = useState('10');

  // Auto-Cashout
  const [cashoutRules, setCashoutRules] = useState<AutoCashoutRule[]>([]);

  // Limits
  const [limits, setLimits] = useState<BetLimits | null>(null);

  useEffect(() => { loadData(); }, []);

  async function loadData() {
    setLoading(true);
    try {
      const [rulesData, cashoutData, feedData] = await Promise.all([
        api.get<AutoBetRule[]>('/auto-bet-rules'),
        api.get<AutoCashoutRule[]>('/auto-cashout-rules'),
        api.get<LiveFixture[]>('/fixtures/feed'),
      ]);
      setRules(rulesData);
      setCashoutRules(cashoutData);
      setFixtures(feedData);

      // Try loading teams from feed fixtures' seasons
      // We'll extract unique team info from fixtures
      try {
        const limitsData = await api.get<BetLimits>('/bet-limits');
        setLimits(limitsData);
      } catch {
        // limits might not exist yet for new users
      }

      // Load teams from the active seasons
      try {
        // Fallback: try to get teams from the fixtures themselves
        const teamMap = new Map<string, Team>();
        feedData.forEach(item => {
          const fix = item.fixture as any;
          if (fix.homeTeamId && fix.homeTeamName) {
            teamMap.set(fix.homeTeamId, { id: fix.homeTeamId, name: fix.homeTeamName, strength: 0 });
          }
          if (fix.awayTeamId && fix.awayTeamName) {
            teamMap.set(fix.awayTeamId, { id: fix.awayTeamId, name: fix.awayTeamName, strength: 0 });
          }
        });
        if (teamMap.size > 0) {
          setTeams(Array.from(teamMap.values()));
        }
      } catch {
        // silent
      }
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
    setLoading(false);
  }

  /* ── Auto-Bet Handlers ───── */

  async function handleCreateRule(e: React.FormEvent) {
    e.preventDefault();
    try {
      await api.post('/auto-bet-rules', {
        teamId: ruleTeamId,
        outcome: ruleOutcome,
        minOdds: parseFloat(ruleMinOdds),
        stake: parseFloat(ruleStake),
      });
      setMsg('Auto-bet rule created!');
      setMsgTone('success');
      setShowCreateModal(false);
      setRuleTeamId(''); setRuleMinOdds('1.50'); setRuleStake('10');
      loadData();
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  async function handleToggleRule(id: string, active: boolean) {
    try {
      await api.patch(`/auto-bet-rules/${id}/toggle?active=${active}`);
      setMsg(active ? 'Rule activated' : 'Rule deactivated');
      setMsgTone('success');
      setRules(prev => prev.map(r => r.id === id ? { ...r, active } : r));
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  async function handleDeleteRule(id: string) {
    if (!confirm('Delete this auto-bet rule?')) return;
    try {
      await api.delete(`/auto-bet-rules/${id}`);
      setMsg('Rule deleted');
      setMsgTone('success');
      setRules(prev => prev.filter(r => r.id !== id));
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  /* ── Auto-Cashout Handlers ───── */

  async function handleDeleteCashoutRule(id: string) {
    if (!confirm('Delete this auto-cashout rule?')) return;
    try {
      await api.delete(`/auto-cashout-rules/${id}`);
      setMsg('Auto-cashout rule deleted');
      setMsgTone('success');
      setCashoutRules(prev => prev.filter(r => r.id !== id));
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  /* ── Styles ───── */

  const inputStyle: React.CSSProperties = {
    width: '100%', padding: '0.75rem', marginTop: '0.25rem',
    backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)',
    borderRadius: '0.375rem', color: 'var(--foreground)', fontFamily: 'inherit',
  };

  const tabBtnStyle = (active: boolean): React.CSSProperties => ({
    padding: '0.5rem 1rem', borderRadius: '0.375rem', cursor: 'pointer',
    fontFamily: 'inherit', fontSize: '0.875rem', border: '1px solid var(--panel-border)',
    backgroundColor: active ? 'var(--accent)' : 'var(--panel)',
    color: active ? 'white' : 'var(--text-muted)',
    borderColor: active ? 'var(--accent)' : 'var(--panel-border)',
    transition: 'all 0.2s',
  });

  const toggleBtnStyle = (on: boolean): React.CSSProperties => ({
    padding: '0.25rem 0.75rem', borderRadius: '2rem', fontSize: '0.75rem',
    fontWeight: 600, border: 'none', cursor: 'pointer', fontFamily: 'inherit',
    transition: 'all 0.2s',
    backgroundColor: on ? 'rgba(74, 222, 128, 0.2)' : 'rgba(251, 113, 133, 0.2)',
    color: on ? 'var(--success)' : 'var(--danger)',
  });

  /* ── Render ───── */

  return (
    <DashboardLayout>
      <div className="feed-section-header">
        <span className="feed-section-dot live" />
        <h2>Automation Hub</h2>
      </div>

      {msg && <Toast message={msg} tone={msgTone} onClose={() => setMsg('')} />}

      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
        <button style={tabBtnStyle(tab === 'auto-bet')} onClick={() => setTab('auto-bet')}>
          Auto-Bet Rules ({rules.length})
        </button>
        <button style={tabBtnStyle(tab === 'auto-cashout')} onClick={() => setTab('auto-cashout')}>
          Auto-Cashout ({cashoutRules.length})
        </button>
        <button style={tabBtnStyle(tab === 'limits')} onClick={() => setTab('limits')}>
          My Limits
        </button>
      </div>

      {loading && (
        <div className="feed-loading"><Skeleton height={100} /><Skeleton height={100} /></div>
      )}

      {/* ═══ Auto-Bet Tab ═══ */}
      {!loading && tab === 'auto-bet' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', margin: 0 }}>
              Rules auto-place bets when a matching fixture opens with odds above your threshold.
            </p>
            <button className="btn btn-primary" style={{ fontSize: '0.8125rem', flexShrink: 0 }}
              onClick={() => setShowCreateModal(true)}>
              + New Rule
            </button>
          </div>

          {rules.length === 0 && (
            <div className="feed-empty">
              <h3>No Auto-Bet Rules</h3>
              <p>Create a rule to automatically place bets when odds hit your target.</p>
            </div>
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {rules.map(rule => (
              <div key={rule.id} className="fixture-card" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '0.75rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '1.0625rem' }}>
                      {rule.teamName || 'Team'}
                    </div>
                    <div style={{ display: 'flex', gap: '1rem', marginTop: '0.375rem', fontSize: '0.8125rem', color: 'var(--text-muted)', flexWrap: 'wrap' }}>
                      <span>Outcome: <strong style={{ color: 'var(--foreground)' }}>{rule.outcome.replace(/_/g, ' ')}</strong></span>
                      <span>Min Odds: <strong style={{ color: 'var(--foreground)' }}>{Number(rule.minOdds).toFixed(2)}</strong></span>
                      <span>Stake: <strong style={{ color: 'var(--foreground)' }}>£{Number(rule.stake).toFixed(2)}</strong></span>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <button style={toggleBtnStyle(rule.active)}
                      onClick={() => handleToggleRule(rule.id, !rule.active)}>
                      {rule.active ? '● Active' : '○ Paused'}
                    </button>
                    <button className="btn" style={{ fontSize: '0.75rem', padding: '0.25rem 0.625rem', backgroundColor: 'var(--danger)', color: 'white', border: 'none', borderRadius: '0.375rem', cursor: 'pointer' }}
                      onClick={() => handleDeleteRule(rule.id)}>
                      Delete
                    </button>
                  </div>
                </div>
                {rule.lastTriggeredAt && (
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                    Last triggered: {new Date(rule.lastTriggeredAt).toLocaleString()}
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Open Fixtures Preview */}
          {fixtures.filter(f => ['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED', 'SCHEDULED'].includes(f.fixture.status)).length > 0 && (
            <div style={{ marginTop: '2rem' }}>
              <h3 style={{ fontWeight: 600, marginBottom: '0.75rem', fontSize: '1.125rem' }}>Available Fixtures for Auto-Bet</h3>
              <div className="fixture-grid">
                {fixtures
                  .filter(f => ['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED', 'SCHEDULED'].includes(f.fixture.status))
                  .map(({ fixture: f, odds }) => (
                    <div key={f.id} className={`fixture-card ${['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.status) ? 'live' : ''}`} style={{ flexDirection: 'column', gap: '0.5rem' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div className="fixture-teams">
                          <span style={{ fontWeight: 600 }}>{f.homeTeamName}</span>
                          <span className="fixture-vs" style={{ margin: '0 0.375rem', color: 'var(--text-muted)' }}>v</span>
                          <span style={{ fontWeight: 600 }}>{f.awayTeamName}</span>
                        </div>
                        <span style={{
                          color: ['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.status) ? 'var(--success)' : 'var(--text-muted)',
                          fontWeight: 600, fontSize: '0.75rem'
                        }}>
                          {['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.status) ? `● LIVE ${f.matchMinute}'` : f.status}
                        </span>
                      </div>
                      {odds && (
                        <div style={{ display: 'flex', gap: '0.5rem', fontSize: '0.8125rem' }}>
                          <span style={oddsPill}>1: {Number(odds.homeWinOdds).toFixed(2)}</span>
                          <span style={oddsPill}>X: {Number(odds.drawOdds).toFixed(2)}</span>
                          <span style={oddsPill}>2: {Number(odds.awayWinOdds).toFixed(2)}</span>
                        </div>
                      )}
                    </div>
                  ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* ═══ Auto-Cashout Tab ═══ */}
      {!loading && tab === 'auto-cashout' && (
        <div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginBottom: '1rem' }}>
            Auto-cashout rules trigger when your live bet hits a profit target or loss limit. Set them from the <strong>My Bets</strong> page.
          </p>

          {cashoutRules.length === 0 && (
            <div className="feed-empty">
              <h3>No Auto-Cashout Rules</h3>
              <p>Visit "My Bets" and click "Set Auto-Cashout" on any open bet.</p>
            </div>
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {cashoutRules.map(rule => (
              <div key={rule.id} className="fixture-card" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '0.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.9375rem', marginBottom: '0.25rem' }}>
                      Bet: {rule.betId.slice(0, 8)}…
                    </div>
                    <div style={{ display: 'flex', gap: '1rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                      {rule.profitTarget != null && (
                        <span>Profit Target: <strong style={{ color: 'var(--success)' }}>£{Number(rule.profitTarget).toFixed(2)}</strong></span>
                      )}
                      {rule.lossLimit != null && (
                        <span>Loss Limit: <strong style={{ color: 'var(--danger)' }}>£{Number(rule.lossLimit).toFixed(2)}</strong></span>
                      )}
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <span style={{
                      fontSize: '0.75rem', fontWeight: 600, padding: '0.25rem 0.625rem', borderRadius: '0.25rem',
                      color: rule.triggeredAt ? 'var(--accent)' : rule.active ? 'var(--success)' : 'var(--text-muted)',
                      backgroundColor: rule.triggeredAt ? 'rgba(56, 189, 248, 0.15)' : rule.active ? 'rgba(74, 222, 128, 0.15)' : 'rgba(100,100,100,0.15)',
                    }}>
                      {rule.triggeredAt ? 'Triggered' : rule.active ? 'Active' : 'Inactive'}
                    </span>
                    {!rule.triggeredAt && (
                      <button className="btn" style={{ fontSize: '0.75rem', padding: '0.25rem 0.625rem', backgroundColor: 'var(--danger)', color: 'white', border: 'none', borderRadius: '0.375rem', cursor: 'pointer' }}
                        onClick={() => handleDeleteCashoutRule(rule.id)}>
                        Delete
                      </button>
                    )}
                  </div>
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Created: {new Date(rule.createdAt).toLocaleString()}
                  {rule.triggeredAt && ` · Triggered: ${new Date(rule.triggeredAt).toLocaleString()}`}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ═══ Limits Tab ═══ */}
      {!loading && tab === 'limits' && (
        <div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginBottom: '1rem' }}>
            Your dynamic bet limits are recalculated automatically based on your betting behaviour, account age, and risk score.
          </p>

          {!limits ? (
            <div className="feed-empty">
              <h3>No Limits Data</h3>
              <p>Bet limits haven't been calculated yet. They update automatically after your first bets.</p>
            </div>
          ) : (
            <div className="fixture-card" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '1.25rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
                <div style={statCard}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Max Single Bet</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--foreground)' }}>£{Number(limits.maxSingleBet).toFixed(2)}</div>
                </div>
                <div style={statCard}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Max Daily Total</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--foreground)' }}>£{Number(limits.maxDailyTotal).toFixed(2)}</div>
                </div>
                <div style={statCard}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Risk Score</div>
                  <div style={{
                    fontSize: '1.5rem', fontWeight: 700,
                    color: Number(limits.riskScore) > 60 ? 'var(--danger)' : Number(limits.riskScore) > 30 ? 'var(--warning)' : 'var(--success)',
                  }}>
                    {Number(limits.riskScore).toFixed(1)}
                  </div>
                </div>
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Last recalculated: {new Date(limits.lastRecalculatedAt).toLocaleString()}
              </div>
            </div>
          )}
        </div>
      )}

      {/* ═══ Create Rule Modal ═══ */}
      <Modal open={showCreateModal} title="Create auto-bet rule" onClose={() => setShowCreateModal(false)}>
        <h3 style={{ marginBottom: '1rem' }}>Create Auto-Bet Rule</h3>
        <form onSubmit={handleCreateRule} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <label style={{ fontSize: '0.875rem' }}>
            Team
            <select value={ruleTeamId} onChange={e => setRuleTeamId(e.target.value)} required style={inputStyle}>
              <option value="">Select a team</option>
              {teams.map(t => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
            {teams.length === 0 && (
              <input
                placeholder="Paste Team ID (no active fixtures found)"
                value={ruleTeamId}
                onChange={e => setRuleTeamId(e.target.value)}
                required
                style={{ ...inputStyle, marginTop: '0.5rem' }}
              />
            )}
          </label>
          <label style={{ fontSize: '0.875rem' }}>
            Outcome
            <select value={ruleOutcome} onChange={e => setRuleOutcome(e.target.value)} required style={inputStyle}>
              {OUTCOMES.map(o => (
                <option key={o} value={o}>{o.replace(/_/g, ' ')}</option>
              ))}
            </select>
          </label>
          <label style={{ fontSize: '0.875rem' }}>
            Minimum Odds
            <input type="number" min="1.01" step="0.01" value={ruleMinOdds}
              onChange={e => setRuleMinOdds(e.target.value)} required style={inputStyle} />
          </label>
          <label style={{ fontSize: '0.875rem' }}>
            Stake (£)
            <input type="number" min="0.01" step="0.01" value={ruleStake}
              onChange={e => setRuleStake(e.target.value)} required style={inputStyle} />
          </label>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Create Rule</button>
            <button type="button" className="btn" style={{ flex: 1, backgroundColor: 'var(--panel-border)', color: 'var(--foreground)' }}
              onClick={() => setShowCreateModal(false)}>Cancel</button>
          </div>
        </form>
      </Modal>
    </DashboardLayout>
  );
}

/* ── Shared Styles ───── */

const oddsPill: React.CSSProperties = {
  padding: '0.25rem 0.625rem', borderRadius: '0.25rem',
  backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)',
};

const statCard: React.CSSProperties = {
  textAlign: 'center', padding: '1rem',
  backgroundColor: 'var(--background)', borderRadius: '0.5rem',
  border: '1px solid var(--panel-border)',
};
