import { useState, useEffect, useCallback } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import DashboardLayout from '../components/DashboardLayout';
import {
  FileText, Pill, CheckCircle2, User, Clock, Search, X, Plus, Trash2, UserRound,
  RotateCcw, Siren, UserPlus
} from 'lucide-react';

const MedicationBuilder = ({ list, isFile, updateMedication, removeMedicationRow, addMedicationRow }) => (
  <div className="space-y-3">
    {list.map((med, index) => (
      <div
        key={index}
        className="flex flex-wrap gap-2.5 items-center bg-white p-3 rounded-xl border border-slate-200"
      >
        <div className="flex-1 min-w-[160px]">
          <input
            list="drugs-list"
            value={med.name}
            onChange={(e) => updateMedication(index, 'name', e.target.value, isFile)}
            placeholder="اسم الدواء (إنجليزي)"
            className="input-fancy w-full p-2.5 text-left text-sm font-semibold"
            dir="ltr"
          />
        </div>
        <select
          value={med.dose}
          onChange={(e) => updateMedication(index, 'dose', e.target.value, isFile)}
          className="input-fancy w-24 p-2.5 text-sm font-semibold"
        >
          <option value="قرص">قرص</option>
          <option value="كبسولة">كبسولة</option>
          <option value="ملعقة صغيرة">ملعقة صغيرة</option>
          <option value="5ml">5ml</option>
          <option value="10ml">10ml</option>
          <option value="حقنة">حقنة</option>
          <option value="بخة">بخة</option>
          <option value="لبوس">لبوس</option>
          <option value="قطرة">قطرة</option>
        </select>
        <div className="w-36">
          <input
            list="freq-list"
            value={med.freq}
            onChange={(e) => updateMedication(index, 'freq', e.target.value, isFile)}
            placeholder="التكرار..."
            className="input-fancy w-full p-2.5 text-sm text-center font-semibold"
          />
        </div>
        <select
          value={med.timing}
          onChange={(e) => updateMedication(index, 'timing', e.target.value, isFile)}
          className="input-fancy w-32 p-2.5 text-sm font-semibold"
        >
          <option value="">بدون توقيت</option>
          <option value="بعد الأكل">بعد الأكل</option>
          <option value="قبل الأكل">قبل الأكل</option>
          <option value="قبل النوم">قبل النوم</option>
          <option value="على معدة فارغة">على معدة فارغة</option>
        </select>
        <button
          onClick={() => removeMedicationRow(index, isFile)}
          type="button"
          className="text-red-400 hover:text-red-600 p-2 rounded-lg"
        >
          <Trash2 size={18} />
        </button>
      </div>
    ))}

    <button
      onClick={() => addMedicationRow(isFile)}
      type="button"
      className="flex items-center gap-1.5 text-indigo-600 font-bold text-sm bg-indigo-50 hover:bg-indigo-100 px-4 py-2.5 rounded-xl"
    >
      <Plus size={16} /> إضافة دواء آخر
    </button>
  </div>
);

export default function DoctorDashboard() {
  const { user, selectedClinicId } = useAuth();
  const doctorId = user?.userId;
  const { queueData } = useWebSocket(selectedClinicId, doctorId);

  const [diagnosis, setDiagnosis] = useState('');
  const [medicationsList, setMedicationsList] = useState([{ name: '', dose: 'قرص', freq: '', timing: '' }]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [patientDetails, setPatientDetails] = useState(null);
  const [skippedPatients, setSkippedPatients] = useState([]);

  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [selectedPatientFile, setSelectedPatientFile] = useState(null);

  const [fileDiagnosis, setFileDiagnosis] = useState('');
  const [fileMedicationsList, setFileMedicationsList] = useState([{ name: '', dose: 'قرص', freq: '', timing: '' }]);
  const [isSavingFile, setIsSavingFile] = useState(false);

  const [isEmergencyOpen, setIsEmergencyOpen] = useState(false);
  const [emergencyName, setEmergencyName] = useState('');
  const [emergencyPhone, setEmergencyPhone] = useState('');
  const [emergencyVisitType, setEmergencyVisitType] = useState('EXAMINATION');
  const [emergencyPriority, setEmergencyPriority] = useState(true);
  const [isAddingEmergency, setIsAddingEmergency] = useState(false);

  const commonDrugs = [
    'Panadol', 'Augmentin', 'Brufen', 'Amigraine', 'Congestal',
    'Cataflam', 'Zithromax', 'Flagyl', 'Nexium', 'Controloc',
    'Concor', 'Glucophage', 'Insulin', 'Ventolin'
  ];

  const fetchSkipped = useCallback(async () => {
    if (!selectedClinicId || !doctorId) return;
    try {
      const res = await axiosClient.get(`/queues/${selectedClinicId}/skipped?doctorId=${doctorId}`);
      setSkippedPatients(res.data || []);
    } catch {
      setSkippedPatients([]);
    }
  }, [selectedClinicId, doctorId]);

  useEffect(() => {
    fetchSkipped();
  }, [fetchSkipped, queueData?.currentQueueNumber, queueData?.waitingCount]);

  const addMedicationRow = (isFile = false) => {
    const newRow = { name: '', dose: 'قرص', freq: '', timing: '' };
    if (isFile) setFileMedicationsList([...fileMedicationsList, newRow]);
    else setMedicationsList([...medicationsList, newRow]);
  };

  const removeMedicationRow = (index, isFile = false) => {
    const target = isFile ? [...fileMedicationsList] : [...medicationsList];
    target.splice(index, 1);
    if (isFile) setFileMedicationsList(target);
    else setMedicationsList(target);
  };

  const updateMedication = (index, field, value, isFile = false) => {
    const target = isFile ? [...fileMedicationsList] : [...medicationsList];
    target[index][field] = value;
    if (isFile) setFileMedicationsList(target);
    else setMedicationsList(target);
  };

  const generateMedicationsString = (list) =>
    list
      .filter((med) => med.name.trim() !== '')
      .map((med) => [med.name.trim(), med.dose, med.freq?.trim(), med.timing?.trim()].filter(Boolean).join(' - '))
      .join('\n');

  const calculateAge = (dob) => {
    if (!dob) return 'غير محدد';
    const birthDate = new Date(dob);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) age--;
    return `${age} سنة`;
  };

  useEffect(() => {
    const fetchPatientDetails = async () => {
      if (selectedClinicId && queueData?.currentQueueNumber) {
        try {
          const res = await axiosClient.get(`/doctor/clinics/${selectedClinicId}/current-patient-details`);
          setPatientDetails(res.data);
        } catch {
          setPatientDetails(null);
        }
      } else {
        setPatientDetails(null);
      }
    };
    fetchPatientDetails();
  }, [queueData?.currentQueueNumber, selectedClinicId]);

  const handleSearch = async (e) => {
    const query = e.target.value;
    setSearchQuery(query);
    if (query.length < 2) {
      setSearchResults([]);
      return;
    }
    try {
      const res = await axiosClient.get(`/doctor/patients/search?query=${encodeURIComponent(query)}`);
      setSearchResults(res.data);
    } catch (error) {
      console.error('Search failed', error);
    }
  };

  const openPatientFile = async (patientId) => {
    try {
      const res = await axiosClient.get(`/doctor/patients/${patientId}/file`);
      setSelectedPatientFile(res.data);
      setSearchResults([]);
      setSearchQuery('');
      setFileDiagnosis('');
      setFileMedicationsList([{ name: '', dose: 'قرص', freq: '', timing: '' }]);
    } catch {
      alert('فشل في فتح ملف المريض');
    }
  };

  const handleComplete = async () => {
    const finalMedications = generateMedicationsString(medicationsList);
    if (!diagnosis || !finalMedications) return alert('من فضلك اكتب التشخيص والأدوية');

    setIsSubmitting(true);
    try {
      await axiosClient.post(`/doctor/clinics/${selectedClinicId}/complete-current`, {
        diagnosis,
        medications: finalMedications,
      });
      await axiosClient.post(`/queues/${selectedClinicId}/call-next?doctorId=${doctorId}`);
      setDiagnosis('');
      setMedicationsList([{ name: '', dose: 'قرص', freq: '', timing: '' }]);
    } catch {
      alert('حدث خطأ أثناء حفظ الروشتة');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDirectVisit = async () => {
    const finalMedications = generateMedicationsString(fileMedicationsList);
    if (!fileDiagnosis || !finalMedications) return alert('اكتب التشخيص والأدوية');

    setIsSavingFile(true);
    try {
      await axiosClient.post(`/doctor/clinics/${selectedClinicId}/direct-visit`, {
        patientId: String(selectedPatientFile.id),
        diagnosis: fileDiagnosis,
        medications: finalMedications,
      });
      alert('تم حفظ الروشتة بنجاح!');
      setFileDiagnosis('');
      setFileMedicationsList([{ name: '', dose: 'قرص', freq: '', timing: '' }]);

      const res = await axiosClient.get(`/doctor/patients/${selectedPatientFile.id}/file`);
      setSelectedPatientFile(res.data);
    } catch {
      alert('حدث خطأ أثناء الحفظ');
    } finally {
      setIsSavingFile(false);
    }
  };

  const handleRequeueSkipped = async (appointmentId) => {
    try {
      await axiosClient.post(`/queues/${selectedClinicId}/requeue/${appointmentId}?doctorId=${doctorId}`);
      fetchSkipped();
    } catch {
      alert('فشل في إعادة إدراج المريض');
    }
  };

  const handleAddEmergencyWalkIn = async () => {
    if (!emergencyName.trim()) return alert('اكتب اسم المريض');
    setIsAddingEmergency(true);
    try {
      await axiosClient.post(`/queues/${selectedClinicId}/walk-in`, {
        doctorId: String(doctorId),
        fullName: emergencyName.trim(),
        phoneNumber: emergencyPhone.trim(),
        visitType: emergencyVisitType,
        priority: emergencyPriority,
      });
      setIsEmergencyOpen(false);
      setEmergencyName('');
      setEmergencyPhone('');
      setEmergencyVisitType('EXAMINATION');
      setEmergencyPriority(true);
    } catch (err) {
      alert(err.response?.data?.detail || err.response?.data?.error || 'فشل في إضافة الحالة');
    } finally {
      setIsAddingEmergency(false);
    }
  };

  if (!selectedClinicId) {
    return (
      <DashboardLayout title="لوحة الطبيب">
        <div className="card p-10 text-center font-bold text-slate-500">
          لم يتم ربطك بأي فرع بعد.
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout title="لوحة الطبيب">
      <datalist id="drugs-list">
        {commonDrugs.map((drug) => <option key={drug} value={drug} />)}
      </datalist>

      <datalist id="freq-list">
        <option value="مرة يومياً" />
        <option value="مرتين يومياً" />
        <option value="3 مرات يومياً" />
        <option value="كل 4 ساعات" />
        <option value="كل 6 ساعات" />
        <option value="كل 8 ساعات" />
        <option value="كل 12 ساعة" />
        <option value="عند اللزوم" />
      </datalist>

      <div className="max-w-6xl mx-auto mb-8 flex flex-wrap gap-4 items-center relative z-40">
        <div className="relative flex-1 min-w-[260px]">
          <Search className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400" size={20} />
          <input
            type="text"
            value={searchQuery}
            onChange={handleSearch}
            placeholder="ابحث عن مريض بالاسم..."
            className="input-fancy w-full py-4 pr-12 pl-4 shadow-lg"
          />
          {searchResults.length > 0 && (
            <div className="absolute w-full mt-2 bg-white rounded-2xl shadow-2xl border max-h-60 overflow-y-auto">
              {searchResults.map((p) => (
                <div
                  key={p.id}
                  onClick={() => openPatientFile(p.id)}
                  className="p-4 border-b last:border-0 hover:bg-indigo-50 cursor-pointer flex justify-between items-center"
                >
                  <div>
                    <span className="font-bold text-slate-800 block">{p.fullName}</span>
                    <span className="text-slate-400 text-xs">
                      {p.dateOfBirth ? `العمر: ${calculateAge(p.dateOfBirth)}` : 'العمر غير مسجل'}
                    </span>
                  </div>
                  <span className="text-slate-400 text-sm">{p.phoneNumber || ''}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        <button
          onClick={() => setIsEmergencyOpen(true)}
          className="px-5 py-4 rounded-2xl bg-rose-600 text-white font-black flex items-center gap-2 shadow-lg hover:bg-rose-700"
        >
          <Siren size={20} /> إضافة حالة طارئة / مباشر
        </button>
      </div>

      <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          <div className="card p-8 flex items-center gap-6">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 text-white flex items-center justify-center shrink-0">
              <User size={36} />
            </div>
            <div>
              <p className="text-slate-400 font-bold mb-1 flex items-center gap-2">
                المريض الحالي في العيادة
                {queueData?.currentQueueNumber && (
                  <span className="bg-indigo-100 text-indigo-600 px-3 py-0.5 rounded-full text-sm font-black">
                    رقم {queueData.currentQueueNumber}
                  </span>
                )}
              </p>
              <div className="flex items-center gap-3 flex-wrap">
                <p className={`text-3xl font-black ${patientDetails ? 'text-slate-800' : 'text-slate-300'}`}>
                  {patientDetails ? patientDetails.patientName : 'لا يوجد مريض'}
                </p>
                {patientDetails?.dateOfBirth && (
                  <span className="bg-orange-100 text-orange-600 px-3 py-1 rounded-full text-sm font-black">
                    {calculateAge(patientDetails.dateOfBirth)}
                  </span>
                )}
              </div>
            </div>
          </div>

          {queueData?.currentQueueNumber && (
            <div className="card p-8">
              <h2 className="text-2xl font-black text-slate-800 mb-6 flex items-center gap-2">
                <FileText className="text-indigo-500" /> كتابة الروشتة الطبية
              </h2>
              <div className="space-y-6">
                <div>
                  <label className="block text-slate-600 font-bold mb-2">التشخيص</label>
                  <textarea
                    value={diagnosis}
                    onChange={(e) => setDiagnosis(e.target.value)}
                    className="input-fancy w-full p-4 resize-none"
                    rows="3"
                    placeholder="اكتب التشخيص هنا..."
                  />
                </div>
                <div className="bg-indigo-50/80 p-6 rounded-2xl border border-indigo-100">
                  <label className="flex items-center gap-2 text-slate-800 font-bold text-lg mb-4">
                    <Pill className="text-indigo-500" /> الأدوية والجرعات
                  </label>
                  <MedicationBuilder
                    list={medicationsList}
                    isFile={false}
                    updateMedication={updateMedication}
                    removeMedicationRow={removeMedicationRow}
                    addMedicationRow={addMedicationRow}
                  />
                </div>
              </div>
              <button
                onClick={handleComplete}
                disabled={isSubmitting}
                className="btn-primary mt-8 w-full font-bold py-4 flex justify-center items-center gap-3"
              >
                <CheckCircle2 size={24} />
                {isSubmitting ? 'جاري الحفظ...' : 'إنهاء الكشف وحفظ الروشتة'}
              </button>
            </div>
          )}

          {skippedPatients.length > 0 && (
            <div className="card p-6">
              <h3 className="text-xl font-black text-slate-800 mb-4">مرضى متخطون (إعادة إدراج)</h3>
              <div className="space-y-3">
                {skippedPatients.map((p) => (
                  <div key={p.appointmentId} className="flex justify-between items-center bg-orange-50/70 border border-orange-100 p-4 rounded-xl">
                    <div className="flex items-center gap-3">
                      <div className="bg-orange-100 text-orange-700 w-10 h-10 rounded-xl flex items-center justify-center font-black">
                        {p.queueNumber}
                      </div>
                      <span className="font-bold text-slate-700">{p.patientName}</span>
                    </div>
                    <button
                      onClick={() => handleRequeueSkipped(p.appointmentId)}
                      className="bg-indigo-600 text-white px-4 py-2 rounded-xl font-bold text-sm flex items-center gap-1.5"
                    >
                      <RotateCcw size={15} /> إرجاع للطابور
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="lg:col-span-1">
          <div className="card p-6 max-h-[800px] overflow-y-auto">
            <h3 className="text-xl font-black text-slate-800 mb-6 flex items-center gap-2 border-b pb-4">
              <Clock className="text-orange-500" /> التاريخ المرضي
            </h3>
            {!patientDetails?.history?.length ? (
              <div className="text-center mt-10 text-slate-400">
                <UserRound size={48} className="mx-auto mb-3 opacity-40" />
                <p className="font-bold">لا يوجد تاريخ مرضي سابق.</p>
              </div>
            ) : (
              <div className="space-y-4">
                {patientDetails.history.map((p) => (
                  <div key={p.id} className="bg-orange-50/80 border border-orange-100 p-4 rounded-2xl">
                    <p className="text-xs text-slate-400 mb-2 font-bold">
                      {new Date(p.createdAt).toLocaleDateString('ar-EG')}
                    </p>
                    <p className="font-bold text-slate-700 text-sm">التشخيص: {p.diagnosis}</p>
                    <p className="text-indigo-600 font-semibold text-sm mt-1 whitespace-pre-wrap">
                      الأدوية: {p.medications}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {selectedPatientFile && (
        <div className="modal-backdrop flex items-center justify-center z-50 p-4" onClick={() => setSelectedPatientFile(null)}>
          <div
            className="bg-white rounded-3xl w-full max-w-5xl max-h-[90vh] overflow-y-auto p-8 shadow-2xl relative flex flex-col md:flex-row gap-8"
            onClick={(e) => e.stopPropagation()}
          >
            <button onClick={() => setSelectedPatientFile(null)} className="absolute top-6 left-6 text-slate-400 hover:text-red-500">
              <X size={24} />
            </button>

            <div className="flex-1 md:border-l border-slate-100 md:pl-8">
              <h2 className="text-3xl font-black text-slate-800 mb-2">{selectedPatientFile.fullName}</h2>
              <p className="text-slate-500 text-sm">رقم الهاتف: {selectedPatientFile.phoneNumber || 'غير متوفر'}</p>
              <p className="text-slate-500 text-sm mb-6">العمر: {calculateAge(selectedPatientFile.dateOfBirth)}</p>

              <h3 className="text-xl font-black text-slate-800 mb-4">التاريخ المرضي</h3>
              <div className="space-y-4 max-h-[50vh] overflow-y-auto">
                {selectedPatientFile.history.length === 0 ? (
                  <p className="text-slate-400 font-bold">لا يوجد زيارات سابقة.</p>
                ) : (
                  selectedPatientFile.history.map((p, idx) => (
                    <div key={idx} className="bg-slate-50 border p-4 rounded-xl">
                      <p className="text-xs text-slate-400 mb-1 font-bold">{new Date(p.createdAt).toLocaleDateString('ar-EG')}</p>
                      <p className="font-bold text-slate-700 text-sm">التشخيص: {p.diagnosis}</p>
                      <p className="text-indigo-600 text-sm whitespace-pre-wrap">الأدوية: {p.medications}</p>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="flex-1">
              <h3 className="text-xl font-black text-indigo-600 mb-6 flex items-center gap-2">
                <FileText /> زيارة جديدة (بدون طابور)
              </h3>
              <div className="space-y-4">
                <textarea
                  value={fileDiagnosis}
                  onChange={(e) => setFileDiagnosis(e.target.value)}
                  className="input-fancy w-full p-3 resize-none"
                  rows="2"
                  placeholder="التشخيص..."
                />
                <div className="bg-indigo-50/60 p-5 rounded-2xl border border-indigo-100">
                  <MedicationBuilder
                    list={fileMedicationsList}
                    isFile={true}
                    updateMedication={updateMedication}
                    removeMedicationRow={removeMedicationRow}
                    addMedicationRow={addMedicationRow}
                  />
                </div>
                <button onClick={handleDirectVisit} disabled={isSavingFile} className="btn-primary w-full font-bold py-3.5">
                  {isSavingFile ? 'جاري الحفظ...' : 'حفظ الروشتة'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {isEmergencyOpen && (
        <div className="modal-backdrop flex items-center justify-center z-50 p-4" onClick={() => setIsEmergencyOpen(false)}>
          <div className="bg-white p-8 rounded-3xl w-[460px] max-w-full shadow-2xl" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-2xl font-black mb-5 text-slate-800 flex items-center gap-2">
              <UserPlus className="text-rose-600" /> إضافة مريض مباشر / طارئ
            </h3>
            <div className="space-y-3 mb-5">
              <input
                type="text"
                value={emergencyName}
                onChange={(e) => setEmergencyName(e.target.value)}
                placeholder="اسم المريض..."
                className="input-fancy w-full p-3 font-semibold"
              />
              <input
                type="text"
                value={emergencyPhone}
                onChange={(e) => setEmergencyPhone(e.target.value)}
                placeholder="رقم الهاتف (اختياري)"
                className="input-fancy w-full p-3 font-semibold"
                dir="ltr"
              />
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setEmergencyVisitType('EXAMINATION')}
                  className={`p-3 rounded-xl border font-bold text-sm ${
                    emergencyVisitType === 'EXAMINATION'
                      ? 'bg-indigo-50 border-indigo-500 text-indigo-700'
                      : 'border-slate-200 text-slate-500'
                  }`}
                >
                  كشف جديد
                </button>
                <button
                  type="button"
                  onClick={() => setEmergencyVisitType('CONSULTATION')}
                  className={`p-3 rounded-xl border font-bold text-sm ${
                    emergencyVisitType === 'CONSULTATION'
                      ? 'bg-indigo-50 border-indigo-500 text-indigo-700'
                      : 'border-slate-200 text-slate-500'
                  }`}
                >
                  استشارة
                </button>
              </div>
              <label className="flex items-center gap-3 p-3 rounded-2xl border border-rose-200 bg-rose-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={emergencyPriority}
                  onChange={(e) => setEmergencyPriority(e.target.checked)}
                  className="w-5 h-5 accent-rose-600"
                />
                <span className="font-bold text-rose-700 text-sm">حالة طارئة (توضع في مقدمة الطابور)</span>
              </label>
            </div>
            <div className="flex gap-3">
              <button onClick={handleAddEmergencyWalkIn} disabled={isAddingEmergency} className="btn-primary flex-1 py-3 font-bold">
                {isAddingEmergency ? 'جاري الإضافة...' : 'إضافة للطابور'}
              </button>
              <button onClick={() => setIsEmergencyOpen(false)} className="flex-1 bg-slate-100 text-slate-600 font-bold py-3 rounded-xl">
                إلغاء
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}