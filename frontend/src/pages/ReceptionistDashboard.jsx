import { useState, useEffect, useCallback } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import DashboardLayout from '../components/DashboardLayout';
import {
  Users, UserCheck, Clock, PhoneCall, UserX, UserPlus, Search, RotateCcw, AlertTriangle, Siren
} from 'lucide-react';

export default function ReceptionistDashboard() {
  const { selectedClinicId, selectedDoctorId } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { queueData, connected } = useWebSocket(selectedClinicId, selectedDoctorId);

  const [skippedPatients, setSkippedPatients] = useState([]);
  const [waitingList, setWaitingList] = useState([]);

  const [isWalkInModalOpen, setIsWalkInModalOpen] = useState(false);
  const [walkInName, setWalkInName] = useState('');
  const [isAddingWalkIn, setIsAddingWalkIn] = useState(false);
  const [walkInDob, setWalkInDob] = useState('');
  const [walkInPhone, setWalkInPhone] = useState('');
  const [walkInSearch, setWalkInSearch] = useState('');
  const [walkInResults, setWalkInResults] = useState([]);
  const [selectedWalkInPatient, setSelectedWalkInPatient] = useState(null);
  const [walkInVisitType, setWalkInVisitType] = useState('EXAMINATION');
  const [walkInPriority, setWalkInPriority] = useState(false);

  const fetchWaitingList = useCallback(async () => {
    if (!selectedClinicId || !selectedDoctorId) return;
    try {
      const res = await axiosClient.get(`/queues/${selectedClinicId}/waiting-list?doctorId=${selectedDoctorId}`);
      setWaitingList(res.data);
    } catch (error) {
      console.error('Failed to fetch waiting list:', error);
    }
  }, [selectedClinicId, selectedDoctorId]);

  const fetchSkipped = useCallback(async () => {
    if (!selectedClinicId || !selectedDoctorId) return;
    try {
      const res = await axiosClient.get(`/queues/${selectedClinicId}/skipped?doctorId=${selectedDoctorId}`);
      setSkippedPatients(res.data);
    } catch (error) {
      console.error(error);
    }
  }, [selectedClinicId, selectedDoctorId]);

  useEffect(() => {
    fetchWaitingList();
  }, [fetchWaitingList, queueData?.waitingCount]);

  useEffect(() => {
    fetchSkipped();
    const interval = setInterval(fetchSkipped, 10000);
    return () => clearInterval(interval);
  }, [fetchSkipped]);

  const handleWalkInSearch = async (e) => {
    const query = e.target.value;
    setWalkInSearch(query);
    if (query.length < 2) {
      setWalkInResults([]);
      return;
    }
    try {
      const res = await axiosClient.get(`/doctor/patients/search?query=${encodeURIComponent(query)}`);
      setWalkInResults(res.data);
    } catch (error) {
      console.error(error);
    }
  };

  const handleCallNext = async () => {
    if (isSubmitting || !selectedClinicId || !selectedDoctorId) return;
    setIsSubmitting(true);
    try {
      await axiosClient.post(`/queues/${selectedClinicId}/call-next?doctorId=${selectedDoctorId}`);
    } catch {
      alert('فشل في استدعاء المريض التالي');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAddWalkIn = async () => {
    if (!selectedWalkInPatient && !walkInName.trim()) {
      return alert('من فضلك ابحث عن مريض أو اكتب اسمه');
    }
    setIsAddingWalkIn(true);
    try {
      await axiosClient.post(`/queues/${selectedClinicId}/walk-in`, {
        doctorId: String(selectedDoctorId),
        patientId: selectedWalkInPatient ? String(selectedWalkInPatient.id) : null,
        fullName: selectedWalkInPatient ? selectedWalkInPatient.fullName : walkInName,
        phoneNumber: walkInPhone,
        dateOfBirth: walkInDob || null,
        visitType: walkInVisitType,
        priority: walkInPriority,
      });
      setIsWalkInModalOpen(false);
      setWalkInName('');
      setWalkInDob('');
      setWalkInPhone('');
      setWalkInSearch('');
      setSelectedWalkInPatient(null);
      setWalkInVisitType('EXAMINATION');
      setWalkInPriority(false);
      fetchWaitingList();
    } catch (error) {
      alert(error.response?.data?.detail || error.response?.data?.error || 'فشل في إضافة المريض');
    } finally {
      setIsAddingWalkIn(false);
    }
  };

  const handleRequeue = async (appointmentId) => {
    try {
      await axiosClient.post(`/queues/${selectedClinicId}/requeue/${appointmentId}?doctorId=${selectedDoctorId}`);
      fetchSkipped();
      fetchWaitingList();
    } catch {
      alert('فشل في إرجاع المريض');
    }
  };

  const handleSkip = async () => {
    if (isSubmitting) return;
    if (!window.confirm('هل أنت متأكد من تخطي هذا المريض؟')) return;

    setIsSubmitting(true);
    try {
      await axiosClient.post(`/queues/${selectedClinicId}/skip-current?doctorId=${selectedDoctorId}`);
      fetchSkipped();
      fetchWaitingList();
    } catch {
      alert('فشل في تخطي المريض');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancelTodayQueue = async () => {
    if (isSubmitting || !selectedClinicId || !selectedDoctorId) return;
    if (!window.confirm('تحذير طارئ: سيتم إلغاء جميع حجوزات اليوم المتبقية لهذا الطبيب. متابعة؟')) return;
    setIsSubmitting(true);
    try {
      await axiosClient.post(`/queues/${selectedClinicId}/cancel-today?doctorId=${selectedDoctorId}`);
      alert('تم إلغاء طابور اليوم بالكامل.');
      fetchWaitingList();
      fetchSkipped();
    } catch (error) {
      alert(error.response?.data?.detail || error.response?.data?.error || 'فشل في إلغاء طابور اليوم');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!selectedClinicId || !selectedDoctorId) {
    return (
      <DashboardLayout title="لوحة السكرتارية">
        <div className="card p-10 text-center font-bold text-slate-500">
          لا يوجد فرع أو طبيب مسكّن حالياً. يرجى ربط طبيب بالفرع من صفحة إدارة المنشأة.
        </div>
      </DashboardLayout>
    );
  }

  const hasWaiting = (queueData?.waitingCount ?? 0) > 0;
  const hasCurrent = Boolean(queueData?.currentQueueNumber);

  return (
    <DashboardLayout title="لوحة السكرتارية">
      <div className="max-w-5xl mx-auto space-y-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-2 bg-white px-4 py-3 rounded-xl shadow-sm border border-slate-100 w-fit">
            <div className={`w-3 h-3 rounded-full live-dot ${connected ? 'bg-emerald-500' : 'bg-red-500'}`} />
            <span className="text-sm text-slate-600 font-bold">
              {connected ? 'متصل بالطابور (Live)' : 'جاري الاتصال...'}
            </span>
          </div>

          {(hasWaiting || hasCurrent) && (
            <button
              onClick={handleCancelTodayQueue}
              disabled={isSubmitting}
              className="px-4 py-2.5 rounded-xl bg-rose-50 text-rose-700 border border-rose-200 font-bold text-sm flex items-center gap-2 hover:bg-rose-100"
            >
              <AlertTriangle size={16} /> إلغاء طابور اليوم (طوارئ)
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 stagger">
          <div className="relative overflow-hidden rounded-3xl p-8 text-white bg-gradient-to-br from-blue-600 via-indigo-600 to-indigo-800 shadow-xl">
            <div className="flex justify-between items-start mb-6">
              <div>
                <p className="text-blue-100 font-bold mb-2">الرقم الحالي داخل العيادة</p>
                {queueData?.currentQueueNumber ? (
                  <p key={queueData.currentQueueNumber} className="text-7xl font-black animate-number-pop">
                    {queueData.currentQueueNumber}
                  </p>
                ) : (
                  <p className="text-7xl font-black text-white/40">-</p>
                )}
              </div>
              <div className="bg-white/15 p-4 rounded-2xl">
                <UserCheck size={32} />
              </div>
            </div>
          </div>

          <div className="card p-8">
            <div className="flex justify-between items-start mb-6">
              <div>
                <p className="text-slate-400 font-bold mb-2">عدد المنتظرين</p>
                <p className="text-7xl font-black text-slate-800">{queueData?.waitingCount ?? 0}</p>
              </div>
              <div className="bg-orange-100 p-4 rounded-2xl text-orange-500">
                <Users size={32} />
              </div>
            </div>
            <div className="flex items-center gap-2 text-slate-500 text-sm mt-4 bg-slate-50 border border-slate-100 p-3 rounded-xl w-fit font-bold">
              <Clock size={16} className="text-orange-400" />
              الوقت المتوقع: {queueData?.estimatedWaitMinutes ?? 0} دقيقة
            </div>
          </div>
        </div>

        {waitingList.length > 0 && (
          <div className="card p-6">
            <h3 className="text-xl font-black text-slate-800 mb-4 flex items-center gap-2">
              <Users className="text-indigo-500" /> المرضى في الانتظار
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {waitingList.map((p) => (
                <div
                  key={p.appointmentId}
                  className={`flex items-center justify-between gap-3 p-3 rounded-xl border ${
                    p.priority ? 'bg-rose-50 border-rose-200' : 'bg-slate-50 border-slate-100'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-9 h-9 rounded-xl flex items-center justify-center font-black text-white ${
                        p.priority
                          ? 'bg-gradient-to-br from-rose-500 to-red-600'
                          : 'bg-gradient-to-br from-blue-500 to-indigo-600'
                      }`}
                    >
                      {p.queueNumber}
                    </div>
                    <div>
                      <span className="font-bold text-slate-700 block">{p.patientName}</span>
                      <span className="text-xs font-bold text-slate-400">
                        {p.visitType === 'CONSULTATION' ? 'استشارة' : 'كشف جديد'}
                      </span>
                    </div>
                  </div>
                  {p.priority && (
                    <span className="inline-flex items-center gap-1 bg-rose-600 text-white text-[11px] font-black px-2.5 py-1 rounded-full">
                      <Siren size={12} /> طوارئ
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="flex flex-col md:flex-row gap-4">
          <button
            onClick={handleCallNext}
            disabled={!hasWaiting || isSubmitting}
            className={`flex-1 py-6 px-4 rounded-3xl text-2xl font-black flex items-center justify-center gap-4 transition-all shadow-xl ${
              hasWaiting && !isSubmitting
                ? 'bg-gradient-to-br from-emerald-500 to-emerald-600 text-white'
                : 'bg-slate-200 text-slate-400 cursor-not-allowed'
            }`}
          >
            <PhoneCall size={32} />
            {hasWaiting ? 'استدعاء المريض التالي' : 'لا يوجد مرضى في الانتظار'}
          </button>

          <button
            onClick={handleSkip}
            disabled={!hasCurrent || isSubmitting}
            className={`flex-1 py-6 px-4 rounded-3xl text-2xl font-black flex items-center justify-center gap-4 transition-all shadow-xl ${
              hasCurrent && !isSubmitting
                ? 'bg-gradient-to-br from-red-500 to-rose-600 text-white'
                : 'bg-slate-200 text-slate-400 cursor-not-allowed'
            }`}
          >
            <UserX size={32} />
            تخطي المريض (غير موجود)
          </button>
        </div>

        <button
          onClick={() => setIsWalkInModalOpen(true)}
          className="w-full py-4 rounded-2xl text-lg font-black flex items-center justify-center gap-3 bg-white border-2 border-dashed border-indigo-300 text-indigo-600 hover:bg-indigo-50"
        >
          <UserPlus size={24} /> إضافة مريض مباشر أو حالة طارئة (Walk-in)
        </button>

        {skippedPatients.length > 0 && (
          <div className="card p-6">
            <h3 className="text-xl font-black text-slate-800 mb-4">مرضى تم تخطيهم</h3>
            <div className="space-y-3">
              {skippedPatients.map((p) => (
                <div key={p.appointmentId} className="flex justify-between items-center bg-slate-50 border border-slate-100 p-4 rounded-xl">
                  <div className="flex items-center gap-3">
                    <div className="bg-orange-100 text-orange-600 w-10 h-10 rounded-xl flex items-center justify-center font-black">
                      {p.queueNumber}
                    </div>
                    <span className="font-bold text-slate-700 text-lg">{p.patientName}</span>
                  </div>
                  <button
                    onClick={() => handleRequeue(p.appointmentId)}
                    className="bg-indigo-50 text-indigo-600 px-4 py-2 rounded-xl font-bold flex items-center gap-2"
                  >
                    <RotateCcw size={16} /> إرجاع للطابور
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {isWalkInModalOpen && (
          <div className="modal-backdrop flex items-center justify-center z-50 p-4" onClick={() => setIsWalkInModalOpen(false)}>
            <div className="bg-white p-8 rounded-3xl w-[500px] max-w-full shadow-2xl" onClick={(e) => e.stopPropagation()}>
              <h3 className="text-2xl font-black mb-6 text-slate-800 flex items-center gap-2">
                <UserPlus className="text-indigo-500" /> إضافة مريض للطابور
              </h3>

              <div className="mb-4 relative">
                <div className="relative">
                  <Search className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                  <input
                    type="text"
                    value={walkInSearch}
                    onChange={handleWalkInSearch}
                    placeholder="ابحث عن مريض مسجل..."
                    className="input-fancy w-full p-3 pr-10 font-semibold"
                  />
                </div>
                {walkInResults.length > 0 && (
                  <div className="absolute w-full mt-2 bg-white rounded-2xl shadow-2xl border max-h-56 overflow-y-auto z-50">
                    {walkInResults.map((p) => (
                      <div
                        key={p.id}
                        onClick={() => {
                          setSelectedWalkInPatient(p);
                          setWalkInResults([]);
                          setWalkInSearch(p.fullName);
                        }}
                        className="p-3.5 border-b hover:bg-indigo-50 cursor-pointer flex justify-between items-center"
                      >
                        <span className="font-bold">{p.fullName}</span>
                        <span className="text-xs text-slate-400">{p.phoneNumber || ''}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {selectedWalkInPatient && (
                <div className="mb-4 bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-2 rounded-xl font-bold text-sm flex items-center">
                  ✓ تم اختيار: {selectedWalkInPatient.fullName}
                  <button onClick={() => { setSelectedWalkInPatient(null); setWalkInSearch(''); }} className="mr-auto font-black">✕</button>
                </div>
              )}

              <input
                type="text"
                value={selectedWalkInPatient ? selectedWalkInPatient.fullName : walkInName}
                onChange={(e) => setWalkInName(e.target.value)}
                placeholder="اسم المريض..."
                disabled={!!selectedWalkInPatient}
                className="input-fancy w-full p-3 mb-3 font-semibold"
              />
              <input
                type="text"
                value={walkInPhone}
                onChange={(e) => setWalkInPhone(e.target.value)}
                placeholder="رقم الموبايل..."
                className="input-fancy w-full p-3 mb-3 font-semibold"
                dir="ltr"
              />
              {!selectedWalkInPatient && (
                <input
                  type="date"
                  value={walkInDob}
                  onChange={(e) => setWalkInDob(e.target.value)}
                  className="input-fancy w-full p-3 mb-4 font-semibold"
                />
              )}

              <div className="grid grid-cols-2 gap-3 mb-4">
                <button
                  type="button"
                  onClick={() => setWalkInVisitType('EXAMINATION')}
                  className={`p-3 rounded-xl border font-bold text-sm ${
                    walkInVisitType === 'EXAMINATION'
                      ? 'bg-indigo-50 border-indigo-500 text-indigo-700'
                      : 'border-slate-200 text-slate-500'
                  }`}
                >
                  كشف جديد
                </button>
                <button
                  type="button"
                  onClick={() => setWalkInVisitType('CONSULTATION')}
                  className={`p-3 rounded-xl border font-bold text-sm ${
                    walkInVisitType === 'CONSULTATION'
                      ? 'bg-indigo-50 border-indigo-500 text-indigo-700'
                      : 'border-slate-200 text-slate-500'
                  }`}
                >
                  استشارة
                </button>
              </div>

              <label className="flex items-center gap-3 p-3.5 rounded-2xl border border-rose-200 bg-rose-50 cursor-pointer mb-6">
                <input
                  type="checkbox"
                  checked={walkInPriority}
                  onChange={(e) => setWalkInPriority(e.target.checked)}
                  className="w-5 h-5 accent-rose-600"
                />
                <div>
                  <p className="font-black text-rose-700 text-sm flex items-center gap-1.5">
                    <Siren size={16} /> حالة طارئة (أولوية قصوى)
                  </p>
                  <p className="text-xs text-rose-600">
                    يدخل في مقدمة المنتظرين مباشرة دون تغيير أرقامهم
                  </p>
                </div>
              </label>

              <div className="flex gap-4">
                <button onClick={handleAddWalkIn} disabled={isAddingWalkIn} className="btn-primary flex-1 font-bold py-3">
                  {isAddingWalkIn ? 'جاري الإضافة...' : 'إضافة للطابور'}
                </button>
                <button onClick={() => setIsWalkInModalOpen(false)} className="flex-1 bg-slate-100 text-slate-600 font-bold py-3 rounded-xl">
                  إلغاء
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}