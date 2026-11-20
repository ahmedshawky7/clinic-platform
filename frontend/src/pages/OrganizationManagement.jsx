import { useState, useEffect, useCallback, useRef } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import {
  Building2, UserPlus, Stethoscope, CreditCard, Link2, Users, Pencil, Banknote, Clock, Info
} from 'lucide-react';

export default function OrganizationManagement() {
  const { refreshClinics } = useAuth();
  const [data, setData] = useState(null);
  const [newClinic, setNewClinic] = useState({ name: '', city: '', address: '', phone: '' });
  const [newSpec, setNewSpec] = useState('');
  const [newDoctor, setNewDoctor] = useState({
    fullName: '', username: '', password: '', phoneNumber: '',
    specialtyId: '', consultationFee: 200, followUpFee: 100, avgConsultationMinutes: 10
  });
  const [doctorAddedNotice, setDoctorAddedNotice] = useState('');
  const [newRec, setNewRec] = useState({ fullName: '', username: '', password: '', phoneNumber: '' });
  const [assign, setAssign] = useState({
    clinicId: '', doctorId: '',
    workingDays: 'SATURDAY,SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY',
    arrivalTime: '17:00', departureTime: '22:00',
    examinationFee: '', followUpFee: ''
  });
  const [isEditingAssignment, setIsEditingAssignment] = useState(false);
  const assignSectionRef = useRef(null);

  const load = useCallback(async () => {
    try {
      const res = await axiosClient.get('/org/overview');
      setData(res.data);
    } catch (e) {
      console.error(e);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const runAction = async (fn, successMsg) => {
    try {
      await fn();
      if (successMsg) alert(successMsg);
      await load();
      await refreshClinics();
    } catch (e) {
      alert(e.response?.data?.detail || e.response?.data?.error || 'حدث خطأ');
    }
  };

  if (!data) {
    return (
      <DashboardLayout title="إدارة المنشأة والاشتراك">
        <div className="p-10 text-center font-bold text-slate-500">جاري التحميل...</div>
      </DashboardLayout>
    );
  }

  const { organization: org, clinics, specialties, doctors, receptionists, assignments } = data;

  const handleEditAssignment = (a) => {
    const clinic = clinics.find((c) => c.id === a.clinicId);
    const doc = doctors.find((d) => d.userId === a.doctorId);
    const effectiveExamFee = a.examinationFee ?? doc?.consultationFee ?? 200;
    const effectiveFollowUpFee = a.followUpFee ?? doc?.followUpFee ?? 100;

    setAssign({
      clinicId: String(a.clinicId),
      doctorId: String(a.doctorId),
      workingDays: a.workingDays || clinic?.workingDays || 'SATURDAY,SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY',
      arrivalTime: a.arrivalTime || '17:00',
      departureTime: a.departureTime || '22:00',
      examinationFee: String(effectiveExamFee),
      followUpFee: String(effectiveFollowUpFee),
    });
    setIsEditingAssignment(true);
    assignSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  };

  const resetAssignForm = () => {
    setAssign({
      clinicId: '',
      doctorId: '',
      workingDays: 'SATURDAY,SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY',
      arrivalTime: '17:00',
      departureTime: '22:00',
      examinationFee: '',
      followUpFee: '',
    });
    setIsEditingAssignment(false);
  };

  return (
    <DashboardLayout title={`إدارة: ${org.name}`}>
      <div className="max-w-6xl mx-auto space-y-8">
        {/* الاشتراك والحدود */}
        <div className="card p-6 flex flex-wrap items-center justify-between gap-6">
          <div>
            <span className="inline-block px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-black mb-2">
              {org.type === 'SOLO_DOCTOR' ? 'طبيب مستقل متعدد الفروع' : 'مركز طبي متعدد الأطباء والتخصصات'}
            </span>
            <h3 className="text-2xl font-black text-slate-800">
              الباقة: {org.plan} — الحالة: {org.subscriptionStatus}
            </h3>
            <p className="text-sm text-slate-600 mt-2 font-bold">
              الفروع: {data.clinicsCount} | الأطباء: {data.doctorsCount} | حجوزات هذا الشهر: {data.monthlyAppointments}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {['BASIC', 'PRO', 'ENTERPRISE'].map((plan) => (
              <button
                key={plan}
                onClick={() => runAction(() => axiosClient.put('/org/subscription', { plan }), `تم التفعيل على باقة ${plan}`)}
                className={`px-4 py-2.5 rounded-xl font-bold text-sm flex items-center gap-1.5 transition-all ${
                  org.plan === plan
                    ? 'bg-emerald-600 text-white shadow-md'
                    : 'bg-indigo-50 text-indigo-700 hover:bg-indigo-100'
                }`}
              >
                <CreditCard size={16} /> باقة {plan}
              </button>
            ))}
          </div>
        </div>

        {/* الفروع */}
        <div className="card p-6 space-y-4">
          <h4 className="font-black text-lg flex items-center gap-2">
            <Building2 size={20} className="text-indigo-500" /> الفروع ({clinics.length})
          </h4>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">اسم الفرع *</label>
              <input
                className="input-fancy w-full p-2.5"
                placeholder="مثال: فرع المهندسين"
                value={newClinic.name}
                onChange={(e) => setNewClinic({ ...newClinic, name: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">المدينة</label>
              <input
                className="input-fancy w-full p-2.5"
                placeholder="مثال: الجيزة"
                value={newClinic.city}
                onChange={(e) => setNewClinic({ ...newClinic, city: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">العنوان التفصيلي</label>
              <input
                className="input-fancy w-full p-2.5"
                placeholder="الشارع / البرج / الدور"
                value={newClinic.address}
                onChange={(e) => setNewClinic({ ...newClinic, address: e.target.value })}
              />
            </div>
            <button
              className="btn-primary py-2.5 font-bold"
              onClick={() =>
                runAction(
                  () => axiosClient.post('/org/clinics', newClinic).then(() => setNewClinic({ name: '', city: '', address: '', phone: '' })),
                  'تمت إضافة الفرع'
                )
              }
            >
              + إضافة فرع
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2">
            {clinics.map((c) => (
              <div key={c.id} className="p-4 rounded-2xl bg-white border border-slate-200">
                <p className="font-bold text-slate-800">{c.name}</p>
                <p className="text-xs text-slate-500 mt-1">{c.city || ''} - {c.address || 'بدون عنوان'}</p>
              </div>
            ))}
          </div>
        </div>

        {/* التخصصات والأطباء للمركز الطبي */}
        {org.type === 'MEDICAL_CENTER' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* التخصصات */}
            <div className="card p-6 space-y-4">
              <h4 className="font-black text-lg flex items-center gap-2">
                <Stethoscope size={20} className="text-indigo-500" /> التخصصات الطبية
              </h4>
              <div className="flex gap-2 items-end">
                <div className="flex-1">
                  <label className="block text-xs font-bold text-slate-600 mb-1.5">اسم التخصص *</label>
                  <input
                    className="input-fancy w-full p-2.5"
                    placeholder="مثال: أطفال، عظام، جلدية..."
                    value={newSpec}
                    onChange={(e) => setNewSpec(e.target.value)}
                  />
                </div>
                <button
                  className="btn-primary px-5 py-2.5 font-bold"
                  onClick={() =>
                    runAction(
                      () => axiosClient.post('/org/specialties', { name: newSpec }).then(() => setNewSpec('')),
                      'تمت إضافة التخصص'
                    )
                  }
                >
                  إضافة
                </button>
              </div>
              <div className="flex flex-wrap gap-2">
                {specialties.map((s) => (
                  <span key={s.id} className="px-3.5 py-1.5 rounded-full bg-indigo-50 text-indigo-700 text-sm font-bold">
                    {s.name}
                  </span>
                ))}
              </div>
            </div>

            {/* إضافة طبيب */}
            <div className="card p-6 space-y-4">
              <h4 className="font-black text-lg flex items-center gap-2">
                <UserPlus size={20} className="text-indigo-500" /> إضافة طبيب جديد
              </h4>

              {doctorAddedNotice && (
                <div className="bg-amber-50 border border-amber-200 text-amber-800 p-3.5 rounded-2xl text-sm font-bold flex items-start gap-2">
                  <Info size={18} className="shrink-0 mt-0.5 text-amber-600" />
                  <span>{doctorAddedNotice}</span>
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1.5">الاسم الكامل للطبيب *</label>
                  <input
                    className="input-fancy w-full p-2.5"
                    placeholder="مثال: د. أحمد محمود"
                    value={newDoctor.fullName}
                    onChange={(e) => setNewDoctor({ ...newDoctor, fullName: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1.5">رقم الهاتف</label>
                  <input
                    className="input-fancy w-full p-2.5"
                    dir="ltr"
                    placeholder="01xxxxxxxxx"
                    value={newDoctor.phoneNumber}
                    onChange={(e) => setNewDoctor({ ...newDoctor, phoneNumber: e.target.value })}
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1.5">اسم المستخدم (لتسجيل الدخول) *</label>
                  <input
                    className="input-fancy w-full p-2.5"
                    dir="ltr"
                    placeholder="dr_ahmed"
                    value={newDoctor.username}
                    onChange={(e) => setNewDoctor({ ...newDoctor, username: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1.5">كلمة المرور *</label>
                  <input
                    className="input-fancy w-full p-2.5"
                    dir="ltr"
                    type="password"
                    placeholder="••••••"
                    value={newDoctor.password}
                    onChange={(e) => setNewDoctor({ ...newDoctor, password: e.target.value })}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1.5">التخصص</label>
                  <select
                    className="input-fancy w-full p-2.5 text-sm"
                    value={newDoctor.specialtyId}
                    onChange={(e) => setNewDoctor({ ...newDoctor, specialtyId: e.target.value })}
                  >
                    <option value="">اختر التخصص</option>
                    {specialties.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1.5">سعر الكشف (ج)</label>
                  <input
                    className="input-fancy w-full p-2.5 text-sm"
                    type="number"
                    min="0"
                    placeholder="200"
                    value={newDoctor.consultationFee}
                    onChange={(e) => setNewDoctor({ ...newDoctor, consultationFee: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1.5">سعر الاستشارة (ج)</label>
                  <input
                    className="input-fancy w-full p-2.5 text-sm"
                    type="number"
                    min="0"
                    placeholder="100"
                    value={newDoctor.followUpFee}
                    onChange={(e) => setNewDoctor({ ...newDoctor, followUpFee: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-600 mb-1.5">مدة الكشف (دقيقة)</label>
                  <input
                    className="input-fancy w-full p-2.5 text-sm"
                    type="number"
                    min="1"
                    placeholder="10"
                    value={newDoctor.avgConsultationMinutes}
                    onChange={(e) => setNewDoctor({ ...newDoctor, avgConsultationMinutes: e.target.value })}
                  />
                </div>
              </div>

              <button
                className="btn-primary w-full py-2.5 font-bold"
                onClick={() => {
                  const docName = newDoctor.fullName;
                  runAction(
                    () =>
                      axiosClient.post('/org/doctors', newDoctor).then(() => {
                        setNewDoctor({
                          fullName: '', username: '', password: '', phoneNumber: '',
                          specialtyId: '', consultationFee: 200, followUpFee: 100, avgConsultationMinutes: 10
                        });
                        setDoctorAddedNotice(
                          `تم تسجيل "${docName}" بنجاح! يرجى الآن تسكينه في أحد الفروع من القسم التالي ليظهر للمرضى في التطبيق.`
                        );
                      }),
                    null
                  );
                }}
              >
                + إضافة الطبيب
              </button>
            </div>
          </div>
        )}

        {/* جدول الفروع والأطباء المسكنون والأسعار الحالية */}
        <div className="card p-6 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h4 className="font-black text-lg flex items-center gap-2">
              <Banknote size={20} className="text-emerald-600" />
              الأسعار الحالية وجداول العمل بالفروع ({assignments?.length || 0})
            </h4>
            <span className="text-xs text-slate-500 font-bold">
              اضغط "تعديل السعر والمواعيد" بجانب أي صف لتحديث أسعاره ومواعيده فوراً
            </span>
          </div>

          {!assignments || assignments.length === 0 ? (
            <div className="p-6 text-center bg-slate-50 rounded-2xl text-slate-500 font-bold text-sm">
              لا يوجد أطباء مسكنون في الفروع بعد. استخدم نموذج التسكين بالأسفل لربط الطبيب بالفرع.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-right">
                <thead>
                  <tr className="border-b border-slate-100 text-slate-500 text-xs font-black">
                    <th className="p-3">الفرع</th>
                    <th className="p-3">الطبيب والتخصص</th>
                    <th className="p-3">سعر الكشف الحالي</th>
                    <th className="p-3">سعر الاستشارة الحالي</th>
                    <th className="p-3">مواعيد العمل</th>
                    <th className="p-3">إجراء</th>
                  </tr>
                </thead>
                <tbody>
                  {assignments.map((a) => {
                    const clinic = clinics.find((c) => c.id === a.clinicId);
                    const doc = doctors.find((d) => d.userId === a.doctorId);
                    const examFee = a.examinationFee ?? doc?.consultationFee ?? 200;
                    const followUpFee = a.followUpFee ?? doc?.followUpFee ?? 100;
                    const isCustomExam = a.examinationFee != null;
                    const isCustomFollowUp = a.followUpFee != null;

                    return (
                      <tr key={a.id} className="border-b border-slate-50">
                        <td className="p-3 font-bold text-slate-800">{clinic?.name || `فرع #${a.clinicId}`}</td>
                        <td className="p-3">
                          <p className="font-bold text-slate-800">{doc?.fullName || `طبيب #${a.doctorId}`}</p>
                          <p className="text-xs text-slate-400">{doc?.specialtyName || ''}</p>
                        </td>
                        <td className="p-3">
                          <span className="font-black text-indigo-700">{examFee} ج</span>
                          <span className="text-[11px] text-slate-400 mr-1.5">
                            ({isCustomExam ? 'مخصص للفرع' : 'افتراضي'})
                          </span>
                        </td>
                        <td className="p-3">
                          <span className="font-black text-teal-700">{followUpFee} ج</span>
                          <span className="text-[11px] text-slate-400 mr-1.5">
                            ({isCustomFollowUp ? 'مخصص للفرع' : 'افتراضي'})
                          </span>
                        </td>
                        <td className="p-3 text-sm font-bold text-slate-600">
                          <span className="inline-flex items-center gap-1">
                            <Clock size={14} className="text-slate-400" />
                            {a.arrivalTime || '17:00'} - {a.departureTime || '22:00'}
                          </span>
                        </td>
                        <td className="p-3">
                          <button
                            type="button"
                            onClick={() => handleEditAssignment(a)}
                            className="px-3.5 py-2 rounded-xl bg-indigo-50 text-indigo-700 hover:bg-indigo-100 font-bold text-xs flex items-center gap-1.5"
                          >
                            <Pencil size={14} /> تعديل السعر والمواعيد
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* نموذج تسكين أو تعديل سعر ومواعيد الطبيب في الفرع */}
        <div
          ref={assignSectionRef}
          className={`card p-6 space-y-4 transition-all ${
            isEditingAssignment ? 'ring-2 ring-indigo-500 bg-indigo-50/20' : ''
          }`}
        >
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h4 className="font-black text-lg flex items-center gap-2">
              <Link2 size={20} className="text-indigo-500" />
              {isEditingAssignment ? 'تعديل السعر والمواعيد في الفرع' : 'ربط وتسكين طبيب في فرع (أو تحديث أسعاره)'}
            </h4>
            {isEditingAssignment && (
              <button
                type="button"
                onClick={resetAssignForm}
                className="text-xs font-bold text-slate-500 bg-slate-100 px-3 py-1.5 rounded-lg hover:bg-slate-200"
              >
                إلغاء التعديل
              </button>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-3 items-end">
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">الفرع *</label>
              <select
                className="input-fancy w-full p-2.5 text-sm"
                value={assign.clinicId}
                onChange={(e) => setAssign({ ...assign, clinicId: e.target.value })}
              >
                <option value="">اختر الفرع</option>
                {clinics.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">الطبيب *</label>
              <select
                className="input-fancy w-full p-2.5 text-sm"
                value={assign.doctorId}
                onChange={(e) => setAssign({ ...assign, doctorId: e.target.value })}
              >
                <option value="">اختر الطبيب</option>
                {doctors.map((d) => (
                  <option key={d.userId} value={d.userId}>{d.fullName} ({d.specialtyName})</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">موعد الحضور</label>
              <input
                type="time"
                className="input-fancy w-full p-2.5"
                value={assign.arrivalTime}
                onChange={(e) => setAssign({ ...assign, arrivalTime: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">موعد الانصراف</label>
              <input
                type="time"
                className="input-fancy w-full p-2.5"
                value={assign.departureTime}
                onChange={(e) => setAssign({ ...assign, departureTime: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">سعر الكشف بالفرع (ج)</label>
              <input
                type="number"
                min="0"
                className="input-fancy w-full p-2.5 text-sm"
                placeholder="مثال: 200"
                value={assign.examinationFee}
                onChange={(e) => setAssign({ ...assign, examinationFee: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">سعر الاستشارة بالفرع (ج)</label>
              <input
                type="number"
                min="0"
                className="input-fancy w-full p-2.5 text-sm"
                placeholder="مثال: 100"
                value={assign.followUpFee}
                onChange={(e) => setAssign({ ...assign, followUpFee: e.target.value })}
              />
            </div>
          </div>

          <div className="flex justify-end">
            <button
              className="btn-primary font-bold px-8 py-2.5"
              onClick={() =>
                runAction(
                  () => axiosClient.post('/org/assignments', assign).then(() => resetAssignForm()),
                  isEditingAssignment ? 'تم تحديث السعر والمواعيد بنجاح' : 'تم حفظ التسكين بنجاح'
                )
              }
            >
              {isEditingAssignment ? 'حفظ التعديلات' : 'حفظ الجدول والأسعار'}
            </button>
          </div>
        </div>

        {/* السكرتارية */}
        <div className="card p-6 space-y-4">
          <h4 className="font-black text-lg flex items-center gap-2">
            <Users size={20} className="text-indigo-500" /> فريق الاستقبال والسكرتارية ({receptionists?.length || 0})
          </h4>
          <div className="grid grid-cols-1 md:grid-cols-5 gap-3 items-end">
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">الاسم الكامل *</label>
              <input
                className="input-fancy w-full p-2.5"
                placeholder="الاسم"
                value={newRec.fullName}
                onChange={(e) => setNewRec({ ...newRec, fullName: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">اسم المستخدم *</label>
              <input
                className="input-fancy w-full p-2.5"
                dir="ltr"
                placeholder="reception_user"
                value={newRec.username}
                onChange={(e) => setNewRec({ ...newRec, username: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-600 mb-1.5">رقم الهاتف</label>
              <input
                className="input-fancy w-full p-2.5"
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
                className="input-fancy w-full p-2.5"
                dir="ltr"
                placeholder="••••••"
                value={newRec.password}
                onChange={(e) => setNewRec({ ...newRec, password: e.target.value })}
              />
            </div>
            <button
              className="btn-primary font-bold py-2.5"
              onClick={() =>
                runAction(
                  () => axiosClient.post('/org/receptionists', newRec).then(() => setNewRec({ fullName: '', username: '', password: '', phoneNumber: '' })),
                  'تمت إضافة موظف الاستقبال'
                )
              }
            >
              + إضافة سكرتارية
            </button>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}