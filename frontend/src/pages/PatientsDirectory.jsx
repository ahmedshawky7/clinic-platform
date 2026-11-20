import { useState, useEffect, useCallback } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import {
  Search, Key, CheckCircle2, UsersRound, Phone, KeyRound, Ban, ShieldCheck, X, Copy
} from 'lucide-react';

export default function PatientsDirectory() {
  const { selectedClinicId } = useAuth();
  const [patients, setPatients] = useState([]);
  const [blockedPhones, setBlockedPhones] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [credsModal, setCredsModal] = useState(null);

  const fetchPatients = useCallback(async () => {
    try {
      const res = await axiosClient.get('/doctor/patients/all');
      setPatients(res.data || []);
    } catch (error) {
      console.error('Failed to fetch patients', error);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchBlocked = useCallback(async () => {
    if (!selectedClinicId) return;
    try {
      const res = await axiosClient.get(`/clinics/${selectedClinicId}/blocked-patients`);
      setBlockedPhones((res.data || []).map((b) => b.phoneNumber));
    } catch {
      setBlockedPhones([]);
    }
  }, [selectedClinicId]);

  useEffect(() => {
    fetchPatients();
    fetchBlocked();
  }, [fetchPatients, fetchBlocked]);

  const handleGenerateAccount = async (patientId, fullName) => {
    if (!window.confirm('هل تريد إنشاء حساب لهذا المريض؟')) return;
    try {
      const res = await axiosClient.post(`/doctor/patients/${patientId}/generate-account`);
      setCredsModal({
        title: `تم إنشاء حساب للمريض: ${fullName}`,
        username: res.data.username,
        password: res.data.password,
      });
      fetchPatients();
    } catch (err) {
      alert(err.response?.data?.detail || err.response?.data?.error || 'فشل في إنشاء الحساب.');
    }
  };

  const handleResetPassword = async (patientId, fullName) => {
    if (!window.confirm(`هل تريد توليد كلمة مرور مؤقتة جديدة للمريض "${fullName}"؟`)) return;
    try {
      const res = await axiosClient.post(`/doctor/patients/${patientId}/reset-password`);
      setCredsModal({
        title: `كلمة المرور المؤقتة للمريض: ${fullName}`,
        username: res.data.username,
        password: res.data.temporaryPassword || res.data.password,
      });
    } catch (err) {
      alert(err.response?.data?.detail || err.response?.data?.error || 'فشل في إعادة تعيين كلمة المرور.');
    }
  };

  const handleToggleBlock = async (patient) => {
    if (!selectedClinicId) return alert('اختر فرعاً أولاً');
    if (!patient.phoneNumber) return alert('المريض لا يملك رقم هاتف مسجل');

    const isBlocked = blockedPhones.includes(patient.phoneNumber);
    try {
      if (isBlocked) {
        if (!window.confirm(`هل تريد فك الحظر عن رقم ${patient.phoneNumber}؟`)) return;
        await axiosClient.delete(`/clinics/${selectedClinicId}/blocked-patients`, {
          params: { phoneNumber: patient.phoneNumber },
        });
      } else {
        const reason = window.prompt('سبب حظر الحجز الأونلاين لهذا الرقم (اختياري):', 'حجوزات وهمية متكررة');
        if (reason === null) return;
        await axiosClient.post(`/clinics/${selectedClinicId}/blocked-patients`, {
          phoneNumber: patient.phoneNumber,
          reason,
        });
      }
      await fetchBlocked();
    } catch (err) {
      alert(err.response?.data?.detail || err.response?.data?.error || 'فشل في تحديث حالة الحظر');
    }
  };

  const filteredPatients = patients.filter(
    (p) =>
      (p.fullName || '').toLowerCase().includes(search.toLowerCase()) ||
      (p.phoneNumber && p.phoneNumber.includes(search))
  );

  return (
    <DashboardLayout title="دليل المرضى">
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="relative">
          <Search className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="ابحث عن مريض بالاسم أو رقم الهاتف..."
            className="input-fancy w-full py-4 pr-12 pl-4 shadow-lg font-semibold"
          />
        </div>

        <div className="card overflow-hidden">
          {loading ? (
            <div className="p-12 text-center font-bold text-slate-400">جاري تحميل البيانات...</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-right">
                <thead>
                  <tr className="border-b border-slate-100">
                    <th className="p-5 font-black text-slate-500 text-sm">الاسم</th>
                    <th className="p-5 font-black text-slate-500 text-sm">رقم الهاتف</th>
                    <th className="p-5 font-black text-slate-500 text-sm">تاريخ الميلاد</th>
                    <th className="p-5 font-black text-slate-500 text-sm">حالة الحساب</th>
                    <th className="p-5 font-black text-slate-500 text-sm">الحظر والأمان</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredPatients.map((p) => {
                    const isBlocked = p.phoneNumber && blockedPhones.includes(p.phoneNumber);
                    return (
                      <tr key={p.id} className="border-b border-slate-50">
                        <td className="p-5 font-bold text-slate-800">{p.fullName}</td>
                        <td className="p-5 text-slate-500 font-semibold" dir="ltr">
                          <span className="inline-flex items-center gap-1.5">
                            <Phone size={14} /> {p.phoneNumber || 'غير مسجل'}
                          </span>
                        </td>
                        <td className="p-5 text-slate-500 font-semibold">{p.dateOfBirth || 'غير مسجل'}</td>
                        <td className="p-5">
                          {p.hasAccount ? (
                            <span className="inline-flex items-center gap-1.5 bg-emerald-50 text-emerald-600 px-3 py-1.5 rounded-xl font-bold text-xs">
                              <CheckCircle2 size={15} /> حساب نشط
                            </span>
                          ) : (
                            <button
                              onClick={() => handleGenerateAccount(p.id, p.fullName)}
                              className="inline-flex items-center gap-2 bg-indigo-50 text-indigo-600 px-3.5 py-2 rounded-xl font-bold text-xs hover:bg-indigo-100"
                            >
                              <Key size={14} /> إنشاء حساب
                            </button>
                          )}
                        </td>
                        <td className="p-5">
                          <div className="flex flex-wrap gap-2">
                            {p.hasAccount && (
                              <button
                                onClick={() => handleResetPassword(p.id, p.fullName)}
                                className="inline-flex items-center gap-1.5 bg-amber-50 text-amber-700 px-3 py-1.5 rounded-xl font-bold text-xs hover:bg-amber-100"
                              >
                                <KeyRound size={14} /> إعادة تعيين كلمة المرور
                              </button>
                            )}
                            {p.phoneNumber && selectedClinicId && (
                              <button
                                onClick={() => handleToggleBlock(p)}
                                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl font-bold text-xs ${
                                  isBlocked
                                    ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                                    : 'bg-red-50 text-red-600 hover:bg-red-100'
                                }`}
                              >
                                {isBlocked ? <><ShieldCheck size={14} /> فك الحظر</> : <><Ban size={14} /> حظر أونلاين</>}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                  {filteredPatients.length === 0 && (
                    <tr>
                      <td colSpan="5" className="p-14 text-center">
                        <UsersRound size={44} className="mx-auto text-slate-200 mb-3" />
                        <p className="text-slate-400 font-bold">لا يوجد مرضى مطابقين للبحث.</p>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {credsModal && (
        <div className="modal-backdrop flex items-center justify-center z-50 p-4" onClick={() => setCredsModal(null)}>
          <div className="bg-white p-8 rounded-3xl w-96 max-w-full shadow-2xl relative animate-scale-in" onClick={(e) => e.stopPropagation()}>
            <button onClick={() => setCredsModal(null)} className="absolute top-5 left-5 text-slate-400 hover:text-red-500">
              <X size={20} />
            </button>
            <h3 className="text-lg font-black text-slate-800 mb-4">{credsModal.title}</h3>
            <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-2 mb-5 text-sm">
              <p className="font-bold text-slate-700">اسم المستخدم: <span dir="ltr">{credsModal.username}</span></p>
              <p className="font-bold text-indigo-700">كلمة المرور: <span dir="ltr">{credsModal.password}</span></p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => navigator.clipboard?.writeText(`اسم المستخدم: ${credsModal.username}\nكلمة المرور: ${credsModal.password}`)}
                className="btn-primary flex-1 py-3 font-bold flex items-center justify-center gap-2"
              >
                <Copy size={16} /> نسخ البيانات
              </button>
              <button onClick={() => setCredsModal(null)} className="flex-1 bg-slate-100 text-slate-600 font-bold py-3 rounded-xl">
                إغلاق
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}