import { useEffect, useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { api } from '../services/apiClient';
import { Skeleton } from '../components/ui/Skeleton';
import '../components/betting/betting.css';

interface TransactionResponse {
  id: string;
  type: string;
  amount: number;
  balanceAfter: number;
  referenceId: string;
  referenceType: string;
  description: string;
  createdAt: string;
}

export function Transactions() {
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadTransactions();
  }, []);

  async function loadTransactions() {
    try {
      // The API uses pagination, we'll fetch the first 50 for now
      const data = await api.get<TransactionResponse[]>('/wallet/transactions?page=0&size=50');
      setTransactions(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load transactions.');
    } finally {
      setLoading(false);
    }
  }

  // Format the transaction type to be more readable
  const formatType = (type: string) => {
    return type.replace(/_/g, ' ');
  };

  // Determine if it's an income or expense
  const isIncome = (type: string) => {
    return ['DEPOSIT', 'BET_WON', 'BET_CASHED_OUT', 'INSURANCE_REFUND'].includes(type);
  };

  return (
    <DashboardLayout>
      <div className="feed-section-header">
        <span className="feed-section-dot ongoing"></span>
        <h2>Transactions</h2>
      </div>

      {loading && (
        <div className="feed-loading">
          <div style={{ width: '100%', display: 'grid', gap: '0.75rem' }}>
            <Skeleton height={60} />
            <Skeleton height={60} />
            <Skeleton height={60} />
          </div>
          <p>Loading transactions...</p>
        </div>
      )}

      {error && (
        <div style={{ padding: '1rem', color: 'var(--danger)', textAlign: 'center' }}>
          {error}
        </div>
      )}

      {!loading && !error && transactions.length === 0 && (
        <div className="feed-empty">
          <h3>No Transactions</h3>
          <p>You haven't made any transactions yet.</p>
        </div>
      )}

      {!loading && !error && transactions.length > 0 && (
        <table className="bets-table" aria-label="Transaction history">
          <thead>
            <tr>
              <th>Date</th>
              <th>Description</th>
              <th>Type</th>
              <th>Amount</th>
              <th>Balance</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map(txn => {
              const income = isIncome(txn.type);
              return (
                <tr key={txn.id}>
                  <td>
                    <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
                      {new Date(txn.createdAt).toLocaleString()}
                    </div>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{txn.description}</div>
                    {txn.referenceId && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                        Ref: {txn.referenceId.substring(0, 8)}... ({txn.referenceType})
                      </div>
                    )}
                  </td>
                  <td>
                    <span style={{ 
                      fontSize: '0.75rem', 
                      padding: '0.25rem 0.5rem', 
                      borderRadius: '1rem', 
                      backgroundColor: 'var(--panel-border)',
                      fontWeight: 600
                    }}>
                      {formatType(txn.type)}
                    </span>
                  </td>
                  <td>
                    <div style={{ fontWeight: 700, color: income ? 'var(--success)' : 'var(--foreground)' }}>
                      {income ? '+' : '-'}£{txn.amount.toFixed(2)}
                    </div>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600, color: 'var(--text-muted)' }}>
                      £{txn.balanceAfter.toFixed(2)}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </DashboardLayout>
  );
}
