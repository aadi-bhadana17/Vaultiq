import { useState } from 'react';
import { useBetSlip } from '../../context/BetSlipContext';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { api } from '../../services/apiClient';
import './betting.css';

export function BetSlip() {
  const {
    selections, removeSelection, stake, setStake,
    potentialPayout, totalStake, clearSlip,
    betMode, setBetMode, builderOdds,
  } = useBetSlip();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusMsg, setStatusMsg] = useState<{type: 'error' | 'success', text: string} | null>(null);

  if (selections.length === 0) {
    return (
      <div className="bet-slip empty">
        <div className="slip-header" style={{ width: '100%', borderBottom: 'none' }}>
          <h3>Bet Slip</h3>
        </div>
        <p>No bets selected.</p>
        <p style={{fontSize: '0.875rem'}}>Click on odds to add them to your slip.</p>
      </div>
    );
  }

  const canBuilder = selections.length >= 2;

  const handlePlaceBet = async () => {
    setStatusMsg(null);
    setIsSubmitting(true);

    try {
      if (betMode === 'BUILDER') {
        // Send as a single bet builder (accumulator)
        await api.post('/bet-builders', {
          legs: selections.map(sel => ({
            fixtureId: sel.fixtureId,
            outcome: sel.outcome,
          })),
          stake,
        });
        setStatusMsg({ type: 'success', text: 'Bet Builder placed successfully!' });
      } else {
        // Place each selection as an independent single bet
        let successCount = 0;
        for (const sel of selections) {
          await api.post('/bets', {
            fixtureId: sel.fixtureId,
            outcome: sel.outcome,
            stake: stake,
          });
          successCount++;
        }
        setStatusMsg({ type: 'success', text: `Successfully placed ${successCount} bet(s)!` });
      }

      // Delay clear so user can read message
      setTimeout(() => {
        clearSlip();
        setStatusMsg(null);
      }, 3000);
    } catch (err: any) {
      setStatusMsg({ type: 'error', text: err.message || 'Failed to place bet. Check balance.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const modeToggleStyle = (active: boolean): React.CSSProperties => ({
    flex: 1,
    padding: '0.375rem 0.5rem',
    borderRadius: '0.375rem',
    cursor: 'pointer',
    fontFamily: 'inherit',
    fontSize: '0.75rem',
    fontWeight: 600,
    border: 'none',
    transition: 'all 0.2s',
    backgroundColor: active ? 'var(--accent)' : 'transparent',
    color: active ? 'white' : 'var(--text-muted)',
  });

  return (
    <div className="bet-slip">
      <div className="slip-header">
        <h3>Bet Slip <span className="badge">{selections.length}</span></h3>
        <button className="clear-slip" onClick={clearSlip}>Clear</button>
      </div>

      {/* Mode Toggle */}
      <div style={{
        display: 'flex', gap: '0.25rem', padding: '0.25rem',
        backgroundColor: 'var(--panel)', borderRadius: '0.5rem',
        marginBottom: '0.75rem', border: '1px solid var(--panel-border)',
      }}>
        <button style={modeToggleStyle(betMode === 'SINGLES')} onClick={() => setBetMode('SINGLES')}>
          Singles
        </button>
        <button
          style={{
            ...modeToggleStyle(betMode === 'BUILDER'),
            ...(canBuilder ? {} : { opacity: 0.4, cursor: 'not-allowed' }),
          }}
          onClick={() => canBuilder && setBetMode('BUILDER')}
          title={canBuilder ? '' : 'Need at least 2 selections'}
        >
          Bet Builder
        </button>
      </div>

      <div className="slip-selections">
        {selections.map(sel => (
          <div key={sel.id} className="slip-item">
            <div className="slip-item-header">
              <span className="slip-outcome">
                {sel.outcome.replace(/_/g, ' ')}
              </span>
              <button className="remove-item" onClick={() => removeSelection(sel.id)}>×</button>
            </div>
            <div className="slip-item-match">
              {sel.homeTeam} vs {sel.awayTeam}
            </div>
            <div className="slip-item-odds">
              {sel.odds.toFixed(2)}
            </div>
          </div>
        ))}
      </div>

      <div className="slip-footer">
        {statusMsg && (
          <div className={`slip-status-msg ${statusMsg.type}`}>
            {statusMsg.text}
          </div>
        )}

        {betMode === 'BUILDER' && canBuilder && (
          <div className="slip-summary-row" style={{ color: 'var(--accent)', fontWeight: 700 }}>
            <span>Combined Odds</span>
            <span>{builderOdds.toFixed(2)}</span>
          </div>
        )}

        <div className="slip-summary-row">
          <span>{betMode === 'SINGLES' ? 'Stakes (Singles)' : 'Stake (Builder)'}</span>
          <span className="slip-total-odds">
            {betMode === 'SINGLES' ? `${selections.length}x` : '1x'}
          </span>
        </div>
        
        <div className="slip-stake-input">
          <Input 
            label={betMode === 'SINGLES' ? 'Stake Per Bet (£)' : 'Total Stake (£)'}
            type="number" 
            min="0"
            step="1"
            value={stake || ''}
            onChange={(e) => setStake(parseFloat(e.target.value) || 0)}
          />
        </div>

        <div className="slip-summary-row">
          <span>Total Cost</span>
          <span>£{totalStake.toFixed(2)}</span>
        </div>

        <div className="slip-summary-row returns">
          <span>Potential Returns</span>
          <span className="slip-payout">£{potentialPayout.toFixed(2)}</span>
        </div>

        <Button 
          fullWidth 
          onClick={handlePlaceBet} 
          disabled={stake <= 0 || isSubmitting || (betMode === 'BUILDER' && !canBuilder)}
        >
          {isSubmitting
            ? 'Processing...'
            : betMode === 'BUILDER'
              ? 'Place Bet Builder'
              : 'Place Bet'}
        </Button>
      </div>
    </div>
  );
}
