import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  LogOut, LayoutDashboard, Stethoscope, Monitor, Users, BarChart3,
  XCircle, CheckCircle, Clock, Building2, ShieldAlert,
  PanelRightClose, PanelRightOpen, HeartPulse, MapPin, UserCog,
  KeyRound, AlertTriangle, Eye, EyeOff, Tv
} from 'lucide-react';
import axiosClient from '../api/axiosClient';

export default function DashboardLayout({ children, title }) {
  const {
    user,
    logout,
    clinicsData,
    currentClinic,
    availableDoctors,
    selectedClinicId,
    selectedDoctorId,
    selectClinic,
    selectDoctor,
    refreshClinics,
  } = useAuth();

  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  const [isClinicOpen, setIsClinicOpen] = useState(true);
  const [showReasonModal, setShowReasonModal] = useState(false);
  const [closureReason, setClosureReason] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [schedule, setSchedule] = useState({
    bookingOpenTime: '00:00',
    bookingCloseTime: '23:59',
    doctorArrivalTime: '17:00',
  });

  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [showPasswords, setShowPasswords] = useState({
    current: false,
    next: false,
    confirm: false,
  });
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [isCancellingQueue, setIsCancellingQueue] = useState(false);

  useEffect(() => {
    if (currentClinic) {
      setIsClinicOpen(Boolean(currentClinic.openToday));
      setClosureReason(currentClinic.closureReason || '');
      setSchedule({
        bookingOpenTime: currentClinic.bookingOpenTime || '00:00',
        bookingCloseTime: currentClinic.bookingCloseTime || '23:59',
        doctorArrivalTime: currentClinic.doctorArrivalTime || '17:00',
      });
    }
  }, [currentClinic]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const closePasswordModal = () => {
    setShowPasswordModal(false);
    setShowPasswords({ current: false, next: false, confirm: false });
  };

  const handleToggleStatus = async (openStatus, reason = '') => {
    if (!selectedClinicId) return;
    setIsLoading(true);
    try {
      await axiosClient.put(`/clinics/${selectedClinicId}/status`, { isOpen: openStatus, reason });
      setIsClinicOpen(openStatus);
      setShowReasonModal(false);
      if (openStatus) setClosureReason('');
      await refreshClinics();
    } catch (e) {
      alert(e.response?.data?.detail || e.response?.data?.error || 'خطأ في تغيير حالة الفرع');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSaveSchedule = async () => {
    if (!selectedClinicId) return;
    try {
      await axiosClient.put(`/clinics/${selectedClinicId}/schedule`, schedule);
      alert('تم حفظ مواعيد الحجز وحضور الطبيب بنجاح');
      setShowScheduleModal(false);
      await refreshClinics();
    } catch (e) {
      alert(e.response?.data?.detail || e.response?.data?.error || 'فشل في حفظ المواعيد');
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (!passwordForm.currentPassword || !passwordForm.newPassword) {
      return alert('يرجى إدخال كلمة المرور الحالية والجديدة');
    }
    if (passwordForm.newPassword.length < 6) {
      return alert('كلمة المرور الجديدة يجب أن تكون 6 أحرف على الأقل');
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      return alert('تأكيد كلمة المرور غير متطابق');
    }
    setIsChangingPassword(true);
    try {
      await axiosClient.post('/auth/change-password', {
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      alert('تم تغيير كلمة المرور بنجاح');
      closePasswordModal();
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      const data = err.response?.data;
      const msg = data?.detail || data?.error || data?.message
        || `خطأ (${err.response?.status || 'اتصال'}) أثناء تحديث كلمة المرور`;
      alert(msg);
    } finally {
      setIsChangingPassword(false);
    }
  };

  const handleCancelTodayQueue = async () => {
    if (!selectedClinicId || !selectedDoctorId) return;
    if (!window.confirm('تحذير: سيتم إلغاء جميع الحجوزات المنتظرة والجارية لهذا الطبيب اليوم. هل أنت متأكد؟')) {
      return;
    }
    setIsCancellingQueue(true);
    try {
      await axiosClient.post(`/queues/${selectedClinicId}/cancel-today?doctorId=${selectedDoctorId}`);
      alert('تم إلغاء طابور اليوم بالكامل وإخطار المرضى.');
      window.location.reload();
    } catch (err) {
      alert(err.response?.data?.detail || err.response?.data?.error || 'فشل في إلغاء الطابور');
    } finally {
      setIsCancellingQueue(false);
    }
  };

  const isSuperAdmin = user?.role === 'SUPER_ADMIN';
  const isOrgAdmin = user?.role === 'ORG_ADMIN' || user?.isOwner;
  const isDoctor = user?.role === 'DOCTOR';
  const isReceptionist = user?.role === 'RECEPTIONIST';

  const navItems = [];
  if (isSuperAdmin) {
    navItems.push({ path: '/super-admin', label: 'إدارة المنصة (SaaS)', icon: ShieldAlert });
  } else {
    if (isOrgAdmin) {
      navItems.push({ path: '/organization', label: 'المنشأة والاشتراك', icon: Building2 });
      navItems.push({ path: '/staff', label: 'فريق العمل والسكرتارية', icon: UserCog });
    }
    if (isReceptionist || isOrgAdmin) {
      navItems.push({ path: '/receptionist', label: 'لوحة السكرتارية', icon: LayoutDashboard });
    }
    if (isDoctor) {
      navItems.push({ path: '/doctor', label: 'لوحة الطبيب', icon: Stethoscope });
    }
    if (isDoctor || isOrgAdmin || isReceptionist) {
      navItems.push({ path: '/patients', label: 'دليل المرضى', icon: Users });
    }
    if (isReceptionist || isDoctor || isOrgAdmin) {
      navItems.push({ path: '/analytics', label: 'الإحصائيات', icon: BarChart3 });
    }
    if (isOrgAdmin) {
      navItems.push({ path: '/clinics', label: 'إدارة الفروع', icon: MapPin });
    }
  }

  return (
    <div className="flex h-screen overflow-hidden" dir="rtl">
      <aside
        className={`${
          collapsed ? 'w-20' : 'w-64'
        } bg-gradient-to-b from-slate-900 via-slate-900 to-indigo-950 flex flex-col transition-all duration-300 relative shrink-0`}
      >
        <div className="h-20 flex items-center justify-center border-b border-white/10 relative">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white shadow-lg">
              <HeartPulse size={22} className="animate-float" />
            </div>
            {!collapsed && (
              <div className="animate-fade-in">
                <h1 className="text-xl font-black text-white leading-tight">عيادتي</h1>
                <p className="text-[10px] text-slate-400 font-bold tracking-wide">SAAS PLATFORM</p>
              </div>
            )}
          </div>
        </div>

        <button
          onClick={() => setCollapsed(!collapsed)}
          className="absolute -left-3 top-24 w-7 h-7 bg-white rounded-full shadow-lg border border-slate-200 flex items-center justify-center text-slate-500 hover:scale-110 transition-all z-20"
        >
          {collapsed ? <PanelRightOpen size={15} /> : <PanelRightClose size={15} />}
        </button>

        <nav className="flex-1 p-4 space-y-2 relative overflow-y-auto">
          {navItems.map((item) => {
            const active = location.pathname === item.path;
            return (
              <button
                key={item.path}
                onClick={() => navigate(item.path)}
                title={collapsed ? item.label : undefined}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-300 group relative ${
                  active
                    ? 'bg-white/10 text-white font-bold shadow-inner'
                    : 'text-slate-400 hover:text-white hover:bg-white/5'
                } ${collapsed ? 'justify-center px-0' : ''}`}
              >
                {active && <span className="absolute right-0 top-1/2 -translate-y-1/2 w-1 h-7 rounded-full"></span>}
                <item.icon size={20} />
                {!collapsed && <span>{item.label}</span>}
              </button>
            );
          })}

          {!isSuperAdmin && selectedClinicId && (
            <div className="pt-2 space-y-1.5 border-t border-white/10">
              <button
                onClick={() => window.open(`/tv?clinicId=${selectedClinicId}`, '_blank')}
                title={collapsed ? 'شاشة الصالة (كل الأطباء)' : undefined}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-cyan-300 hover:text-white hover:bg-white/5 transition-all ${
                  collapsed ? 'justify-center px-0' : ''
                }`}
              >
                <Tv size={20} />
                {!collapsed && <span>شاشة الصالة (كل الأطباء)</span>}
              </button>

              {selectedDoctorId && (
                <button
                  onClick={() =>
                    window.open(`/tv?clinicId=${selectedClinicId}&doctorId=${selectedDoctorId}`, '_blank')
                  }
                  title={collapsed ? 'شاشة الطبيب المحدد (TV)' : undefined}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-slate-400 hover:text-white hover:bg-white/5 transition-all ${
                    collapsed ? 'justify-center px-0' : ''
                  }`}
                >
                  <Monitor size={20} />
                  {!collapsed && <span>شاشة الطبيب (TV)</span>}
                </button>
              )}
            </div>
          )}
        </nav>

        <div className="p-4 border-t border-white/10 space-y-2">
          <button
            onClick={() => setShowPasswordModal(true)}
            title={collapsed ? 'تغيير كلمة المرور' : undefined}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white font-bold transition-all ${
              collapsed ? 'justify-center px-0' : ''
            }`}
          >
            <KeyRound size={18} />
            {!collapsed && <span>تغيير كلمة المرور</span>}
          </button>

          <button
            onClick={handleLogout}
            title={collapsed ? 'تسجيل الخروج' : undefined}
            className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 font-bold transition-all ${
              collapsed ? 'justify-center px-0' : ''
            }`}
          >
            <LogOut size={20} />
            {!collapsed && <span>تسجيل الخروج</span>}
          </button>
        </div>
      </aside>

      <main className="flex-1 flex flex-col overflow-hidden">
        <header className="min-h-20 py-3 bg-white/80 backdrop-blur-xl border-b border-slate-200/80 flex flex-wrap items-center px-6 justify-between gap-4 shrink-0 z-30">
          <h2 className="text-2xl font-black text-slate-800">{title}</h2>

          {!isSuperAdmin ? (
            <div className="flex flex-wrap items-center gap-3">
              {clinicsData.length > 0 && (
                <select
                  value={selectedClinicId || ''}
                  onChange={(e) => selectClinic(e.target.value)}
                  className="input-fancy px-3 py-2 text-sm font-bold"
                >
                  {clinicsData.map((item) => (
                    <option key={item.clinic.id} value={item.clinic.id}>
                      فرع: {item.clinic.name}
                    </option>
                  ))}
                </select>
              )}

              {availableDoctors.length > 0 && (user?.role !== 'DOCTOR' || user?.isOwner) && (
                <select
                  value={selectedDoctorId || ''}
                  onChange={(e) => selectDoctor(e.target.value)}
                  className="input-fancy px-3 py-2 text-sm font-bold"
                >
                  {availableDoctors.map((doc) => (
                    <option key={doc.userId} value={doc.userId}>
                      {doc.fullName} ({doc.specialtyName})
                    </option>
                  ))}
                </select>
              )}

              {selectedClinicId && (
                <>
                  {isClinicOpen ? (
                    <button
                      onClick={() => setShowReasonModal(true)}
                      className="bg-red-50 text-red-600 px-3.5 py-2 rounded-xl font-bold text-sm hover:bg-red-100 flex items-center gap-1.5"
                    >
                      <XCircle size={16} /> قفل الفرع
                    </button>
                  ) : (
                    <button
                      onClick={() => handleToggleStatus(true)}
                      disabled={isLoading}
                      className="bg-emerald-50 text-emerald-700 px-3.5 py-2 rounded-xl font-bold text-sm hover:bg-emerald-100 flex items-center gap-1.5"
                    >
                      <CheckCircle size={16} /> فتح الفرع
                    </button>
                  )}

                  <button
                    onClick={() => setShowScheduleModal(true)}
                    className="bg-indigo-50 text-indigo-600 px-3.5 py-2 rounded-xl font-bold text-sm hover:bg-indigo-100 flex items-center gap-1.5"
                  >
                    <Clock size={16} /> مواعيد الحجز والكشف
                  </button>

                  {(isDoctor || isOrgAdmin) && selectedDoctorId && (
                    <button
                      onClick={handleCancelTodayQueue}
                      disabled={isCancellingQueue}
                      className="bg-rose-600 text-white px-3.5 py-2 rounded-xl font-bold text-sm hover:bg-rose-700 disabled:opacity-50 flex items-center gap-1.5"
                    >
                      <AlertTriangle size={15} />
                      {isCancellingQueue ? 'جاري الإلغاء...' : 'إلغاء طابور اليوم (طوارئ)'}
                    </button>
                  )}
                </>
              )}

              <div className="text-xs text-slate-600 font-bold bg-slate-100 px-3.5 py-2 rounded-full">
                {user?.username}
              </div>
            </div>
          ) : (
            <div className="text-xs text-slate-600 font-bold bg-slate-100 px-3.5 py-2 rounded-full">
              {user?.username}
            </div>
          )}
        </header>

        <div className="flex-1 overflow-y-auto p-8">
          <div className="animate-fade-up" key={location.pathname}>
            {children}
          </div>
        </div>
      </main>

      {showReasonModal && (
        <div className="modal-backdrop flex items-center justify-center z-50 p-4">
          <div className="bg-white p-8 rounded-3xl w-96 shadow-2xl animate-scale-in">
            <h3 className="text-2xl font-bold mb-3 text-red-600 flex items-center gap-2">
              <XCircle /> قفل الفرع اليوم
            </h3>
            <p className="text-slate-500 mb-5 text-sm">
              سيتم منع الحجز لهذا الفرع وإظهار سبب الإغلاق للمرضى في التطبيق.
            </p>
            <input
              type="text"
              value={closureReason}
              onChange={(e) => setClosureReason(e.target.value)}
              placeholder="سبب الإغلاق (ظرف طارئ، إجازة...)"
              className="input-fancy w-full p-3 mb-6"
            />
            <div className="flex gap-3">
              <button
                onClick={() => handleToggleStatus(false, closureReason)}
                disabled={isLoading || !closureReason.trim()}
                className="flex-1 bg-red-600 text-white font-bold py-3 rounded-xl hover:bg-red-700 disabled:opacity-50"
              >
                تأكيد القفل
              </button>
              <button
                onClick={() => setShowReasonModal(false)}
                className="flex-1 bg-slate-100 text-slate-600 font-bold py-3 rounded-xl"
              >
                إلغاء
              </button>
            </div>
          </div>
        </div>
      )}

      {showScheduleModal && (
        <div className="modal-backdrop flex items-center justify-center z-50 p-4">
          <div className="bg-white p-8 rounded-3xl w-[480px] max-w-full shadow-2xl animate-scale-in">
            <h3 className="text-2xl font-bold mb-6 text-indigo-600 flex items-center gap-2">
              <Clock /> إعدادات مواعيد الحجز للفرع
            </h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-bold text-slate-700 mb-2">بداية استقبال الحجوزات</label>
                <input
                  type="time"
                  value={schedule.bookingOpenTime}
                  onChange={(e) => setSchedule({ ...schedule, bookingOpenTime: e.target.value })}
                  className="input-fancy w-full p-3"
                />
              </div>
              <div>
                <label className="block text-sm font-bold text-slate-700 mb-2">آخر ميعاد للحجز</label>
                <input
                  type="time"
                  value={schedule.bookingCloseTime}
                  onChange={(e) => setSchedule({ ...schedule, bookingCloseTime: e.target.value })}
                  className="input-fancy w-full p-3"
                />
              </div>
              <div className="pt-2 border-t border-slate-100">
                <label className="block text-sm font-bold text-emerald-700 mb-2">
                  موعد حضور الطبيب (بدء الكشف الفعلي)
                </label>
                <input
                  type="time"
                  value={schedule.doctorArrivalTime}
                  onChange={(e) => setSchedule({ ...schedule, doctorArrivalTime: e.target.value })}
                  className="input-fancy w-full p-3"
                />
              </div>
            </div>
            <div className="flex gap-3 mt-6">
              <button onClick={handleSaveSchedule} className="btn-primary flex-1 font-bold py-3">
                حفظ المواعيد
              </button>
              <button
                onClick={() => setShowScheduleModal(false)}
                className="flex-1 bg-slate-100 text-slate-600 font-bold py-3 rounded-xl"
              >
                إلغاء
              </button>
            </div>
          </div>
        </div>
      )}

      {showPasswordModal && (
        <div className="modal-backdrop flex items-center justify-center z-50 p-4" onClick={closePasswordModal}>
          <div className="bg-white p-8 rounded-3xl w-[420px] max-w-full shadow-2xl animate-scale-in" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-2xl font-black mb-5 text-slate-800 flex items-center gap-2">
              <KeyRound className="text-indigo-600" /> تغيير كلمة المرور
            </h3>
            <form onSubmit={handleChangePassword} className="space-y-4">
              <div className="relative">
                <input
                  type={showPasswords.current ? 'text' : 'password'}
                  required
                  dir="ltr"
                  placeholder="كلمة المرور الحالية"
                  value={passwordForm.currentPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                  className="input-fancy w-full p-3 pl-11 font-semibold"
                />
                <button
                  type="button"
                  onClick={() => setShowPasswords({ ...showPasswords, current: !showPasswords.current })}
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-indigo-600"
                  aria-label="إظهار أو إخفاء كلمة المرور الحالية"
                >
                  {showPasswords.current ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>

              <div className="relative">
                <input
                  type={showPasswords.next ? 'text' : 'password'}
                  required
                  dir="ltr"
                  placeholder="كلمة المرور الجديدة (6 أحرف فأكثر)"
                  value={passwordForm.newPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                  className="input-fancy w-full p-3 pl-11 font-semibold"
                />
                <button
                  type="button"
                  onClick={() => setShowPasswords({ ...showPasswords, next: !showPasswords.next })}
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-indigo-600"
                  aria-label="إظهار أو إخفاء كلمة المرور الجديدة"
                >
                  {showPasswords.next ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>

              <div className="relative">
                <input
                  type={showPasswords.confirm ? 'text' : 'password'}
                  required
                  dir="ltr"
                  placeholder="تأكيد كلمة المرور الجديدة"
                  value={passwordForm.confirmPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })}
                  className="input-fancy w-full p-3 pl-11 font-semibold"
                />
                <button
                  type="button"
                  onClick={() => setShowPasswords({ ...showPasswords, confirm: !showPasswords.confirm })}
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-indigo-600"
                  aria-label="إظهار أو إخفاء تأكيد كلمة المرور"
                >
                  {showPasswords.confirm ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>

              <div className="flex gap-3 pt-2">
                <button type="submit" disabled={isChangingPassword} className="btn-primary flex-1 font-bold py-3">
                  {isChangingPassword ? 'جاري الحفظ...' : 'تحديث كلمة المرور'}
                </button>
                <button
                  type="button"
                  onClick={closePasswordModal}
                  className="flex-1 bg-slate-100 text-slate-600 font-bold py-3 rounded-xl"
                >
                  إلغاء
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};