import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { jwtDecode } from 'jwt-decode';
import axiosClient from '../api/axiosClient';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [clinicsData, setClinicsData] = useState([]);
  const [selectedClinicId, setSelectedClinicId] = useState('');
  const [selectedDoctorId, setSelectedDoctorId] = useState('');
  const [loading, setLoading] = useState(true);

  const parseToken = (token) => {
    const decoded = jwtDecode(token);
    return {
      token,
      username: decoded.sub,
      userId: decoded.userId,
      organizationId: decoded.organizationId,
      role: decoded.primaryRole,
      isOwner: Boolean(decoded.isOwner),
      roles: decoded.roles || [],
    };
  };

  const loadClinicsForUser = useCallback(async (parsedUser) => {
    if (!parsedUser || parsedUser.role === 'SUPER_ADMIN' || parsedUser.role === 'PATIENT') {
      setClinicsData([]);
      return;
    }
    try {
      const res = await axiosClient.get('/clinics/my');
      const items = res.data || [];
      setClinicsData(items);

      if (items.length > 0) {
        const savedClinic = Number(localStorage.getItem('selectedClinicId'));
        const matchedClinic = items.find((i) => i.clinic.id === savedClinic) || items[0];
        const clinicIdStr = String(matchedClinic.clinic.id);
        setSelectedClinicId(clinicIdStr);
        localStorage.setItem('selectedClinicId', clinicIdStr);

        if (parsedUser.role === 'DOCTOR' && !parsedUser.isOwner) {
          const docIdStr = String(parsedUser.userId);
          setSelectedDoctorId(docIdStr);
          localStorage.setItem('selectedDoctorId', docIdStr);
        } else {
          const savedDoc = Number(localStorage.getItem('selectedDoctorId'));
          const docs = matchedClinic.doctors || [];
          const matchedDoc = docs.find((d) => d.userId === savedDoc) || docs[0];
          const docIdStr = matchedDoc ? String(matchedDoc.userId) : '';
          setSelectedDoctorId(docIdStr);
          if (docIdStr) localStorage.setItem('selectedDoctorId', docIdStr);
        }
      } else {
        setSelectedClinicId('');
        setSelectedDoctorId('');
      }
    } catch (error) {
      console.error('Failed to load user clinics:', error);
    }
  }, []);

useEffect(() => {
  const init = async () => {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const decoded = jwtDecode(token);
        if (decoded.exp && decoded.exp * 1000 < Date.now()) {
          localStorage.removeItem('token');
          setLoading(false);
          return;
        }
        const parsed = parseToken(token);
        setUser(parsed);
        await loadClinicsForUser(parsed);
      } catch {
        localStorage.removeItem('token');
      }
    }
    setLoading(false);
  };
  init();
}, [loadClinicsForUser]);


  const login = async (username, password) => {
    const res = await axiosClient.post('/auth/login', { username, password });
    localStorage.setItem('token', res.data.token);
    const parsed = parseToken(res.data.token);
    setUser(parsed);
    await loadClinicsForUser(parsed);
    return parsed;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('selectedClinicId');
    localStorage.removeItem('selectedDoctorId');
    setUser(null);
    setClinicsData([]);
    setSelectedClinicId('');
    setSelectedDoctorId('');
  };

  const selectClinic = (clinicId) => {
    const idStr = String(clinicId);
    setSelectedClinicId(idStr);
    localStorage.setItem('selectedClinicId', idStr);

    const found = clinicsData.find((i) => String(i.clinic.id) === idStr);
    if (found) {
      if (user?.role === 'DOCTOR' && !user?.isOwner) {
        setSelectedDoctorId(String(user.userId));
      } else {
        const firstDoc = found.doctors?.[0];
        const docStr = firstDoc ? String(firstDoc.userId) : '';
        setSelectedDoctorId(docStr);
        if (docStr) localStorage.setItem('selectedDoctorId', docStr);
        else localStorage.removeItem('selectedDoctorId');
      }
    }
  };

  const selectDoctor = (doctorId) => {
    const idStr = String(doctorId);
    setSelectedDoctorId(idStr);
    localStorage.setItem('selectedDoctorId', idStr);
  };

  const refreshClinics = async () => {
    if (user) await loadClinicsForUser(user);
  };

  const currentClinicEntry = clinicsData.find((i) => String(i.clinic.id) === String(selectedClinicId));
  const availableDoctors = currentClinicEntry?.doctors || [];

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        logout,
        loading,
        clinicsData,
        currentClinic: currentClinicEntry?.clinic || null,
        availableDoctors,
        selectedClinicId: selectedClinicId ? Number(selectedClinicId) : null,
        selectedDoctorId: selectedDoctorId ? Number(selectedDoctorId) : null,
        selectClinic,
        selectDoctor,
        refreshClinics,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);