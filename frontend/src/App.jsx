import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/Login';
import ReceptionistDashboard from './pages/ReceptionistDashboard';
import DoctorDashboard from './pages/DoctorDashboard';
import TvScreen from './pages/TvScreen';
import PatientsDirectory from './pages/PatientsDirectory';
import AnalyticsDashboard from './pages/AnalyticsDashboard';
import ClinicsManagement from './pages/ClinicsManagement';
import OrganizationManagement from './pages/OrganizationManagement';
import SuperAdminDashboard from './pages/SuperAdminDashboard';
import StaffManagement from './pages/StaffManagement';

const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-slate-500 font-bold">جاري التحميل...</p>
      </div>
    );
  }
  if (!user) return <Navigate to="/login" />;
  return children;
};

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/super-admin" element={<ProtectedRoute><SuperAdminDashboard /></ProtectedRoute>} />
          <Route path="/organization" element={<ProtectedRoute><OrganizationManagement /></ProtectedRoute>} />
          <Route path="/staff" element={<ProtectedRoute><StaffManagement /></ProtectedRoute>} />
          <Route path="/receptionist" element={<ProtectedRoute><ReceptionistDashboard /></ProtectedRoute>} />
          <Route path="/doctor" element={<ProtectedRoute><DoctorDashboard /></ProtectedRoute>} />
          <Route path="/patients" element={<ProtectedRoute><PatientsDirectory /></ProtectedRoute>} />
          <Route path="/analytics" element={<ProtectedRoute><AnalyticsDashboard /></ProtectedRoute>} />
          <Route path="/clinics" element={<ProtectedRoute><ClinicsManagement /></ProtectedRoute>} />
          <Route path="/tv" element={<TvScreen />} />
          <Route path="*" element={<Navigate to="/login" />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;