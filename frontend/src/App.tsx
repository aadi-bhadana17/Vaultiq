import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { Signup } from './pages/Signup';
import { Dashboard } from './pages/Dashboard';
import { LeagueAdminDashboard } from './pages/LeagueAdminDashboard';
import { AdminDashboard } from './pages/AdminDashboard';
import { BetSlipProvider } from './context/BetSlipContext';
import { ProtectedRoute } from './components/ProtectedRoute';

import { FixtureDetails } from './pages/FixtureDetails';
import { MyBets } from './pages/MyBets';
import { TipsterDirectory } from './pages/TipsterDirectory';
import { SyndicateHub } from './pages/SyndicateHub';
import { CopyBets } from './pages/CopyBets';
import { AutomationHub } from './pages/AutomationHub';
import { Transactions } from './pages/Transactions';

function App() {
  return (
    <BetSlipProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/my-bets" element={<MyBets />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/syndicates" element={<SyndicateHub />} />
          
          {/* Protected Area */}
          <Route path="/dashboard" element={
            <ProtectedRoute allowedRoles={['USER']}>
              <Dashboard />
            </ProtectedRoute>
          } />
          
          <Route path="/fixture/:id" element={
            <ProtectedRoute allowedRoles={['USER']}>
              <FixtureDetails />
            </ProtectedRoute>
          } />

          <Route path="/my-bets" element={
            <ProtectedRoute allowedRoles={['USER']}>
              <MyBets />
            </ProtectedRoute>
          } />

          <Route path="/tipsters" element={
            <ProtectedRoute allowedRoles={['USER']}>
              <TipsterDirectory />
            </ProtectedRoute>
          } />

          <Route path="/syndicates" element={
            <ProtectedRoute allowedRoles={['USER']}>
              <SyndicateHub />
            </ProtectedRoute>
          } />

          <Route path="/copy-bets" element={
            <ProtectedRoute allowedRoles={['USER']}>
              <CopyBets />
            </ProtectedRoute>
          } />
          
          <Route path="/automation" element={
            <ProtectedRoute allowedRoles={['USER']}>
              <AutomationHub />
            </ProtectedRoute>
          } />
          
          <Route path="/league-admin" element={
            <ProtectedRoute allowedRoles={['LEAGUE_ADMIN']}>
              <LeagueAdminDashboard />
            </ProtectedRoute>
          } />
          
          <Route path="/admin" element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminDashboard />
            </ProtectedRoute>
          } />
        </Routes>
      </BrowserRouter>
    </BetSlipProvider>
  );
}

export default App;
