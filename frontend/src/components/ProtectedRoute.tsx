import { Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';

interface ProtectedRouteProps {
  children: ReactNode;
  allowedRoles: string[];
}

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const token = localStorage.getItem('vaultiq_token');
  const role = localStorage.getItem('vaultiq_role');

  // 1. If not authenticated at all, redirect to login
  if (!token || !role) {
    return <Navigate to="/login" replace />;
  }

  // 2. If authenticated but has the wrong role for this specific URL
  if (!allowedRoles.includes(role)) {
    // Reroute them down to their proper designated path instantly
    if (role === 'ADMIN') {
      return <Navigate to="/admin" replace />;
    } else if (role === 'LEAGUE_ADMIN') {
      return <Navigate to="/league-admin" replace />;
    } else {
      return <Navigate to="/dashboard" replace />;
    }
  }

  // 3. User is authenticated and is legally allowed to view this component
  return <>{children}</>;
}
