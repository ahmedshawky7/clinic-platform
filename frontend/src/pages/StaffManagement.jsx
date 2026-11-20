import { useState, useEffect, useCallback } from 'react';
import axiosClient from '../api/axiosClient';
import DashboardLayout from '../components/DashboardLayout';
import {
  UserPlus, Users, ShieldCheck, ShieldOff, KeyRound, CheckCircle2, Ban, X, Copy
} from 'lucide-react';

export default function StaffManagement() {
  const [staff, setStaff] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newRec, setNewRec] = useState({ fullName: '', username: '', password: '', phoneNumber: '' });
  const [isAdding, setIsAdding] = useState(false);

  const [tempCreds, setTempCreds] = useState(null);
  const [isResettingId, setIsResettingId] = useState(null);

  const loadStaff = useCallback(async () => {
    try {
      const res = await axiosClient.get('/org/staff');
      setStaff(res.data || []);
    } catch (e) {
      console.error('Failed to load staff', e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadStaff();
  }, [loadStaff]);

  const handleAddReceptionist = async (e) => {
    e.preventDefault();
    if (!newRec.fullName.trim() || !newRec.username.trim() || !newRec.password.trim()) {
      return alert('يرجى إدخال الاسم واسم المستخدم وكلمة المرور');
    }
    setIsAdding(true);
    try {
      await axiosClient.post('/org/receptionists', newRec);
      alert('تم إضافة حساب السكرتير بنجاح');
      setNewRec({ fullName: '', username: '', password: '', phoneNumber: '' });
      await loadStaff();
    } catch (error) {
      alert(error.response?.data?.error || error.response?.data?.detail || 'فشل في إضافة الموظف');
    } finally {
      setIsAdding(false);
    }
  };

  const handleToggleStatus = async (member) => {
    const targetActive = !member.active;
    const confirmMsg = targetActive
      ? `هل تريد إعادة تفعيل حساب "${member.fullName}"؟`
      : `هل أنت متأكد من إيقاف حساب "${member.fullName}"؟ سيتم طرده ومنعه من الدخول فوراً مع حفظ سجله القديم.`;

    if (!window.confirm(confirmMsg)) return;

    try {
      await axiosClient.put(`/org/staff/${member.id}/status`, { active: targetActive });
      await loadStaff();
    } catch (error) {
      alert(error.response?.data?.detail || error.response?.data?.error || 'فشل في تغيير حالة الحساب');
    }
  };

  const handleResetPassword = async (member) => {
    if (!window.confirm(`هل تريد توليد كلمة مرور مؤقتة جديدة للموظف "${member.fullName}"؟`)) return;
    setIsResettingId(member.id);
    try {
      const res = await axiosClient.post(`/org/staff/${member.id}/reset-password`);
      setTempCreds({
        fullName: member.fullName,
        username: res.data.username || member.username,
        password: res.data.temporaryPassword || res.data.password,
      });
    } catch (error) {
      alert(error.response?.data?.detail || error.response?.data?.error || 'فشل في إعادة تعيين كلمة المرور');
    } finally {
      setIsResettingId(null);
    }
  };

  const copyPassword = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
      alert('تم نسخ كلمة المرور المؤقتة');
    } catch {
    }
  };

  const roleLabel = (role) => {
    if (role === 'RECEPTIONIST') return 'سكرتارية واستقبال';
    if (role === 'DOCTOR') return 'طبيب';
    if (role === 'ORG_ADMIN') return 'مدير منشأة';
    return role;
  };

  return (
    <DashboardLayout title="إدارة فريق العمل والسكرتارية">
      <div className="max-w-6xl mx-auto space-y-8">
        <form onSubmit={handleAddReceptionist} className="card p-6 space-y-4">
          <h3 className="font-black text-lg text-slate-800 flex items-center gap-2">
            <UserPlus size={20} className="text-indigo-600" /> إضافة سكرتير / موظف استقبال جديد
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-5 gap-3 items-end">
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">الاسم الكامل *</label>
              <input
                className="input-fancy w-full p-3 text-sm font-semibold"
                placeholder="مثال: سارة أحمد"
                required
                value={newRec.fullName}
                onChange={(e) => setNewRec({ ...newRec, fullName: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">اسم المستخدم *</label>
              <input
                className="input-fancy w-full p-3 text-sm font-semibold"
                dir="ltr"
                placeholder="reception_sara"
                required
                value={newRec.username}
                onChange={(e) => setNewRec({ ...newRec, username: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">رقم الهاتف</label>
              <input
                className="input-fancy w-full p-3 text-sm font-semibold"
                dir="ltr"
                placeholder="01xxxxxxxxx"
                value={newRec.phoneNumber}
                onChange={(e) => setNewRec({ ...newRec, phoneNumber: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">كلمة المرور *</label>
              <input
                type="password"
                className="input-fancy w-full p-3 text-sm font-semibold"
                dir="ltr"
                placeholder="••••••"
                required
                value={newRec.password}
                onChange={(e) => setNewRec({ ...newRec, password: e.target.value })}
              />
            </div>
            <button type="submit" disabled={isAdding} className="btn-primary font-bold py-3">
              {isAdding ? 'جاري الإضافة...' : '+ إضافة موظف'}
            </button>
          </div>
        </form>

        <div className="card overflow-hidden">
          <div className="p-6 border-b border-slate-100 flex items-center justify-between">
            <h3 className="font-black text-xl text-slate-800 flex items-center gap-2">
              <Users size={22} className="text-indigo-600" /> فريق العمل ({staff.length})
            </h3>
            <span className="text-xs font-bold text-slate-500">
              يتم الاحتفاظ بسجل العمليات القديمة عند إيقاف أي حساب
            </span>
          </div>

          {loading ? (
            <div className="p-12 text-center font-bold text-slate-400">جاري تحميل فريق العمل...</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-right">
                <thead>
                  <tr className="border-b border-slate-100">
                    <th className="p-4 text-sm font-black">الاسم</th>
                    <th className="p-4 text-sm font-black">اسم المستخدم</th>
                    <th className="p-4 text-sm font-black">الدور الوظيفي</th>
                    <th className="p-4 text-sm font-black">الحالة</th>
                    <th className="p-4 text-sm font-black">الإجراءات الأمنية</th>
                  </tr>
                </thead>
                <tbody>
                  {staff.map((m) => (
                    <tr key={m.id} className="border-b border-slate-50">
                      <td className="p-4">
                        <p className="font-bold text-slate-800">{m.fullName}</p>
                        <p className="text-xs text-slate-400">{m.phoneNumber || ''}</p>
                      </td>
                      <td className="p-4 font-semibold text-slate-600">{m.username}</td>
                      <td className="p-4">
                        <span className="px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-bold">
                          {roleLabel(m.role)} {m.owner ? '• مالك' : ''}
                        </span>
                      </td>
                      <td className="p-4">
                        {m.active ? (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-black">
                            <CheckCircle2 size={14} /> نشط
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-red-50 text-red-600 text-xs font-black">
                            <Ban size={14} /> موقوف
                          </span>
                        )}
                      </td>
                      <td className="p-4">
                        <div className="flex flex-wrap gap-2">
                          {!m.owner && (
                            m.active ? (
                              <button
                                onClick={() => handleToggleStatus(m)}
                                className="px-3 py-1.5 rounded-xl bg-red-50 text-red-600 hover:bg-red-100 font-bold text-xs flex items-center gap-1.5"
                              >
                                <ShieldOff size={14} /> إيقاف الحساب
                              </button>
                            ) : (
                              <button
                                onClick={() => handleToggleStatus(m)}
                                className="px-3 py-1.5 rounded-xl bg-emerald-50 text-emerald-700 hover:bg-emerald-100 font-bold text-xs flex items-center gap-1.5"
                              >
                                <ShieldCheck size={14} /> إعادة تفعيل
                              </button>
                            )
                          )}
                          <button
                            onClick={() => handleResetPassword(m)}
                            disabled={isResettingId === m.id}
                            className="px-3 py-1.5 rounded-xl bg-amber-50 text-amber-700 hover:bg-amber-100 font-bold text-xs flex items-center gap-1.5 disabled:opacity-50"
                          >
                            <KeyRound size={14} />
                            {isResettingId === m.id ? 'جاري التوليد...' : 'توليد كلمة مرور مؤقتة'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {staff.length === 0 && (
                    <tr>
                      <td colSpan="5" className="p-10 text-center text-slate-400 font-bold">
                        لا يوجد موظفون مضافون حالياً.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {tempCreds && (
        <div className="modal-backdrop flex items-center justify-center z-50 p-4" onClick={() => setTempCreds(null)}>
          <div className="bg-white p-8 rounded-3xl w-96 max-w-full shadow-2xl relative animate-scale-in" onClick={(e) => e.stopPropagation()}>
            <button onClick={() => setTempCreds(null)} className="absolute top-5 left-5 text-slate-400 hover:text-red-500">
              <X size={20} />
            </button>
            <h3 className="text-xl font-black text-slate-800 mb-2 flex items-center gap-2">
              <KeyRound className="text-amber-500" /> بيانات الدخول المؤقتة
            </h3>
            <p className="text-sm text-slate-500 font-semibold mb-4">
              الموظف: {tempCreds.fullName}
            </p>
            <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-2 mb-5 text-sm">
              <p className="font-bold text-slate-700">اسم المستخدم: <span dir="ltr">{tempCreds.username}</span></p>
              <p className="font-bold text-indigo-700">كلمة المرور المؤقتة: <span dir="ltr">{tempCreds.password}</span></p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => copyPassword(tempCreds.password)}
                className="btn-primary flex-1 py-3 font-bold flex items-center justify-center gap-2"
              >
                <Copy size={16} /> نسخ كلمة المرور
              </button>
              <button
                onClick={() => setTempCreds(null)}
                className="flex-1 bg-slate-100 text-slate-600 font-bold py-3 rounded-xl"
              >
                إغلاق
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}