import { useEffect, useState } from 'react';
import { DashboardLayout } from '../layouts/DashboardLayout';
import { api } from '../services/apiClient';
import { Modal } from '../components/ui/Modal';
import { Toast } from '../components/ui/Toast';
import '../components/betting/betting.css';

interface League { id: string; name: string; description: string; createdByUsername: string; }
interface Season { id: string; name: string; active: boolean; startDate: string; }
interface Team { id: string; name: string; strength: number; }
interface FixtureOdds {
  homeWinOdds: number; drawOdds: number; awayWinOdds: number;
  over15Odds: number; under15Odds: number; over25Odds: number; under25Odds: number;
  over35Odds: number; under35Odds: number; bttsYesOdds: number; bttsNoOdds: number;
}
interface Fixture {
  id: string; homeTeamName: string; awayTeamName: string; matchMinute: number;
  status: string; scheduledAt: string; platformProfit?: number;
  matchResult?: { homeScore: number; awayScore: number; isFinal: boolean; };
}
interface LiveItem { fixture: Fixture; odds: FixtureOdds | null; }

type Tab = 'fixtures' | 'teams' | 'season' | 'add-team' | 'add-fixture' | 'add-season';

export function LeagueAdminDashboard() {
  const [league, setLeague] = useState<League | null>(null);
  const [season, setSeason] = useState<Season | null>(null);
  const [teams, setTeams] = useState<Team[]>([]);
  const [fixtures, setFixtures] = useState<LiveItem[]>([]);
  const [finishedFixtures, setFinishedFixtures] = useState<Fixture[]>([]);
  const [tab, setTab] = useState<Tab>('fixtures');
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState('');

  // Form states
  const [teamName, setTeamName] = useState('');
  const [teamStrength, setTeamStrength] = useState(550);
  const [homeTeamId, setHomeTeamId] = useState('');
  const [awayTeamId, setAwayTeamId] = useState('');
  const [fixtureDate, setFixtureDate] = useState('');
  const [seasonName, setSeasonName] = useState('');
  const [seasonStart, setSeasonStart] = useState('');
  const [seasonEnd, setSeasonEnd] = useState('');

  const [scoreFixtureId, setScoreFixtureId] = useState<string | null>(null);
  const [homeScore, setHomeScore] = useState(0);
  const [awayScore, setAwayScore] = useState(0);
  const [matchMinute, setMatchMinute] = useState(0);

  // Additional time
  const [additionalTimeFixtureId, setAdditionalTimeFixtureId] = useState<string | null>(null);
  const [additionalTimeMins, setAdditionalTimeMins] = useState(2);

  useEffect(() => { loadData(); }, []);

  async function loadData() {
    setLoading(true);
    try {
      const leagues = await api.get<League[]>('/leagues/my');
      if (leagues.length === 0) { setLoading(false); return; }
      const myLeague = leagues[0]; // 1-to-1 relationship
      setLeague(myLeague);

      const seasons = await api.get<Season[]>(`/seasons/league/${myLeague.id}`);
      const activeSeason = seasons.find(s => s.active);
      if (activeSeason) {
        setSeason(activeSeason);
        const t = await api.get<Team[]>(`/teams/season/${activeSeason.id}`);
        setTeams(t);
        const feed = await api.get<LiveItem[]>(`/fixtures/feed/season/${activeSeason.id}`);
        setFixtures(feed);
        const finished = await api.get<Fixture[]>(`/fixtures/season/${activeSeason.id}/status/FINISHED`);
        setFinishedFixtures(finished);
      }
    } catch (e: any) { setMsg(e.message); }
    setLoading(false);
  }

  async function handleAddTeam(e: React.FormEvent) {
    e.preventDefault();
    if (!season) return;
    try {
      await api.post('/teams', { seasonId: season.id, name: teamName, strength: teamStrength });
      setMsg('Team added!'); setTeamName(''); setTeamStrength(550);
      loadData();
    } catch (e: any) { setMsg(e.message); }
  }

  async function handleAddFixture(e: React.FormEvent) {
    e.preventDefault();
    if (!season) return;
    try {
      await api.post('/fixtures', { seasonId: season.id, homeTeamId, awayTeamId, scheduledAt: fixtureDate });
      setMsg('Fixture created!'); setHomeTeamId(''); setAwayTeamId(''); setFixtureDate('');
      loadData();
    } catch (e: any) { setMsg(e.message); }
  }

  async function handleAddSeason(e: React.FormEvent) {
    e.preventDefault();
    if (!league) return;
    try {
      await api.post('/seasons', { leagueId: league.id, name: seasonName, startDate: seasonStart, endDate: seasonEnd || null });
      setMsg('Season created!'); setSeasonName(''); setSeasonStart(''); setSeasonEnd('');
      loadData();
    } catch (e: any) { setMsg(e.message); }
  }

  async function handleUpdateScore(e: React.FormEvent) {
    e.preventDefault();
    if (!scoreFixtureId) return;
    try {
      await api.put(`/fixtures/${scoreFixtureId}/score`, { homeScore, awayScore, matchMinute });
      setMsg('Score updated!'); setScoreFixtureId(null);
      loadData();
    } catch (e: any) { setMsg(e.message); }
  }

  async function handleFinish(fixtureId: string) {
    try {
      await api.post(`/fixtures/${fixtureId}/finish`, {});
      setMsg('Fixture finished!');
      loadData();
    } catch (e: any) { setMsg(e.message); }
  }

  async function handleSetAdditionalTime(e: React.FormEvent) {
    e.preventDefault();
    if (!additionalTimeFixtureId) return;
    try {
      await api.post(`/fixtures/${additionalTimeFixtureId}/additional-time?minutes=${additionalTimeMins}`, {});
      setMsg(`Additional time set to ${additionalTimeMins} mins`);
      setAdditionalTimeFixtureId(null);
      loadData();
    } catch (e: any) { setMsg(e.message); }
  }

  if (loading) return <DashboardLayout><div className="feed-loading"><div className="loading-pulse" /><p>Loading league...</p></div></DashboardLayout>;

  if (!league) {
    return (
      <DashboardLayout>
        <div className="feed-empty">
          <h3>No League Found</h3>
          <p>You don't have a league yet. Create one to get started.</p>
          <form onSubmit={async (e) => { e.preventDefault(); const name = (e.target as any).lname.value; const desc = (e.target as any).ldesc.value; try { await api.post('/leagues', { name, description: desc }); setMsg('League created!'); loadData(); } catch (err: any) { setMsg(err.message); } }} style={formStyle}>
            <input name="lname" placeholder="League Name" required style={inputStyle} />
            <input name="ldesc" placeholder="Description (optional)" style={inputStyle} />
            <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem' }}>Create League</button>
          </form>
        </div>
        {msg && <div style={msgStyle}>{msg}</div>}
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div style={{ marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 700 }}>{league.name}</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
          {season ? `Active Season: ${season.name}` : 'No active season — create one below'}
        </p>
      </div>

      {msg && <Toast message={msg} tone="info" onClose={() => setMsg('')} />}

      {/* Tab navigation */}
      <div style={tabBarStyle}>
        <button style={tab === 'fixtures' ? activeTabStyle : tabStyle} onClick={() => setTab('fixtures')}>Fixtures</button>
        <button style={tab === 'teams' ? activeTabStyle : tabStyle} onClick={() => setTab('teams')}>Teams</button>
        <button style={tab === 'add-fixture' ? activeTabStyle : tabStyle} onClick={() => setTab('add-fixture')}>+ Fixture</button>
        <button style={tab === 'add-team' ? activeTabStyle : tabStyle} onClick={() => setTab('add-team')}>+ Team</button>
        <button style={tab === 'add-season' ? activeTabStyle : tabStyle} onClick={() => setTab('add-season')}>+ Season</button>
      </div>

      {/* Fixtures Tab */}
      {tab === 'fixtures' && (
        <>
        <div className="fixture-grid">
          {fixtures.length === 0 && <p style={{ color: 'var(--text-muted)' }}>No fixtures yet.</p>}
          {fixtures.map(({ fixture: f, odds }) => (
            <div key={f.id} className={`fixture-card ${['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.status) ? 'live' : ''}`}>
              <div className="fixture-info" style={{ flex: 1 }}>
                <div className="fixture-teams">
                  <span>{f.homeTeamName}</span><span className="fixture-vs">v</span><span>{f.awayTeamName}</span>
                </div>
                <div className="fixture-meta">
                  <span style={{ color: ['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.status) ? '#22c55e' : 'var(--text-muted)', fontWeight: 600, fontSize: '0.75rem' }}>
                    {['OPEN', 'HALF_TIME', 'AWAITING_EXTRA_TIME', 'ADDITIONAL_TIME', 'LOCKED'].includes(f.status) ? `● LIVE ${f.matchMinute}'` : f.status}
                  </span>
                  {f.status === 'ADDITIONAL_TIME' && (
                    <span style={{ color: 'var(--accent)', fontWeight: 600, fontSize: '0.75rem', marginLeft: '0.5rem' }}>
                      Additional Time Running
                    </span>
                  )}
                  {f.matchResult && <span className="fixture-score">{f.matchResult.homeScore} - {f.matchResult.awayScore}</span>}
                </div>
                {odds && (
                  <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.5rem', fontSize: '0.8125rem', color: 'var(--text-muted)' }}>
                    <span>1: {odds.homeWinOdds.toFixed(2)}</span>
                    <span>X: {odds.drawOdds.toFixed(2)}</span>
                    <span>2: {odds.awayWinOdds.toFixed(2)}</span>
                    <span>|</span>
                    <span>O2.5: {odds.over25Odds.toFixed(2)}</span>
                    <span>U2.5: {odds.under25Odds.toFixed(2)}</span>
                    <span>|</span>
                    <span>BTTS Y: {odds.bttsYesOdds.toFixed(2)}</span>
                  </div>
                )}
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                {f.status === 'OPEN' && (
                  <>
                    <button className="btn btn-primary" style={{ fontSize: '0.75rem', padding: '0.375rem 0.75rem' }}
                      onClick={() => { setScoreFixtureId(f.id); setHomeScore(f.matchResult?.homeScore || 0); setAwayScore(f.matchResult?.awayScore || 0); setMatchMinute(f.matchMinute); }}>
                      Update Score
                    </button>
                    <button className="btn" style={{ fontSize: '0.75rem', padding: '0.375rem 0.75rem', backgroundColor: 'var(--danger)', color: 'white' }}
                      onClick={() => handleFinish(f.id)}>
                      Finish
                    </button>
                  </>
                )}
                {f.status === 'HALF_TIME' && (
                  <button className="btn btn-primary" style={{ fontSize: '0.75rem', padding: '0.375rem 0.75rem', backgroundColor: 'var(--success)' }}
                    onClick={async () => {
                      try {
                        await api.post(`/fixtures/${f.id}/resume-half-time`, {});
                        setMsg('Match resumed from half-time');
                        loadData();
                      } catch (e: any) { setMsg(e.message); }
                    }}>
                    Resume Match
                  </button>
                )}
                {f.status === 'AWAITING_EXTRA_TIME' && (
                  <button className="btn btn-primary" style={{ fontSize: '0.75rem', padding: '0.375rem 0.75rem', backgroundColor: 'var(--accent)' }}
                    onClick={() => {
                      setAdditionalTimeFixtureId(f.id);
                      setAdditionalTimeMins(2);
                    }}>
                    Set Additional Time
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
        
        {/* Finished Fixtures */}
        <div className="fixture-grid" style={{ marginTop: '2rem' }}>
          <h3 style={{ gridColumn: '1 / -1', fontSize: '1.125rem', fontWeight: 600, marginBottom: '0.75rem' }}>Finished Fixtures</h3>
          {finishedFixtures.length === 0 && <p style={{ color: 'var(--text-muted)' }}>No finished fixtures yet.</p>}
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
            </div>
          ))}
        </div>
        </>
      )}

      {/* Score Update Modal */}
      <Modal open={Boolean(scoreFixtureId)} title="Update score" onClose={() => setScoreFixtureId(null)}>
          <div>
            <h3 style={{ marginBottom: '1rem' }}>Update Score</h3>
            <form onSubmit={handleUpdateScore} style={formStyle}>
              <div style={{ display: 'flex', gap: '1rem' }}>
                <label style={{ flex: 1 }}>Home<input type="number" min={0} value={homeScore} onChange={e => setHomeScore(+e.target.value)} style={inputStyle} /></label>
                <label style={{ flex: 1 }}>Away<input type="number" min={0} value={awayScore} onChange={e => setAwayScore(+e.target.value)} style={inputStyle} /></label>
              </div>
              <label>Match Minute<input type="number" min={0} value={matchMinute} onChange={e => setMatchMinute(+e.target.value)} style={inputStyle} /></label>
              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Save</button>
                <button type="button" className="btn" style={{ flex: 1, backgroundColor: 'var(--panel-border)', color: 'var(--foreground)' }} onClick={() => setScoreFixtureId(null)}>Cancel</button>
              </div>
            </form>
          </div>
      </Modal>

      {/* Additional Time Modal */}
      <Modal open={Boolean(additionalTimeFixtureId)} title="Set Additional Time" onClose={() => setAdditionalTimeFixtureId(null)}>
          <div>
            <h3 style={{ marginBottom: '1rem' }}>Set Additional Time</h3>
            <form onSubmit={handleSetAdditionalTime} style={formStyle}>
              <label>Additional Minutes
                <input type="number" min={1} max={15} value={additionalTimeMins} onChange={e => setAdditionalTimeMins(+e.target.value)} style={inputStyle} required />
              </label>
              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Set Time</button>
                <button type="button" className="btn" style={{ flex: 1, backgroundColor: 'var(--panel-border)', color: 'var(--foreground)' }} onClick={() => setAdditionalTimeFixtureId(null)}>Cancel</button>
              </div>
            </form>
          </div>
      </Modal>

      {/* Teams Tab */}
      {tab === 'teams' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {teams.length === 0 && <p style={{ color: 'var(--text-muted)' }}>No teams in this season.</p>}
          {teams.map(t => (
            <div key={t.id} className="fixture-card" style={{ padding: '1rem' }}>
              <span style={{ fontWeight: 600 }}>{t.name}</span>
              <span style={{ color: 'var(--accent)', fontWeight: 600 }}>Elo: {t.strength}</span>
            </div>
          ))}
        </div>
      )}

      {/* Add Team Tab */}
      {tab === 'add-team' && season && (
        <form onSubmit={handleAddTeam} style={formStyle}>
          <h3>Add Team to {season.name}</h3>
          <input placeholder="Team Name" value={teamName} onChange={e => setTeamName(e.target.value)} required style={inputStyle} />
          <label>Elo Strength (100–1000)
            <input type="number" min={100} max={1000} value={teamStrength} onChange={e => setTeamStrength(+e.target.value)} required style={inputStyle} />
          </label>
          <button type="submit" className="btn btn-primary">Add Team</button>
        </form>
      )}

      {/* Add Fixture Tab */}
      {tab === 'add-fixture' && season && (
        <form onSubmit={handleAddFixture} style={formStyle}>
          <h3>Schedule a Fixture</h3>
          <label>Home Team
            <select value={homeTeamId} onChange={e => setHomeTeamId(e.target.value)} required style={inputStyle}>
              <option value="">Select home team</option>
              {teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </label>
          <label>Away Team
            <select value={awayTeamId} onChange={e => setAwayTeamId(e.target.value)} required style={inputStyle}>
              <option value="">Select away team</option>
              {teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </label>
          <label>Scheduled At
            <input type="datetime-local" value={fixtureDate} onChange={e => setFixtureDate(e.target.value)} required style={inputStyle} />
          </label>
          <button type="submit" className="btn btn-primary">Create Fixture</button>
        </form>
      )}

      {/* Add Season Tab */}
      {tab === 'add-season' && (
        <form onSubmit={handleAddSeason} style={formStyle}>
          <h3>Create New Season</h3>
          <input placeholder='Season Name (e.g. "2026-27")' value={seasonName} onChange={e => setSeasonName(e.target.value)} required style={inputStyle} />
          <label>Start Date<input type="date" value={seasonStart} onChange={e => setSeasonStart(e.target.value)} required style={inputStyle} /></label>
          <label>End Date (optional)<input type="date" value={seasonEnd} onChange={e => setSeasonEnd(e.target.value)} style={inputStyle} /></label>
          <button type="submit" className="btn btn-primary">Create Season</button>
        </form>
      )}
    </DashboardLayout>
  );
}

// Inline styles
const formStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: '1rem', maxWidth: '480px' };
const inputStyle: React.CSSProperties = { width: '100%', padding: '0.75rem 1rem', backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)', borderRadius: '0.375rem', color: 'var(--foreground)', fontFamily: 'inherit', marginTop: '0.25rem' };
const msgStyle: React.CSSProperties = { padding: '0.75rem 1rem', backgroundColor: 'rgba(14, 165, 233, 0.1)', color: 'var(--accent)', borderRadius: '0.5rem', fontSize: '0.875rem', marginBottom: '1rem', display: 'flex', alignItems: 'center' };
const tabBarStyle: React.CSSProperties = { display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', flexWrap: 'wrap' };
const tabStyle: React.CSSProperties = { padding: '0.5rem 1rem', backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)', borderRadius: '0.375rem', color: 'var(--text-muted)', cursor: 'pointer', fontFamily: 'inherit', fontSize: '0.875rem' };
const activeTabStyle: React.CSSProperties = { ...tabStyle, backgroundColor: 'var(--accent)', color: 'white', borderColor: 'var(--accent)' };
