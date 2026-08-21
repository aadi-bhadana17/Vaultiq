import { useEffect, useState, useRef } from 'react';
import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BetSlip } from '../components/betting/BetSlip';
import { ThemeToggle } from '../components/ui/ThemeToggle';
import { AccessibleDropdown } from '../components/ui/AccessibleDropdown';
import { Modal } from '../components/ui/Modal';
import { Toast } from '../components/ui/Toast';
import { api } from '../services/apiClient';
import './DashboardLayout.css';

interface UserProfile {
  id: string;
  username: string;
  email: string;
  role: string;
  walletBalance: number;
  bettingRestricted: boolean;
}

interface BetStats {
  totalBets: number;
  winRate: number;
}

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

export function DashboardLayout({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [betStats, setBetStats] = useState<BetStats | null>(null);
  const [tipsterProfile, setTipsterProfile] = useState<TipsterProfile | null>(null);
  const [tipsterApplied, setTipsterApplied] = useState(false);
  
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [msg, setMsg] = useState('');
  const [msgTone, setMsgTone] = useState<'success' | 'error' | 'info'>('info');

  const menuRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    async function fetchProfile() {
      try {
        const profile = await api.get<UserProfile>('/auth/me');
        setUser(profile);
        
        if (profile.role === 'USER') {
          const stats = await api.get<BetStats>('/bets/stats');
          setBetStats(stats);
          
          try {
            const tipster = await api.get<TipsterProfile>(`/tipsters/${profile.id}`);
            setTipsterProfile(tipster);
            setTipsterApplied(true);
          } catch {
            setTipsterApplied(false);
          }
        }
      } catch {
        navigate('/login');
      }
    }
    fetchProfile();
  }, [navigate]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setShowProfileMenu(false);
      }
    }
    if (showProfileMenu) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showProfileMenu]);

  const initials = user ? user.username.slice(0, 2).toUpperCase() : '..';
  const balance = user ? `£${user.walletBalance.toLocaleString('en-GB', { minimumFractionDigits: 2 })}` : '—';
  const isUser = user?.role === 'USER';
  const isLeagueAdmin = user?.role === 'LEAGUE_ADMIN';
  const homeLink = isLeagueAdmin ? '/league-admin' : isUser ? '/dashboard' : '/admin';
  const roleBadge = isUser ? 'User' : isLeagueAdmin ? 'League Admin' : 'Admin';
  
  const [quickNav, setQuickNav] = useState(homeLink);

  const handleLogout = () => {
    localStorage.removeItem('vaultiq_token');
    localStorage.removeItem('vaultiq_role');
    navigate('/login');
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < 6) {
      setMsg('New password must be at least 6 characters');
      setMsgTone('error');
      return;
    }
    try {
      await api.post('/auth/change-password', { oldPassword, newPassword });
      setMsg('Password changed successfully. Please log in again.');
      setMsgTone('success');
      setShowPasswordModal(false);
      setTimeout(handleLogout, 2000);
    } catch (err: any) {
      setMsg(err.message || 'Failed to change password');
      setMsgTone('error');
    }
  };

  return (
    <div className="dashboard-layout">
      {msg && <Toast message={msg} tone={msgTone} onClose={() => setMsg('')} />}
      
      <nav className="top-nav">
        <div className="nav-brand">
          <Link to={homeLink}>Vaultiq</Link>
          <span className="role-badge">{roleBadge}</span>
        </div>
        <div style={{ display: 'flex', gap: '1.5rem', marginLeft: '2rem', flex: 1 }}>
          {isUser && <>
            <Link to="/dashboard" style={navLinkStyle}>Markets</Link>
            <Link to="/my-bets" style={navLinkStyle}>My Bets</Link>
            <Link to="/transactions" style={navLinkStyle}>Transactions</Link>
            <Link to="/tipsters" style={navLinkStyle}>Tipsters</Link>
            <Link to="/syndicates" style={navLinkStyle}>Syndicates</Link>
            <Link to="/copy-bets" style={navLinkStyle}>Copy Bets</Link>
            <Link to="/automation" style={navLinkStyle}>Automation</Link>
          </>}
        </div>
        <div className="nav-actions">
          <AccessibleDropdown
            label="Quick navigation"
            value={quickNav}
            options={[
              { label: 'Home', value: homeLink },
              ...(isUser ? [
                { label: 'Markets', value: '/dashboard' },
                { label: 'My Bets', value: '/my-bets' },
                { label: 'Tipsters', value: '/tipsters' },
                { label: 'Syndicates', value: '/syndicates' },
                { label: 'Copy Bets', value: '/copy-bets' },
                { label: 'Automation', value: '/automation' },
              ] : []),
              { label: 'Main Area', value: homeLink },
            ]}
            onChange={(value) => {
              setQuickNav(value);
              navigate(value);
            }}
          />
          <ThemeToggle />
          <div className="wallet-pill">
            <span className="wallet-label">Balance</span>
            <span className="wallet-amount">{balance}</span>
          </div>
          
          {/* Profile Menu Wrapper */}
          <div style={{ position: 'relative' }} ref={menuRef}>
            <div 
              className="profile-icon" 
              title={user?.username || ''}
              onClick={() => setShowProfileMenu(!showProfileMenu)}
              style={{ cursor: 'pointer' }}
            >
              {initials}
            </div>
            
            {showProfileMenu && user && (
              <div style={{
                position: 'absolute', top: '120%', right: 0, width: '280px',
                backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)',
                borderRadius: '0.5rem', boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
                zIndex: 50, padding: '1rem', display: 'flex', flexDirection: 'column', gap: '1rem'
              }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{user.username}</div>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>{user.email}</div>
                </div>
                
                {isUser && betStats && (
                  <div style={{ backgroundColor: 'var(--background)', padding: '0.75rem', borderRadius: '0.375rem', border: '1px solid var(--panel-border)' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem', textTransform: 'uppercase' }}>Betting Record</div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem' }}>
                      <span>Win Rate</span>
                      <span style={{ fontWeight: 600 }}>{betStats.winRate.toFixed(1)}%</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem' }}>
                      <span>Total Bets</span>
                      <span style={{ fontWeight: 600 }}>{betStats.totalBets}</span>
                    </div>
                  </div>
                )}
                
                {isUser && tipsterApplied && tipsterProfile && (
                  <div style={{ backgroundColor: 'var(--background)', padding: '0.75rem', borderRadius: '0.375rem', border: '1px solid var(--panel-border)' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.25rem', textTransform: 'uppercase' }}>Tipster Status</div>
                    
                    {tipsterProfile.eligible ? (
                      <>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem' }}>
                          <span>Credibility</span>
                          <span style={{ fontWeight: 600, color: 'var(--success)' }}>{tipsterProfile.credibilityScore.toFixed(2)}</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem' }}>
                          <span>Your Cut</span>
                          <span style={{ fontWeight: 600 }}>{tipsterProfile.cutPercentage.toFixed(1)}%</span>
                        </div>
                      </>
                    ) : (
                      <>
                        <div style={{ fontSize: '0.8125rem', color: 'var(--warning)', marginBottom: '0.5rem' }}>In Probation</div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem' }}>
                          <span>Bets</span>
                          <span style={{ fontWeight: 600 }}>{tipsterProfile.totalBets} / 10</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.875rem' }}>
                          <span>Win Rate</span>
                          <span style={{ fontWeight: 600 }}>{tipsterProfile.winRate.toFixed(1)}% / 40%</span>
                        </div>
                      </>
                    )}
                  </div>
                )}
                
                <div style={{ borderTop: '1px solid var(--panel-border)', margin: '0 -1rem' }}></div>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  <button 
                    className="btn" 
                    style={{ width: '100%', textAlign: 'left', backgroundColor: 'transparent', border: 'none', padding: '0.5rem' }}
                    onClick={() => { setShowProfileMenu(false); setShowPasswordModal(true); }}
                  >
                    Change Password
                  </button>
                  <button 
                    className="btn" 
                    style={{ width: '100%', textAlign: 'left', backgroundColor: 'transparent', border: 'none', color: 'var(--danger)', padding: '0.5rem' }}
                    onClick={handleLogout}
                  >
                    Logout
                  </button>
                </div>
              </div>
            )}
          </div>
          
        </div>
      </nav>
      
      <div className="dashboard-content">
        <main className="main-feed">
          {children}
        </main>
        {isUser && (
          <aside className="right-sidebar">
            <BetSlip />
          </aside>
        )}
      </div>

      <Modal open={showPasswordModal} title="Change Password" onClose={() => setShowPasswordModal(false)}>
        <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <label style={{ fontSize: '0.875rem' }}>
            Old Password
            <input 
              type="password" 
              value={oldPassword} 
              onChange={(e) => setOldPassword(e.target.value)} 
              required
              style={inputStyle} 
            />
          </label>
          <label style={{ fontSize: '0.875rem' }}>
            New Password
            <input 
              type="password" 
              value={newPassword} 
              onChange={(e) => setNewPassword(e.target.value)} 
              required
              minLength={6}
              style={inputStyle} 
            />
          </label>
          <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>Update Password</button>
            <button type="button" className="btn" style={{ flex: 1 }} onClick={() => setShowPasswordModal(false)}>Cancel</button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

const navLinkStyle: React.CSSProperties = {
  color: 'var(--foreground)',
  textDecoration: 'none',
  fontWeight: 500,
  fontSize: '0.875rem',
};

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '0.75rem', marginTop: '0.25rem',
  backgroundColor: 'var(--panel)', border: '1px solid var(--panel-border)',
  borderRadius: '0.375rem', color: 'var(--foreground)', fontFamily: 'inherit',
};
