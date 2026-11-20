import { useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import { MapPin, Clock, Save } from 'lucide-react';

const DAYS = [
  { key: 'SATURDAY', label: 'السبت' },
  { key: 'SUNDAY', label: 'الأحد' },
  { key: 'MONDAY', label: 'الاثنين' },
  { key: 'TUESDAY', label: 'الثلاثاء' },
  { key: 'WEDNESDAY', label: 'الأربعاء' },
  { key: 'THURSDAY', label: 'الخميس' },
  { key: 'FRIDAY', label: 'الجمعة' },
];

export default function ClinicsManagement() {
  const { refreshClinics } = useAuth();
  const [clinics, setClinics] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchClinics = async () => {
    try {
      const res = await axiosClient.get('/clinics/my');
      setClinics(res.data.map((item) => item.clinic));
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClinics();
  }, []);

  const toggleDay = (clinicIndex, dayKey) => {
    const updated = [...clinics];
    const days = (updated[clinicIndex].workingDays || '').split(',').filter(Boolean);
    updated[clinicIndex].workingDays = days.includes(dayKey)
      ? days.filter((d) => d !== dayKey).join(',')
      : [...days, dayKey].join(',');
    setClinics(updated);
  };

  const saveClinic = async (clinic) => {
    try {
      await axiosClient.put(`/clinics/${clinic.id}/schedule`, {
        bookingOpenTime: clinic.bookingOpenTime || '00:00',
        bookingCloseTime: clinic.bookingCloseTime || '23:59',
        doctorArrivalTime: clinic.doctorArrivalTime || '17:00',
      });
      await axiosClient.put(`/clinics/${clinic.id}/info`, {
        workingDays: clinic.workingDays || '',
        address: clinic.address || '',
        city: clinic.city || '',
        name: clinic.name || '',
      });
      alert(`تم حفظ بيانات ${clinic.name}`);
      await fetchClinics();
      await refreshClinics();
    } catch {
      alert('فشل في الحفظ');
    }
  };

  if (loading) {
    return (
      <DashboardLayout title="إدارة الفروع">
        <div className="text-center p-10 font-bold">جاري التحميل...</div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout title="إدارة الفروع">
      <div className="max-w-5xl mx-auto space-y-6">
        {clinics.map((clinic, idx) => (
          <div key={clinic.id} className="card p-8">
            <div className="flex flex-wrap justify-between items-start gap-4 mb-6">
              <div>
                <input
                  type="text"
                  value={clinic.name}
                  onChange={(e) => {
                    const nc = [...clinics];
                    nc[idx].name = e.target.value;
                    setClinics(nc);
                  }}
                  className="text-2xl font-bold bg-transparent border-b-2 border-teal-200 outline-none"
                />
                <div className="flex items-center gap-2 mt-2 text-slate-500">
                  <MapPin size={16} />
                  <input
                    type="text"
                    value={clinic.address || ''}
                    onChange={(e) => {
                      const nc = [...clinics];
                      nc[idx].address = e.target.value;
                      setClinics(nc);
                    }}
                    placeholder="عنوان الفرع..."
                    className="bg-transparent border-b border-slate-200 outline-none text-sm w-64"
                  />
                </div>
              </div>
              <button onClick={() => saveClinic(clinic)} className="btn-primary px-6 py-2.5 font-bold flex items-center gap-2">
                <Save size={18} /> حفظ التعديلات
              </button>
            </div>

            <div className="mb-6">
              <p className="text-sm font-bold text-slate-700 mb-3">أيام العمل:</p>
              <div className="flex flex-wrap gap-2">
                {DAYS.map((day) => {
                  const isActive = (clinic.workingDays || '').split(',').includes(day.key);
                  return (
                    <button
                      key={day.key}
                      onClick={() => toggleDay(idx, day.key)}
                      className={`px-4 py-2 rounded-xl font-bold transition-all ${
                        isActive ? 'bg-teal-700 text-white shadow-md' : 'bg-slate-100 text-slate-500'
                      }`}
                    >
                      {day.label}
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="text-xs font-bold text-slate-500 flex items-center gap-1 mb-1">
                  <Clock size={14} /> بداية الحجز
                </label>
                <input
                  type="time"
                  value={clinic.bookingOpenTime || '00:00'}
                  onChange={(e) => {
                    const nc = [...clinics];
                    nc[idx].bookingOpenTime = e.target.value;
                    setClinics(nc);
                  }}
                  className="input-fancy w-full p-2.5 text-sm"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-500 mb-1 block">آخر ميعاد للحجز</label>
                <input
                  type="time"
                  value={clinic.bookingCloseTime || '23:59'}
                  onChange={(e) => {
                    const nc = [...clinics];
                    nc[idx].bookingCloseTime = e.target.value;
                    setClinics(nc);
                  }}
                  className="input-fancy w-full p-2.5 text-sm"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-emerald-700 mb-1 block">موعد حضور الطبيب (بدء الكشف)</label>
                <input
                  type="time"
                  value={clinic.doctorArrivalTime || '17:00'}
                  onChange={(e) => {
                    const nc = [...clinics];
                    nc[idx].doctorArrivalTime = e.target.value;
                    setClinics(nc);
                  }}
                  className="input-fancy w-full p-2.5 text-sm"
                />
              </div>
            </div>
          </div>
        ))}
      </div>
    </DashboardLayout>
  );
}