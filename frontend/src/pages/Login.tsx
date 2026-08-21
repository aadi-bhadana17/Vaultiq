import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';

export function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || 'Invalid username or password');
      }

      // Vaultiq Backend returns ApiResponse<AuthResponse> 
      // Structure: { success: true, data: { token: 'ey...', username: '...', role: '...' } }
      if (data.data?.token) {
        localStorage.setItem('vaultiq_token', data.data.token);
        localStorage.setItem('vaultiq_role', data.data.role);
      }
      
      // Route to the correct dashboard based on role
      const role = data.data?.role;
      if (role === 'ADMIN') {
        navigate('/admin');
      } else if (role === 'LEAGUE_ADMIN') {
        navigate('/league-admin');
      } else {
        navigate('/dashboard');
      }
    } catch (err: any) {
      setError(err.message || 'An error occurred while connecting to the server');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card glass-panel slide-up">
        <div className="auth-header">
          <h2>Welcome to Vaultiq</h2>
          <p>Enter your credentials to access the exchange.</p>
        </div>
        
        <form className="auth-form" onSubmit={handleLogin}>
          {error && (
            <div style={{ padding: '0.75rem', backgroundColor: 'rgba(244, 63, 94, 0.1)', color: 'var(--danger)', borderRadius: '0.375rem', fontSize: '0.875rem', textAlign: 'center' }}>
              {error}
            </div>
          )}

          <div className="auth-fields">
            <Input 
              label="Username" 
              type="text" 
              placeholder="vaultiq_user" 
              required 
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
            <Input 
              label="Password" 
              type="password" 
              placeholder="••••••••" 
              required 
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <div style={{ paddingTop: '0.5rem' }}>
            <Button type="submit" fullWidth disabled={isLoading}>
              {isLoading ? 'Authenticating...' : 'Sign In'}
            </Button>
          </div>
        </form>

        <div className="auth-footer">
          Don't have an account?{' '}
          <Link to="/signup">
            Sign up
          </Link>
        </div>
      </div>
    </div>
  );
}
