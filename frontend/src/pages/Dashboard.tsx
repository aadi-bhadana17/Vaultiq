import { useEffect, useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { FixtureCard } from '../components/betting/FixtureCard';
import type { LiveFixture } from '../components/betting/FixtureCard';
import { AccessibleDropdown } from '../components/ui/AccessibleDropdown';
import { Skeleton } from '../components/ui/Skeleton';
import { api } from '../services/apiClient';

export function Dashboard() {
  const [fixtures, setFixtures] = useState<LiveFixture[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'OPEN' | 'SCHEDULED' | 'LOCKED'>('ALL');

  useEffect(() => {
    let active = true;

    async function fetchFeed() {
      try {
        const data = await api.get<LiveFixture[]>('/fixtures/feed');
        if (active) {
          setFixtures(data);
          setError('');
        }
      } catch (err: any) {
        if (active) setError(err.message || 'Failed to load fixtures');
      } finally {
        if (active) setLoading(false);
      }
    }

    fetchFeed();

    // Poll every 15 seconds for live odds updates
    const interval = setInterval(fetchFeed, 15000);

    return () => {
      active = false;
      clearInterval(interval);
    };
  }, []);

  const filtered = statusFilter === 'ALL' ? fixtures : fixtures.filter((f) => f.fixture.status === statusFilter);
  const liveFixtures = filtered.filter(f => ['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.fixture.status));
  const upcomingFixtures = filtered.filter(f => f.fixture.status === 'SCHEDULED');
  const lockedFixtures = filtered.filter(f => f.fixture.status === 'LOCKED');

  return (
    <DashboardLayout>
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <AccessibleDropdown
          label="Filter fixtures"
          value={statusFilter}
          options={[
            { label: 'All Fixtures', value: 'ALL' },
            { label: 'Live', value: 'OPEN' },
            { label: 'Locked', value: 'LOCKED' },
            { label: 'Upcoming', value: 'SCHEDULED' },
          ]}
          onChange={(value) => setStatusFilter(value as 'ALL' | 'OPEN' | 'SCHEDULED' | 'LOCKED')}
        />
      </div>
      {loading && (
        <div className="feed-loading">
          <div style={{ width: '100%', maxWidth: 840, display: 'grid', gap: '0.75rem' }}>
            <Skeleton height={88} />
            <Skeleton height={88} />
            <Skeleton height={88} />
          </div>
          <p>Loading live markets...</p>
        </div>
      )}

      {error && (
        <div style={{ padding: '1rem', backgroundColor: 'rgba(244, 63, 94, 0.1)', color: 'var(--danger)', borderRadius: '0.5rem', textAlign: 'center' }}>
          {error}
        </div>
      )}

      {!loading && !error && fixtures.length === 0 && (
        <div className="feed-empty">
          <h3>No Active Fixtures</h3>
          <p>There are no upcoming or live fixtures at the moment. Check back later!</p>
        </div>
      )}

      {liveFixtures.length > 0 && (
        <section>
          <div className="feed-section-header">
            <span className="feed-section-dot live" />
            <h2>Live Now</h2>
            <span className="feed-section-count">{liveFixtures.length}</span>
          </div>
          <div className="fixture-grid">
            {liveFixtures.map(f => (
              <FixtureCard key={f.fixture.id} data={f} />
            ))}
          </div>
        </section>
      )}

      {lockedFixtures.length > 0 && (
        <section>
          <div className="feed-section-header">
            <span className="feed-section-dot locked" />
            <h2>In-Play (Locked)</h2>
            <span className="feed-section-count">{lockedFixtures.length}</span>
          </div>
          <div className="fixture-grid">
            {lockedFixtures.map(f => (
              <FixtureCard key={f.fixture.id} data={f} />
            ))}
          </div>
        </section>
      )}

      {upcomingFixtures.length > 0 && (
        <section>
          <div className="feed-section-header">
            <span className="feed-section-dot upcoming" />
            <h2>Upcoming</h2>
            <span className="feed-section-count">{upcomingFixtures.length}</span>
          </div>
          <div className="fixture-grid">
            {upcomingFixtures.map(f => (
              <FixtureCard key={f.fixture.id} data={f} />
            ))}
          </div>
        </section>
      )}
    </DashboardLayout>
  );
}
