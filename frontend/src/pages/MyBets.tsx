import { useEffect, useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { api } from '../services/apiClient';
import { Modal } from '../components/ui/Modal';
import { Toast } from '../components/ui/Toast';
import { Skeleton } from '../components/ui/Skeleton';
import '../components/betting/betting.css';

interface BetResponse {
  id: string;
  fixtureId: string;
  homeTeamName: string;
  awayTeamName: string;
  betType: string;
  outcome: string;
  oddsAtPlacement: number;
  stake: number;
  potentialPayout: number;
  status: string;
  cashedOut: boolean;
  cashoutAmount: number;
  createdAt: string;
  settledAt: string | null;
  insured?: boolean;
  insurancePremium?: number;
  insuranceRefundPercentage?: number;
}

export function MyBets() {
  const [bets, setBets] = useState<BetResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [msgTone, setMsgTone] = useState<'success' | 'error' | 'info'>('info');

  // Cashout state
  const [cashoutBetId, setCashoutBetId] = useState<string | null>(null);
  const [cashoutValue, setCashoutValue] = useState<number | null>(null);
  const [cashoutLoading, setCashoutLoading] = useState(false);
  const [cashoutExecuting, setCashoutExecuting] = useState(false);

  // Auto-Cashout modal
  const [autoCashoutBetId, setAutoCashoutBetId] = useState<string | null>(null);
  const [profitTarget, setProfitTarget] = useState('');
  const [lossLimit, setLossLimit] = useState('');

  // Insurance modal
  const [insuranceBetId, setInsuranceBetId] = useState<string | null>(null);
  const [insuranceExecuting, setInsuranceExecuting] = useState(false);
  const [coveragePercentage, setCoveragePercentage] = useState(25);

  useEffect(() => {
    loadBets();
  }, []);

  async function loadBets() {
    try {
      const data = await api.get<BetResponse[]>('/bets');
      const sorted = data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      setBets(sorted);
    } catch (err: any) {
      setError(err.message || 'Failed to load betting history.');
    } finally {
      setLoading(false);
    }
  }

  /* ── Cashout Handlers ───── */

  async function handleCalculateCashout(betId: string) {
    setCashoutBetId(betId);
    setCashoutValue(null);
    setCashoutLoading(true);
    try {
      const data = await api.get<{ cashoutValue: number }>(`/bets/${betId}/cashout-value`);
      setCashoutValue(data.cashoutValue);
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
      setCashoutBetId(null);
    } finally {
      setCashoutLoading(false);
    }
  }

  async function handleExecuteCashout() {
    if (!cashoutBetId) return;
    setCashoutExecuting(true);
    try {
      const data = await api.post<{ cashoutAmount: number }>(`/bets/${cashoutBetId}/cashout`, {});
      setMsg(`Bet cashed out for £${Number(data.cashoutAmount).toFixed(2)}!`);
      setMsgTone('success');
      setCashoutBetId(null);
      setCashoutValue(null);
      loadBets();
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    } finally {
      setCashoutExecuting(false);
    }
  }

  /* ── Auto-Cashout Handler ───── */

  async function handleSetAutoCashout(e: React.FormEvent) {
    e.preventDefault();
    if (!autoCashoutBetId) return;
    if (!profitTarget && !lossLimit) {
      setMsg('Set at least one of profit target or loss limit.');
      setMsgTone('error');
      return;
    }
    try {
      await api.post('/auto-cashout-rules', {
        betId: autoCashoutBetId,
        profitTarget: profitTarget ? parseFloat(profitTarget) : null,
        lossLimit: lossLimit ? parseFloat(lossLimit) : null,
      });
      setMsg('Auto-cashout rule created!');
      setMsgTone('success');
      setAutoCashoutBetId(null);
      setProfitTarget('');
      setLossLimit('');
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    }
  }

  /* ── Insurance Handler ───── */

  async function handleBuyInsurance() {
    if (!insuranceBetId) return;
    setInsuranceExecuting(true);
    try {
      await api.post(`/bets/${insuranceBetId}/insure`, { coveragePercentage });
      setMsg('Insurance purchased successfully!');
      setMsgTone('success');
      setInsuranceBetId(null);
      loadBets();
    } catch (e: any) {
      setMsg(e.message);
      setMsgTone('error');
    } finally {
      setInsuranceExecuting(false);
    }
  }

  /* ── Styles ───── */

  const inputStyle: React.CSSProperties = {
    width: '100%', padding: '0.75rem', marginTop: '0.25rem',
    backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)',
    borderRadius: '0.375rem', color: 'var(--foreground)', fontFamily: 'inherit',
  };

  const actionBtn: React.CSSProperties = {
    fontSize: '0.75rem', padding: '0.3rem 0.625rem', borderRadius: '0.375rem',
    border: 'none', cursor: 'pointer', fontFamily: 'inherit', fontWeight: 600,
    transition: 'all 0.2s',
  };

  return (
    <DashboardLayout>
      <div className="feed-section-header">
        <span className="feed-section-dot ongoing"></span>
        <h2>My Bets</h2>
        <span className="feed-section-count">{bets.length}</span>
      </div>

      {msg && <Toast message={msg} tone={msgTone} onClose={() => setMsg('')} />}

      {loading && (
        <div className="feed-loading">
          <div style={{ width: '100%', display: 'grid', gap: '0.75rem' }}>
            <Skeleton height={60} />
            <Skeleton height={60} />
            <Skeleton height={60} />
          </div>
          <p>Loading history...</p>
        </div>
      )}

      {error && (
        <div style={{ padding: '1rem', color: 'var(--danger)', textAlign: 'center' }}>
          {error}
        </div>
      )}

      {!loading && !error && bets.length === 0 && (
        <div className="feed-empty">
          <h3>No Bets Placed</h3>
          <p>You haven't placed any bets yet. Head to the dashboard to find a fixture!</p>
        </div>
      )}

      {!loading && !error && bets.length > 0 && (
        <table className="bets-table" aria-label="Betting history">
          <thead>
            <tr>
              <th>Fixture</th>
              <th>Selection</th>
              <th>Status</th>
              <th>Returns</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {bets.map(bet => {
            const statusColor = bet.status === 'WON' ? 'var(--success)' :
                                bet.status === 'LOST' ? 'var(--danger)' :
                                bet.status === 'CASHED_OUT' ? 'var(--primary)' : 'var(--warning)';

            const isPending = bet.status === 'PENDING';

            return (
              <tr key={bet.id}>
                <td>
                  <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>
                    {new Date(bet.createdAt).toLocaleString()}
                  </div>
                  <div style={{ fontWeight: 600, fontSize: '1.125rem' }}>
                    {bet.homeTeamName} vs {bet.awayTeamName}
                  </div>
                </td>
                <td>
                  {bet.outcome.replace(/_/g, ' ')} @ <strong>{bet.oddsAtPlacement.toFixed(2)}</strong>
                </td>
                <td>
                  <div style={{ fontWeight: 600, color: statusColor, marginBottom: '0.5rem' }}>
                    ● {bet.status}
                  </div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                    Stake: £{bet.stake.toFixed(2)}
                  </div>
                </td>
                <td>
                  <div style={{ fontWeight: 600 }}>
                    {bet.cashedOut 
                      ? `Cashed Out: £${bet.cashoutAmount?.toFixed(2) || '0.00'}`
                      : bet.status === 'WON' 
                        ? `Won: £${bet.potentialPayout.toFixed(2)}`
                        : `Pot. Returns: £${bet.potentialPayout.toFixed(2)}`}
                  </div>
                </td>
                <td>
                  {isPending && !bet.cashedOut && (
                    <div style={{ display: 'flex', gap: '0.375rem', flexDirection: 'column' }}>
                      <button
                        style={{ ...actionBtn, backgroundColor: 'var(--accent)', color: 'white' }}
                        onClick={() => handleCalculateCashout(bet.id)}>
                        💰 Cash Out
                      </button>
                      {bet.insured ? (
                        <div style={{ padding: '0.25rem', textAlign: 'center', backgroundColor: 'rgba(16, 185, 129, 0.1)', borderRadius: '0.375rem', border: '1px solid var(--success)' }}>
                          <div style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--success)' }}>🛡️ INSURED</div>
                          <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Refund up to: £{((bet.stake * (bet.insuranceRefundPercentage || 0)) / 100).toFixed(2)}</div>
                        </div>
                      ) : (
                        <button
                          style={{ ...actionBtn, backgroundColor: 'rgba(16, 185, 129, 0.2)', color: 'var(--success)' }}
                          onClick={() => { setInsuranceBetId(bet.id); setCoveragePercentage(25); }}>
                          🛡️ Insure Bet
                        </button>
                      )}
                      <button
                        style={{ ...actionBtn, backgroundColor: 'rgba(251, 191, 36, 0.2)', color: 'var(--warning)' }}
                        onClick={() => { setAutoCashoutBetId(bet.id); setProfitTarget(''); setLossLimit(''); }}>
                        ⚡ Auto-Cashout
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            );
          })}
          </tbody>
        </table>
      )}

      {/* ═══ Manual Cashout Modal ═══ */}
      <Modal open={Boolean(cashoutBetId)} title="Cash out bet" onClose={() => { setCashoutBetId(null); setCashoutValue(null); }}>
        <h3 style={{ marginBottom: '1rem' }}>Cash Out</h3>
        {cashoutLoading && (
          <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
            Calculating cashout value...
          </div>
        )}
        {!cashoutLoading && cashoutValue !== null && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{
              textAlign: 'center', padding: '1.5rem',
              backgroundColor: 'var(--background)', borderRadius: '0.5rem',
              border: '1px solid var(--panel-border)',
            }}>
              <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>Cashout Value</div>
              <div style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--success)' }}>
                £{Number(cashoutValue).toFixed(2)}
              </div>
            </div>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                className="btn btn-primary" style={{ flex: 1 }}
                disabled={cashoutExecuting}
                onClick={handleExecuteCashout}>
                {cashoutExecuting ? 'Processing...' : 'Confirm Cash Out'}
              </button>
              <button className="btn" style={{ flex: 1, backgroundColor: 'var(--panel-border)', color: 'var(--foreground)' }}
                onClick={() => { setCashoutBetId(null); setCashoutValue(null); }}>
                Cancel
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* ═══ Auto-Cashout Modal ═══ */}
      <Modal open={Boolean(autoCashoutBetId)} title="Set auto-cashout" onClose={() => setAutoCashoutBetId(null)}>
        <h3 style={{ marginBottom: '1rem' }}>Set Auto-Cashout Rule</h3>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
          Set at least one: a profit target (auto-cashout when profit hits this amount) or a loss limit (auto-cashout when loss reaches this amount).
        </p>
        <form onSubmit={handleSetAutoCashout} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <label style={{ fontSize: '0.875rem' }}>
            Profit Target (£) — optional
            <input type="number" min="0.01" step="0.01" value={profitTarget}
              onChange={e => setProfitTarget(e.target.value)}
              placeholder="e.g. 20.00" style={inputStyle} />
          </label>
          <label style={{ fontSize: '0.875rem' }}>
            Loss Limit (£) — optional
            <input type="number" min="0.01" step="0.01" value={lossLimit}
              onChange={e => setLossLimit(e.target.value)}
              placeholder="e.g. 10.00" style={inputStyle} />
          </label>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Set Rule</button>
            <button type="button" className="btn" style={{ flex: 1, backgroundColor: 'var(--panel-border)', color: 'var(--foreground)' }}
              onClick={() => setAutoCashoutBetId(null)}>Cancel</button>
          </div>
        </form>
      </Modal>

      {/* ═══ Insurance Modal ═══ */}
      <Modal open={Boolean(insuranceBetId)} title="Insure your bet" onClose={() => setInsuranceBetId(null)}>
        {(() => {
          const betToInsure = bets.find(b => b.id === insuranceBetId);
          if (!betToInsure) return null;
          
          const oddsWithMargin = betToInsure.oddsAtPlacement * 1.05;
          const probWin = 1 / oddsWithMargin;
          const probLose = 1 - probWin;
          
          const c = coveragePercentage / 100;
          const minEdge = 1.03;
          const maxEdge = 1.30;
          const dynamicEdge = minEdge + (c * (maxEdge - minEdge));
          
          const expectedPayout = betToInsure.stake * c * probLose;
          const calculatedPremium = expectedPayout * dynamicEdge;
          const calculatedMaxRefund = betToInsure.stake * c;
          
          const premiumExceedsRefund = calculatedPremium >= calculatedMaxRefund;

          return (
            <>
              <h3 style={{ marginBottom: '1rem' }}>Buy Bet Insurance</h3>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '1.5rem', lineHeight: 1.5 }}>
                Insure your bet to protect your stake! Drag the slider to choose how much of your stake you want refunded if you lose. 
              </p>
              
              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', fontWeight: 600 }}>
                  <span>Coverage Percentage</span>
                  <span style={{ color: 'var(--primary)' }}>{coveragePercentage}%</span>
                </label>
                <input 
                  type="range" 
                  min="10" 
                  max="100" 
                  step="5" 
                  value={coveragePercentage} 
                  onChange={(e) => setCoveragePercentage(Number(e.target.value))}
                  style={{ width: '100%', cursor: 'pointer' }}
                />
              </div>

              {premiumExceedsRefund && (
                <div style={{ padding: '0.5rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)', borderRadius: '0.25rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
                  Coverage too high! Premium exceeds potential refund at this mathematical risk level.
                </div>
              )}
              
              <div style={{ backgroundColor: 'var(--background)', padding: '1rem', borderRadius: '0.5rem', border: '1px solid var(--panel-border)', marginBottom: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Original Stake:</span>
                  <span style={{ fontWeight: 600 }}>£{betToInsure.stake.toFixed(2)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', borderBottom: '1px dashed var(--panel-border)', paddingBottom: '0.5rem' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Premium to deduct now:</span>
                  <span style={{ fontWeight: 700, color: 'var(--danger)' }}>-£{calculatedPremium.toFixed(2)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: '0.5rem' }}>
                  <span style={{ color: 'var(--text-muted)' }}>Refund if you lose:</span>
                  <span style={{ fontWeight: 700, color: 'var(--success)' }}>£{calculatedMaxRefund.toFixed(2)}</span>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button
                  className="btn btn-primary" style={{ flex: 1, backgroundColor: 'var(--success)' }}
                  disabled={insuranceExecuting || premiumExceedsRefund}
                  onClick={handleBuyInsurance}>
                  {insuranceExecuting ? 'Processing...' : `Pay £${calculatedPremium.toFixed(2)} Premium`}
                </button>
                <button className="btn" style={{ flex: 1, backgroundColor: 'var(--panel-border)', color: 'var(--foreground)' }}
                  onClick={() => setInsuranceBetId(null)}>
                  Cancel
                </button>
              </div>
            </>
          );
        })()}
      </Modal>
    </DashboardLayout>
  );
}
