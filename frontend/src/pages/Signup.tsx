import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';

export function Signup() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleSignup = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    // Client-side validations
    if (password.length < 6) {
      setError('Password must be at least 6 characters long');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setIsLoading(true);

    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, email, password }),
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || 'Registration failed. Please check your inputs.');
      }

      // Vaultiq Backend returns ApiResponse<AuthResponse>
      // Structure: { success: true, data: { token: 'ey...', username: '...', role: '...' } }
      if (data.data?.token) {
        localStorage.setItem('vaultiq_token', data.data.token);
        localStorage.setItem('vaultiq_role', data.data.role);
      }

      // Redirect user to the dashboard (role is always USER for registration)
      navigate('/dashboard');
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
          <h2>Create an Account</h2>
          <p>Join the premium sports betting platform.</p>
        </div>
        
        <form className="auth-form" onSubmit={handleSignup}>
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
              label="Email Address" 
              type="email" 
              placeholder="name@example.com" 
              required 
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <Input 
              label="Password" 
              type="password" 
              placeholder="••••••••" 
              required 
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <Input 
              label="Confirm Password" 
              type="password" 
              placeholder="••••••••" 
              required 
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          <div style={{ paddingTop: '0.5rem' }}>
            <Button type="submit" fullWidth disabled={isLoading}>
              {isLoading ? 'Creating Account...' : 'Create Account'}
            </Button>
          </div>
        </form>

        <div className="auth-footer">
          Already have an account?{' '}
          <Link to="/login">
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
}
