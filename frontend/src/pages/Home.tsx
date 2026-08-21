import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FixtureCard } from '../components/betting/FixtureCard';
import type { LiveFixture } from '../components/betting/FixtureCard';
import { api } from '../services/apiClient';
import '../layouts/DashboardLayout.css';
import '../components/betting/betting.css';

export function Home() {
  const [fixtures, setFixtures] = useState<LiveFixture[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function fetchFeed() {
      try {
        const data = await api.get<LiveFixture[]>('/fixtures/feed');
        if (active) setFixtures(data);
      } catch {
        // Silently fail on public page — user doesn't need error noise
      } finally {
        if (active) setLoading(false);
      }
    }

    fetchFeed();
    const interval = setInterval(fetchFeed, 20000);
    return () => { active = false; clearInterval(interval); };
  }, []);

  return (
    <div className="dashboard-layout">
      <nav className="top-nav">
        <div className="nav-brand">
          <Link to="/">Vaultiq</Link>
        </div>
        <div className="nav-actions">
          <Link to="/login" style={loginBtnStyle}>Login</Link>
          <Link to="/signup" style={signupBtnStyle}>Sign Up</Link>
        </div>
      </nav>

      <div className="dashboard-content" style={{ justifyContent: 'center' }}>
        <main className="main-feed" style={{ maxWidth: '800px' }}>
          <div style={{ marginBottom: '1rem', marginTop: '1rem' }}>
            <h2 style={{ fontSize: '1.5rem', fontWeight: 600 }}>Featured Match Markets</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
              Log in to place bets and access our automated tools.
            </p>
          </div>

          {loading && (
            <div className="feed-loading">
              <div className="loading-pulse" />
              <p>Loading markets...</p>
            </div>
          )}

          {!loading && fixtures.length === 0 && (
            <div className="feed-empty">
              <h3>No Active Markets</h3>
              <p>There are no fixtures available right now. Check back soon!</p>
            </div>
          )}

          {!loading && fixtures.length > 0 && (
            <div className="fixture-grid">
              {fixtures.map(f => (
                <FixtureCard key={f.fixture.id} data={f} />
              ))}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

const loginBtnStyle: React.CSSProperties = {
  padding: '8px 16px',
  color: 'var(--foreground)',
  textDecoration: 'none',
  fontWeight: 500,
};

const signupBtnStyle: React.CSSProperties = {
  padding: '8px 16px',
  backgroundColor: 'var(--accent)',
  color: 'white',
  textDecoration: 'none',
  borderRadius: '4px',
  fontWeight: 600,
};
